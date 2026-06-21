# SelfReign — Reclaim Life & Habits

A minimalist, AMOLED-first Android app that helps people **quit or reduce a bad habit**
by tracking clean time, turning it into **levels and unlockable rewards**, and softening
slips with a fair **forgiveness + grace-period** system.

Built with **Kotlin + Jetpack Compose + Material 3**, fully **offline**, with
**encrypted** on-device storage.

- **App ID:** `com.aldrenstudios.selfreign`
- **Publisher:** Aldren Studios

---

## Core Concept

Time clean becomes **progress**. The longer you stay on track, the higher your **Level**,
and each level unlocks calming **wallpapers** and **ambient music** in the **Store**.
Relapses are handled with compassion but clear rules (see the Rulebook screen):

1. **First slip is always forgiven** — nothing is lost, one time only.
2. **Grace period** — after that, a relapse opens a timer equal to the time it took to
   reach your current level. Your rewards stay unlocked while you climb back.
3. **Hard lock** — relapse again during grace, or let the timer expire before regaining
   your level, and those rewards lock until you earn the days back.

---

## Features

| Area | What it does |
|---|---|
| Dashboard | Live clean-time clock, current level, progress bar to next level, grace banner, guarded relapse button |
| Levels | Milestones at Day 1/3/7/14/30 drive level-ups |
| Store | Unlockable gradient wallpapers + ambient music, with apply toggles and unlock requirements |
| Rulebook | Clear, scrollable explanation of all mechanics |
| Onboarding | Fade-in "Welcome" + CTA, then a 3-step animated walkthrough |
| Relapse engine | Deterministic forgiveness / grace / hard-lock state machine |
| Feedback | Subtle haptics + optional UI sounds, toggleable |
| Background audio | Ambient music via a media foreground service (plays while minimised) |
| Backup & restore | Export/import an encrypted-at-rest JSON state file via the system file picker, with defensive validation |
| Security | Recovery state stored in `EncryptedSharedPreferences` (AES-256); no network, no backup off-device |

---

## Tech Stack

- **Language:** Kotlin (JVM 17)
- **UI:** Jetpack Compose, Material 3, Navigation-Compose, Compose Animation
- **State:** ViewModel + StateFlow (a pure, testable relapse state machine)
- **Secure storage:** Jetpack Security `EncryptedSharedPreferences`
- **Prefs:** DataStore (font size, reminder toggle)
- **Audio:** `MediaPlayer` + `MediaSessionCompat` in a `mediaPlayback` foreground service
- **Background work:** WorkManager (daily reminder)
- **Min / Target SDK:** 24 / 34

---

## Project Structure

```
app/src/main/java/com/aldrenstudios/selfreign/
├── HabitApp.kt                 # Application + manual DI container
├── MainActivity.kt             # Single activity; onboarding gate + wallpaper background
├── data/
│   ├── Levels.kt               # Milestone definitions
│   ├── StoreCatalog.kt         # Unlockable wallpapers + music
│   ├── RecoveryState.kt        # Persisted state model + derived level logic
│   ├── RelapseEngine.kt        # PURE deterministic forgiveness/grace/hard-lock rules
│   ├── RecoveryStateStore.kt   # Encrypted persistence (AES-256)
│   ├── RecoveryRepository.kt   # StateFlow source of truth
│   ├── BackupManager.kt        # JSON export + validated import
│   └── SettingsRepository.kt   # DataStore prefs (font, reminders)
├── audio/
│   ├── FeedbackManager.kt      # Haptics + UI sound effects
│   └── AmbientAudioService.kt  # Background ambient music foreground service
├── ui/
│   ├── MainViewModel.kt        # Shared VM: clock, relapse, store, audio, backup
│   ├── AppNavigation.kt        # Bottom nav (Dashboard/Store/Rules/Settings)
│   ├── onboarding/             # Welcome + animated walkthrough
│   ├── dashboard/              # Clock, level, progress, relapse + outcome dialogs
│   ├── store/                  # Wallpaper + music store
│   ├── rules/                  # The Rulebook
│   ├── settings/               # Font, feedback, music, reminders, backup, privacy
│   └── theme/                  # Color, Type, Theme, Wallpapers (gradient brushes)
└── util/
    ├── BackupIo.kt             # Robust SAF read/write
    ├── Notifications.kt        # Notification channels
    ├── ReminderWorker.kt       # Daily encouragement reminder
    ├── Quotes.kt               # Daily motivational quote (used by reminder)
    └── TimeFormat.kt           # Duration formatting
```

---

## Optional Assets (graceful by default)

The app ships with **no binary media** and runs fine without it:

- **Wallpapers** are code-defined gradient brushes (`ui/theme/Wallpapers.kt`).
- **UI sounds** are looked up from `res/raw` by name at runtime. Drop in
  `sfx_click`, `sfx_levelup`, `sfx_relapse` (e.g. `.ogg`) to enable them.
- **Ambient tracks** map to `res/raw` names declared in `StoreCatalog.kt`
  (`ambient_rain`, `ambient_forest`, `ambient_waves`, `ambient_drone`). Missing
  files simply produce no audio — never a crash.

Use only audio you have the rights to ship.

---

## How to Build & Run

1. Open the `SelfReign` folder in **Android Studio** (Hedgehog or newer).
2. Let Gradle sync (it downloads the wrapper JAR + dependencies automatically).
3. Requires **JDK 17** and the **Android SDK (API 34)**.
4. Run on a device/emulator (Android 7.0 / API 24+).

Unit tests (relapse engine + time formatting): `gradlew.bat test`.

---

## Privacy & Data Protection

- **Encrypted at rest:** recovery state uses AES-256 `EncryptedSharedPreferences`.
- **On-device only:** no internet permission, no analytics, no third parties.
- **Backup disabled** off-device (`allowBackup=false` + extraction rules); the only
  way data leaves is a user-initiated export file they control.
- **User-controlled portability:** export/import a JSON backup via the system picker.
