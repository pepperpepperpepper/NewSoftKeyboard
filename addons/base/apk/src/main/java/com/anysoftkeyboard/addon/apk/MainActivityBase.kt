package com.anysoftkeyboard.addon.apk

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.anysoftkeyboard.addon.base.apk.R
import com.anysoftkeyboard.addon.base.apk.databinding.ActivityMainBinding

private data class HostKeyboardApp(
    val packageName: String,
    val imeServiceClassName: String,
)

private val HOST_KEYBOARD_APPS =
    listOf(
        HostKeyboardApp(
            "wtf.uhoh.newsoftkeyboard",
            "wtf.uhoh.newsoftkeyboard.NewSoftKeyboardService",
        ),
        // Side-by-side compatibility build: keeps the legacy SoftKeyboard service class.
        HostKeyboardApp(
            "wtf.uhoh.newsoftkeyboard.askcompat",
            "com.menny.android.anysoftkeyboard.SoftKeyboard",
        ),
        // Upstream AnySoftKeyboard (legacy).
        HostKeyboardApp(
            "com.menny.android.anysoftkeyboard",
            "com.menny.android.anysoftkeyboard.SoftKeyboard",
        ),
    )

abstract class MainActivityBase(
    @StringRes private val addOnName: Int,
    @StringRes private val addOnDescription: Int,
    @StringRes private val addOnWebsite: Int,
    @StringRes private val addOnReleaseNotes: Int,
    @DrawableRes private val screenshot: Int,
) : AppCompatActivity() {
  private lateinit var binding: ActivityMainBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
    binding.appScreenshot.setImageResource(screenshot)
    binding.welcomeDescription.text =
        getString(R.string.welcome_subtitle_template, getText(addOnName))
    binding.packDescription.setText(addOnDescription)
    binding.addOnWebSite.text = getString(R.string.add_on_website_template, getText(addOnWebsite))
    val version =
        packageManager.getPackageInfo(packageName, 0).run { "$versionName ($versionCode)" }
    binding.releaseNotes.text =
        getString(R.string.release_notes_template, version, getText(addOnReleaseNotes))

    val installedHost = findInstalledHostKeyboardApp()
    if (installedHost != null) {
      binding.actionDescription.setText(R.string.ask_installed)
      binding.actionButton.setText(R.string.open_ask_main_settings)
      binding.actionButton.setOnClickListener {
        try {
          packageManager.getLaunchIntentForPackage(installedHost.packageName)?.let { intent ->
            it.context.startActivity(intent)
          }
        } catch (ex: Exception) {
          Log.e("NSK_ADD_ON", "Could not launch host settings!", ex)
        }
      }
    } else {
      binding.actionDescription.setText(R.string.ask_is_missing_need_install)
      binding.actionButton.setText(R.string.open_ask_in_vending)
      binding.actionButton.setOnClickListener {
        try {
          val search = Intent(Intent.ACTION_VIEW)
          val uri =
              Uri.Builder()
                  .scheme("market")
                  .authority("search")
                  .appendQueryParameter("q", HOST_KEYBOARD_APPS.first().packageName)
                  .build()
          search.setData(uri)
          it.context.startActivity(search)
        } catch (ex: Exception) {
          Log.e("NSK_ADD_ON", "Could not launch app store search!", ex)
        }
      }
    }
  }

  private fun findInstalledHostKeyboardApp(): HostKeyboardApp? {
    // TODO: ideally query for a broadcast receiver contract; service enumeration is a best-effort.
    return HOST_KEYBOARD_APPS.firstOrNull { candidate ->
      try {
        val services =
            packageManager.getPackageInfo(candidate.packageName, PackageManager.GET_SERVICES)
        services.services?.any { it.name == candidate.imeServiceClassName } ?: false
      } catch (e: Exception) {
        false
      }
    }
  }
}
