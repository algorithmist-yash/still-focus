# Android focus restrictions

The website and its shared assets are unchanged. Android uses the user-enabled AccessibilityService to cover selected foreground apps with a native blocking screen. It does not change Android Battery Saver settings, suspend packages, stop app processes, disable notifications, or prevent uninstalling or revoking permission.

The blocking screen shows remaining time and provides Home and Return to Still controls. Pause, finish, or reset in Still clears native restrictions. A preference listener removes an existing blocking screen after the session stops. If an overlay cannot be shown, the service attempts to return to Home.

The native deadline uses elapsedRealtime, including deep sleep. Changing the wall clock does not directly extend that native deadline; the existing WebView timer still uses wall time and can end a session independently. Restrictions fail open after a reboot. The system settings, launcher, default dialer, known phone services, permission controllers, and Still are excluded from selection and enforcement. No screen content is requested.

## Required physical-device acceptance tests

- Enable the Accessibility service after reading and accepting the disclosure; confirm denial leaves the timer usable.
- Select a nonessential installed app and start a named focus timer. Open it from the launcher, Recents, and a notification. Confirm the blocking screen appears and prevents interaction while visible.
- Test Home, Back, Return to Still, incoming calls, emergency calling access, notification shade, permission prompts, and Settings. Essential controls must stay accessible.
- Verify an unselected app stays usable and switching between selected apps updates the label.
- With the blocking screen visible, let the timer expire. Confirm it disappears and the app becomes usable.
- Pause, finish, and reset in Still. Confirm restrictions stop immediately, then resume and check the new remaining time.
- Close the Still activity without disabling its Accessibility service. Verify enforcement remains active. Test OEM battery restrictions separately.
- Lock and unlock the phone through expiry. Test reboot: native restrictions should be off until a new/resumed session is explicitly started.
- Revoke the service permission while the screen is visible; confirm cleanup and ability to use the device.
- Test landscape, large font, split screen, keyboards, and third-party launchers/dialers. Accessibility window-event behavior can vary; any gaps must be resolved before claiming broad device support.

## Release status

The debug APK now builds successfully with the installed Android toolchain and has been installed on emulator-5554 with app data preserved. Android reports Still's Accessibility service as enabled and bound. A permission-detection bug was fixed: Android stores the component as `com.still.focus/.FocusService`, so the app must compare parsed ComponentName objects rather than raw flattened strings. The previous string comparison incorrectly treated the enabled service as disabled and prevented starting restrictions.

End-to-end blocking after this fix still needs a fresh focus session in the emulator and the acceptance tests above. Physical Android hardware has not been tested. Play release still requires checking the target SDK/build tools against current requirements, a signed release bundle, and Google's Accessibility API declaration review. The development APK is at `app/build/outputs/apk/debug/app-debug.apk`.
