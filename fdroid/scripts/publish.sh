#!/usr/bin/env bash
# Build + publish a signed APK to the F-Droid repo on S3.
# Normally invoked by the pre-push git hook (scripts/git-hooks/pre-push) when an
# "fdroid publish" commit is being pushed to main. Bump and commit happen
# separately (in the dev's commit before push), so SKIP_BUMP and SKIP_COMMIT
# both default to 1. Pass SKIP_BUMP=0 / SKIP_COMMIT=0 to opt back in to the
# legacy "do everything" flow.
#
# Flow:
#  1) source env, guard required vars
#  2) sync from S3 -> staging (repo/archive)
#  3) bump version (only if SKIP_BUMP=0)
#  4) build signed release APK
#  5) stage APK into repo/, regenerate metadata from inventory
#  6) fdroid update --create-metadata
#  7) validate counts/current version
#  8) backup, sync to S3, invalidate CDN
#  9) git commit (only if SKIP_COMMIT=0)

set -euo pipefail

# fdroid is installed via pipx at /mnt/extra/pipx/bin, which isn't on the
# default interactive PATH on this machine. Inject it so the require_cmd check
# passes when the script runs from a git hook or a stripped-down shell.
if ! command -v fdroid >/dev/null && [[ -x /mnt/extra/pipx/bin/fdroid ]]; then
  PATH="/mnt/extra/pipx/bin:$PATH"
fi

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
FDROID_DATA="${FDROID_DATA:-/home/arch/fdroid}"
cd "$ROOT"

ENV_FILE="${ENV_FILE:-${FDROID_ENV_FILE:-}}"
if [[ -z "${ENV_FILE}" ]]; then
  if [[ -f "${ROOT}/fdroid/.env" ]]; then
    ENV_FILE="${ROOT}/fdroid/.env"
  elif [[ -f "${FDROID_DATA}/.env" ]]; then
    ENV_FILE="${FDROID_DATA}/.env"
  elif [[ -f "${HOME}/fdroid/.env" ]]; then
    ENV_FILE="${HOME}/fdroid/.env"
  fi
fi
KEEPVERSIONS="${KEEPVERSIONS:-7}"
EXPECTED_MIN_APKS="${EXPECTED_MIN_APKS:-10}"
AWS_CF_ID="${FDROID_AWS_CF_DISTRIBUTION_ID:-E2RWHYJEODFGYE}"
BACKUP_DIR="${ROOT}/backups"
TIMESTAMP="$(date -u +%Y%m%d-%H%M%S)"
DRY_RUN="${DRY_RUN:-0}"
KEEP_BACKUPS="${KEEP_BACKUPS:-3}"

fail() { echo "ERROR: $*" >&2; exit 1; }

log() { echo "== $*"; }

require_cmd() { command -v "$1" >/dev/null || fail "Missing command: $1"; }

require_cmd aws
require_cmd aapt
require_cmd python
require_cmd fdroid

[[ -n "${ENV_FILE}" && -f "$ENV_FILE" ]] || fail "Env file not found. Set ENV_FILE/FDROID_ENV_FILE or create fdroid/.env (git-ignored)."
log "Using env file: $ENV_FILE"
set -a && source "$ENV_FILE" && set +a

scripts/fdroid/check_keystore_env.sh

if [[ "${SKIP_VERSIONING_CHECK:-0}" != "1" ]]; then
  log "Checking S3 bucket versioning is enabled (set SKIP_VERSIONING_CHECK=1 to bypass)"
  versioning_status="$(aws s3api get-bucket-versioning --bucket "${FDROID_AWS_BUCKET}" --query Status --output text 2>/dev/null || true)"
  [[ "$versioning_status" == "Enabled" ]] || fail "S3 bucket versioning is not Enabled for ${FDROID_AWS_BUCKET} (Status=${versioning_status:-unknown})."
fi

log "Syncing from S3 -> local staging"
aws s3 sync "s3://${FDROID_AWS_BUCKET}/repo" "${FDROID_DATA}/repo"
aws s3 sync "s3://${FDROID_AWS_BUCKET}/archive" "${FDROID_DATA}/archive"

log "Quarantining known-bad repo artifacts (*.apk.prev, *.apk.<digits>)"
quarantine_dir="${FDROID_DATA}/quarantine"
mkdir -p "$quarantine_dir"
while IFS= read -r -d '' path; do
  log "Quarantining $(basename "$path")"
  mv "$path" "$quarantine_dir/$(basename "$path")"
done < <(
  find "${FDROID_DATA}/repo" "${FDROID_DATA}/archive" -maxdepth 1 -type f \
    \( -name '*.apk.prev' -o -regextype posix-extended -regex '.*\.apk\.[0-9]+$' \) -print0
)

log "Enforcing keepversions=${KEEPVERSIONS} in config.yml"
python - "$KEEPVERSIONS" "$FDROID_DATA/config.yml" <<'PY'
import sys, re
keep=sys.argv[1]
path=sys.argv[2]
text=open(path).read()
if re.search(r'archive_older:\s*\d+', text):
    text=re.sub(r'archive_older:\s*\d+', f'archive_older: {keep}', text)
else:
    text = text.rstrip()+"\narchive_older: "+keep+"\n"
open(path,"w").write(text)
PY

if [[ "${SKIP_BUMP:-1}" != "1" ]]; then
  log "Bumping versionCode/versionName in ime/app/build.gradle"
  python - <<'PY'
from pathlib import Path
import re
gradle = Path("ime/app/build.gradle")
text = gradle.read_text()
vc_match = re.search(r"ext\.override_version_code\s*=\s*(\d+)", text)
vn_match = re.search(r"ext\.override_version_name\s*=\s*['\"]([^'\"]+)['\"]", text)
if not vc_match or not vn_match:
    raise SystemExit("version fields not found")
