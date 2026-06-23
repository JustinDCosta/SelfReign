# SelfReign

**Reclaim life & habits.** SelfReign is a calm, fully-offline Android app that helps
you quit a bad habit by tracking your clean time, turning progress into levels and
rewards, and supporting you through cravings and slips — all on-device and private.

---

## Highlights

- **Live clean-time timer** — a hero progress ring counts days/hours/minutes/seconds
  since your last reset and fills toward the next milestone.
- **Levels & milestones** — six poetic levels (Beginning → Transformed) with
  **customizable day thresholds**.
- **Compassionate relapse model**
  - Your **first** relapse is always forgiven — nothing is lost.
  - After that, a **grace period** keeps your earned rewards temporarily unlocked
    while you climb back; reach the level again before the timer ends to keep them.
  - Relapsing again during grace (or letting it expire) hard-locks those rewards.
- **Relapse log** with an optional note and a trigger tag (Stress, Boredom, Social…),
  shown on a dedicated **History** page.
- **Insights** — total relapses, best streak, money reclaimed, and a trigger breakdown.
- **Money reclaimed** — optionally estimate your daily spend and watch the savings add up.
- **Wallpaper store** — unlock calm, code-drawn gradient backgrounds as you level up.
- **Urge surfing** — a guided box-breathing exercise for "I'm craving" moments.
- **Daily encouragement** — an opt-in daily reminder notification.
- **Home-screen widget** — current streak + level (Glance).
- **App lock** — optional PIN with optional biometric unlock.
- **Encrypted backup** — export/import your data as a JSON file you control.
- **Level-up celebrations** and gentle, opt-in haptics.

## Privacy first

- **100% offline** — there is no `INTERNET` permission; nothing ever leaves the device.
- Recovery data is stored **encrypted at rest** (AES-256 via Android Keystore).
- The app lock PIN is **never** written to backups.
- Backups are disabled at the OS level (`allowBackup="false"`).

---

## Tech stack

| Area | Choice |
|------|--------|
| Language | Kotlin (JVM target 17) |
| UI | Jetpack Compose + Material 3 (AMOLED dark, portrait) |
| Navigation | Navigation-Compose |
| Light prefs | DataStore (Preferences) |
| Recovery state | `EncryptedSharedPreferences` (Jetpack Security / Tink) |
| Background work | WorkManager (daily reminder) |
| Widget | Glance |
| Security | AndroidX Biometric |
| Min / Target SDK | 24 / 34 |
| Build | Gradle 8.7 · AGP 8.5.2 · JDK 17 |

There are **no bundled audio or image assets** — wallpapers and the app icon are
code-defined vectors/gradients, keeping the APK tiny.

---

## Project layout

```
SelfReign/
├── app/                      # The Android application module
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/aldrenstudios/selfreign/
│       │   ├── HabitApp.kt            # Application + tiny manual DI container
│       │   ├── MainActivity.kt        # Single activity; lock / onboarding / app
│       │   ├── audio/                 # FeedbackManager (haptics only)
│       │   ├── data/                  # State machine, persistence, backup
│       │   ├── ui/                    # Compose screens + the shared ViewModel
│       │   ├── util/                  # Quotes, time formatting, reminders, PIN, backup IO
│       │   └── widget/                # Glance home-screen widget
│       └── res/                       # Vector icon, themes, strings
├── logo/                     # Logo design options (SVG)
├── PROJECT_CONTEXT.md        # Detailed technical context
└── README.md                 # This file
```

(See `PROJECT_CONTEXT.md` for the full module map and data-flow details.)

---

## Building

**Requirements:** Android Studio (JDK 17), Android SDK 34, Gradle 8.7 (via wrapper).

```bash
# Debug build
./gradlew :app:assembleDebug

# Release build (configure a signing key first)
./gradlew :app:assembleRelease
```

### Releasing to Google Play
1. Create/keep an upload keystore and add a `signingConfig` (or use Android Studio →
   **Build → Generate Signed Bundle / APK** and choose **Android App Bundle**).
2. Run a Lint pass (`./gradlew :app:lintRelease`).
3. Upload the signed `.aab`.

The release build is minified with R8; ProGuard rules live in
`app/proguard-rules.pro`.

---

## License

See [`LICENSE`](LICENSE).
