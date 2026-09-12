package com.still.focus

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.telecom.TelecomManager

/** Native state survives closing the UI; elapsed time includes device sleep. */
class FocusSession(private val context: Context) {
 val prefs = context.getSharedPreferences("focus", Context.MODE_PRIVATE)
 private fun bootCount() = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)
 fun remaining(): Long {
  if (prefs.getInt("bootCount", -2) != bootCount()) return 0
  return (prefs.getLong("elapsedDeadline", 0) - SystemClock.elapsedRealtime()).coerceAtLeast(0)
 }
 fun start(apps: Set<String>, endAt: Long) {
  val duration = endAt - System.currentTimeMillis()
  require(duration in 1..10801000) { "Invalid session duration." }
  prefs.edit().putStringSet("apps", apps - protectedPackages()).putLong("endAt", endAt)
   .putLong("elapsedDeadline", SystemClock.elapsedRealtime() + duration)
   .putInt("bootCount", bootCount()).apply()
 }
 fun stop() { prefs.edit().remove("endAt").remove("elapsedDeadline").remove("apps").apply() }
 fun blocks(app: String): Boolean = remaining() > 0 && app !in protectedPackages() && prefs.getStringSet("apps", emptySet())?.contains(app) == true
 fun protectedPackages(): Set<String> {
  val home = context.packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)?.activityInfo?.packageName
  val settings = context.packageManager.resolveActivity(Intent(Settings.ACTION_SETTINGS), 0)?.activityInfo?.packageName
  val dialer = (context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager)?.defaultDialerPackage
  return setOfNotNull(context.packageName, home, settings, dialer, "com.android.settings", "com.android.systemui", "com.android.phone", "com.android.server.telecom", "com.google.android.dialer", "com.android.dialer", "com.android.permissioncontroller", "com.google.android.permissioncontroller")
 }
}
