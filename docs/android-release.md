# Still Android release preparation

Developer: Yash Raj

Developer contact: yash.algorithmist@gmail.com

Privacy policy: https://github.com/algorithmist-yash/still-focus/blob/master/docs/android-privacy-policy.md

## Included in this revision

- Adaptive launcher icon, round-icon mapping, and Android 13+ monochrome themed icon.
- 512 × 512 opaque Play Store icon: `android/branding/play-store-icon.png`.
- 1024 × 500 Play Store feature graphic: `android/branding/feature-graphic.png`.
- Native Privacy & contact entry; website files remain unchanged.
- API 36 compile/target configuration; Android Gradle plugin 8.10.1 and Gradle 8.11.1 wrapper.
- Version code 2, version name 1.1.0.
- Permission-name normalization fix and native countdown blocking overlay.
- Local IDE settings and signing secrets excluded from Git.

## Suggested listing copy

App name: Still — Focus Timer

Short description: Named focus sessions, distraction limits, and clear activity insights.

Full description:

Make space for focused work with Still. Name your activity, start a focus timer, take a timed break, or use a stopwatch with laps. Review your time by session, clock type, and custom date range.

With your permission, Still uses Android's Accessibility service to detect when a selected distracting app opens and display a focus blocking screen. Restrictions end when your focus session ends or you pause or finish it. Essential device controls remain available. Accessibility access is optional and can be disabled in Android Settings. Still does not read screen content, messages, passwords, or typed text.

Session history and app selections stay on your device. No Still account is required. Restrictions are a voluntary focus aid and do not suspend other applications or prevent changing permissions.

## Remaining before Play production

1. Verify this API-36 build on the emulator again; target-SDK changes can affect layout and permissions. Test physical phones and OEM background behavior.
2. Add actual release screenshots. The feature graphic and store icon are included; do not use fabricated UI screenshots.
3. Create the Play Console developer account, complete identity verification and the Accessibility permission declaration, and record the required demonstration video.
4. Review the actual final build's network/data behavior before completing Data Safety. Policy text is not a substitute for this audit.
5. Create and back up your upload signing key outside the repository. Build a signed AAB in Android Studio; no release key is generated or committed here.
6. Run Play internal/closed testing and request production access as required by your account.

Build with JDK 17 or a compatible configured JDK. From `android`, use `gradlew.bat :app:assembleDebug :app:bundleRelease :app:lintDebug`. A release bundle generated without release signing configuration is unsigned and is not ready for Play upload.

The Build Android workflow runs these checks for Android changes pushed to master and stores the debug APK, unsigned release AAB, and lint report as a 14-day workflow artifact. The unsigned AAB must be signed using your own upload key before Play submission. Never use the debug signing key for production.
