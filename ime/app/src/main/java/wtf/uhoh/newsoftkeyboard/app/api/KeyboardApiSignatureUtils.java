package wtf.uhoh.newsoftkeyboard.app.api;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import androidx.annotation.NonNull;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class KeyboardApiSignatureUtils {

  private KeyboardApiSignatureUtils() {}

  @NonNull
  public static Set<String> getSigningCertDigestsSha256(
      @NonNull Context context, @NonNull String packageName)
      throws PackageManager.NameNotFoundException {
    final PackageManager pm = context.getPackageManager();
    final PackageInfo packageInfo;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
    } else {
      // noinspection deprecation
      packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
    }

    final Signature[] signatures;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      if (packageInfo.signingInfo == null) return Collections.emptySet();
      if (packageInfo.signingInfo.hasMultipleSigners()) {
        signatures = packageInfo.signingInfo.getApkContentsSigners();
      } else {
        signatures = packageInfo.signingInfo.getSigningCertificateHistory();
      }
    } else {
      // noinspection deprecation
      signatures = packageInfo.signatures;
    }

    if (signatures == null || signatures.length == 0) return Collections.emptySet();

    final HashSet<String> digests = new HashSet<>(signatures.length);
    for (Signature signature : signatures) {
      if (signature == null) continue;
      digests.add(sha256Hex(signature.toByteArray()));
    }
    return digests;
  }

  @NonNull
  private static String sha256Hex(@NonNull byte[] bytes) {
    final MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
    final byte[] out = digest.digest(bytes);
    final StringBuilder sb = new StringBuilder(out.length * 2);
    for (byte b : out) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
