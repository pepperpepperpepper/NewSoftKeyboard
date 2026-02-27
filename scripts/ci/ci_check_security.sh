#!/usr/bin/env bash
set -euo pipefail

python3 scripts/security/check_no_sensitive_logging.py
python3 scripts/security/osv_scan_gradle_maven.py

