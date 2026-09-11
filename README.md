# Still

A responsive website and shared-interface Android/iOS focus application. No account or backend is needed. Sessions, selected apps, intentions, and stopwatch laps stay on the device.

## Run the website

With Node.js installed, run `npm run dev`, then open http://127.0.0.1:4173. Run `npm test` for timer behavior and `npm run check` for JavaScript syntax. The authored website is in `dist`; it needs no build or package installation.

Features: focus timer, short breaks, 1–180 minute custom sessions, pause/resume/reset, stopwatch with laps, completion sound while the page is active, intention, device-local session history and daily totals, app selections, responsive layout and zen view. Background timing is computed from timestamps; browsers can suspend page execution, so completion sound/history update when execution resumes. No guaranteed background alarm is claimed.

## Android

1. Run `npm run sync:native` after changing the shared UI.
2. Open `android` in Android Studio. Install SDK 35, use JDK 17 and Gradle 8.9 (a global Gradle install can generate the wrapper with `gradle wrapper --gradle-version 8.9`).
3. Sync and run on an Android 8+ device. Create a signed release using your own keystore when ready.
4. In App limits, tap Set up device blocking, read the disclosure, enable Still's Accessibility service, return, and select installed apps.

The service observes foreground package names only, never window content. It returns to Home when a selected app opens during a running focus session. Settings, this app, Home and common phone packages are excluded. Pause/reset clears enforcement; the deadline is checked on every event, even if the UI is closed. This is a voluntary focus aid: the user can disable the service, and OEM behavior and notifications may offer ways around restrictions. Real-device testing and store accessibility-policy review are required before release.

## iOS

1. Use a Mac with Xcode and XcodeGen. Run `npm run sync:native`, then `cd ios` and `xcodegen generate`.
2. Open Still.xcodeproj. Choose your signing team and unique bundle IDs for both targets.
3. Enable Family Controls for the app and its Device Activity monitor extension. Obtain Apple's distribution entitlement approval before distributing builds.
4. Run on a real iOS 16+ device, authorize individual Screen Time access and select apps in Apple's picker.

ManagedSettings shields selected apps/categories/domains during focus. DeviceActivityMonitor clears shields at the deadline, including while the app is suspended. iOS monitoring intervals require 15 minutes; shorter sessions and resumes with less than 15 minutes remaining run as timers without blocking and show an error explaining that limitation. OS extension delivery can be delayed; reopening the app also reconciles expired shields. Users can revoke authorization. Test signing, extension execution, pause/resume, midnight crossing, device restart, and expiry on real hardware before release.

## Delivery status and verification

The web app is runnable. Native source projects are included; APK/IPA binaries are not built or signed in this Windows workspace, which lacks Android SDK/Gradle and cannot run Xcode. Do not represent these projects as store-ready or device-tested. Mobile store listings, accounts, certificates, and publication are not included.

The web app cannot enumerate or block installed apps. Its app list is explicitly a planning selection; actual blocking is only enabled in native builds with permission. No extension-based website blocking is included. Data is not synchronized across devices. Clearing site/app data deletes local progress.

References: https://developer.android.com/guide/topics/ui/accessibility/views/service and https://developer.apple.com/documentation/familycontrols/requesting-the-family-controls-entitlement