vc = int(vc_match.group(1)) + 1
vn_parts = vn_match.group(1).split(".")
vn_parts[-1] = str(int(vn_parts[-1]) + 1)
vn = ".".join(vn_parts)
text = re.sub(r"ext\.override_version_code\s*=\s*\d+", f"ext.override_version_code = {vc}", text, count=1)
text = re.sub(
    r"ext\.override_version_name\s*=\s*['\"][^'\"]+['\"]",
    f"ext.override_version_name = '{vn}'",
    text,
    count=1,
)
gradle.write_text(text)
print(f"Bumped to versionCode={vc}, versionName={vn}")
PY
fi

log "Building signed release APK"
GRADLE_USER_HOME=${GRADLE_USER_HOME:-/mnt/finished/.gradle} \
KEY_STORE_FILE_PASSWORD=${KEY_STORE_FILE_PASSWORD:-$FDROID_KEYSTORE_PASS} \
KEY_STORE_FILE_DEFAULT_ALIAS_PASSWORD=${KEY_STORE_FILE_DEFAULT_ALIAS_PASSWORD:-${FDROID_KEY_ALIAS_PASS:-$FDROID_KEY_PASS}} \
./gradlew :ime:app:assembleNskRelease -x lint

apk_dir="ime/app/build/outputs/apk/nsk/release"
apk_path="$(ls -1t "${apk_dir}"/*.apk 2>/dev/null | head -n 1 || true)"
[ -f "$apk_path" ] || fail "APK not found under $apk_dir"

pkg_line="$(aapt dump badging "$apk_path" | grep '^package: ' | head -n 1 || true)"
[[ -n "$pkg_line" ]] || fail "Unable to read package info from $apk_path"
version_code="$(echo "$pkg_line" | sed -n "s/.*versionCode='\\([^']*\\)'.*/\\1/p" | head -n 1)"
version_name="$(echo "$pkg_line" | sed -n "s/.*versionName='\\([^']*\\)'.*/\\1/p" | head -n 1)"
[[ -n "$version_code" ]] || fail "Unable to parse versionCode from aapt output"
[[ -n "$version_name" ]] || fail "Unable to parse versionName from aapt output"

log "Copying APK into repo/"
dest_apk="${FDROID_DATA}/repo/wtf.uhoh.newsoftkeyboard_${version_code}.apk"
if [[ -f "$dest_apk" && "${ALLOW_OVERWRITE:-0}" != "1" ]]; then
  fail "Refusing to overwrite existing $dest_apk. Bump version or set ALLOW_OVERWRITE=1."
fi
cp "$apk_path" "$dest_apk"
ln -sf "repo/$(basename "$dest_apk")" "${FDROID_DATA}/wtf.uhoh.newsoftkeyboard.apk"
log "Staged ${dest_apk} (versionName=${version_name})"

log "Regenerating metadata from inventory"
CURRENT_VERSION_CODE=
(cd "$FDROID_DATA" && "$ROOT/scripts/fdroid/generate_metadata.py")

log "Running fdroid update"
(cd "$FDROID_DATA" && fdroid update --create-metadata --verbose)

log "Validating APK count >= $EXPECTED_MIN_APKS"
count=$(find -L "${FDROID_DATA}/repo" "${FDROID_DATA}/archive" -name "wtf.uhoh.newsoftkeyboard_*.apk" | wc -l)
[[ $count -ge $EXPECTED_MIN_APKS ]] || fail "Only $count APKs found"

log "Backup repo+archive+metadata to $BACKUP_DIR"
mkdir -p "$BACKUP_DIR"
log "Pruning old backups (keep last ${KEEP_BACKUPS})"
python - "$BACKUP_DIR" "$KEEP_BACKUPS" <<'PY'
from pathlib import Path
import sys

backup_dir = Path(sys.argv[1])
keep = int(sys.argv[2])
if keep <= 0:
    raise SystemExit(0)

files = sorted(
    backup_dir.glob("fdroid-*.tar.gz"),
    key=lambda p: p.stat().st_mtime,
    reverse=True,
)

for path in files[keep:]:
    try:
        path.unlink()
    except FileNotFoundError:
        pass
PY
tar czf "$BACKUP_DIR/fdroid-${TIMESTAMP}.tar.gz" -C "$FDROID_DATA" repo archive metadata config.yml

if [[ "$DRY_RUN" == "1" ]]; then
  log "DRY_RUN=1 set; skipping S3 sync + CloudFront invalidation + git commit"
  exit 0
fi

log "Syncing back to S3"
aws s3 sync "${FDROID_DATA}/repo" "s3://${FDROID_AWS_BUCKET}/repo"
aws s3 sync "${FDROID_DATA}/archive" "s3://${FDROID_AWS_BUCKET}/archive"
aws s3 sync "${FDROID_DATA}/metadata" "s3://${FDROID_AWS_BUCKET}/metadata"
aws s3 cp "${FDROID_DATA}/config.yml" "s3://${FDROID_AWS_BUCKET}/config.yml"

log "Invalidating CloudFront ($AWS_CF_ID)"
aws cloudfront create-invalidation --distribution-id "$AWS_CF_ID" --paths "/repo/*" "/archive/*" "/metadata/*" "/index.*" "/status/*" "/diff/*" || true

if [[ "${SKIP_COMMIT:-1}" != "1" ]]; then
  log "Creating git commit"
  git add fdroid/scripts fdroid/.env.example || true
  git add ime/app/build.gradle
  git commit -m "fdroid publish"
fi

log "Publish complete."
