# SelfReign — Technical Context

> Complete technical context for working on this app: architecture, data flow,
> file structure, key patterns, and conventions. Kept in sync with the code.

---

## 1. Overview

| | |
|---|---|
| **App name** | SelfReign |
| **Package** | `com.aldrenstudios.selfreign` |
| **Purpose** | Help users quit a bad habit: track clean time, gamify progress into levels/rewards, and support relapse recovery. |
| **Platform** | Android, Jetpack Compose, Material 3 |
| **Min / Target / Compile SDK** | 24 / 34 / 34 |
| **Orientation / Theme** | Portrait only, AMOLED-first dark theme |
| **Connectivity** | 100% offline — no `INTERNET` permission |

**Permissions used:** `POST_NOTIFICATIONS` (daily reminder, Android 13+) and
`VIBRATE` (opt-in haptics). Nothing else.

---

## 2. Architecture

A lightweight, layered, single-activity Compose app. No DI framework — `HabitApp`
acts as a tiny manual container.

```
┌──────────────────────────── UI (Compose) ────────────────────────────┐
│ MainActivity → AppNavigation (bottom nav + NavHost)                   │
│   Dashboard · Insights · Store · Rules · Settings  (+ History page)   │
│   Onboarding · LockScreen · PanicSheet · CelebrationOverlay           │
│                         │ observes StateFlows                         │
│                MainViewModel (activity-scoped)                        │
│                SettingsViewModel (lightweight prefs)                  │
└─────────────────────────────┼─────────────────────────────────────────┘
                              │
┌──────────────────────────── Domain ──────────────────────────────────┐
│ RecoveryRepository  ── single source of truth (StateFlow<RecoveryState>)│
│ RelapseEngine       ── pure relapse/grace state machine (no Android)   │
│ Levels              ── milestone ladder (customizable thresholds)      │
│ BackupManager       ── versioned JSON export/import + validation       │
└─────────────────────────────┼─────────────────────────────────────────┘
                              │
┌──────────────────────────── Data ────────────────────────────────────┐
│ RecoveryStateStore  ── EncryptedSharedPreferences (AES-256 / Keystore) │
│ SettingsRepository  ── DataStore (Preferences): reminders flag only    │
└───────────────────────────────────────────────────────────────────────┘
```

### Layers

| Layer | Components | Responsibility |
|-------|------------|----------------|
| UI | `ui/**` composables | Presentation, interaction |
| ViewModel | `MainViewModel`, `SettingsViewModel` | State exposure, reactive streams |
| Domain | `RecoveryRepository`, `RelapseEngine`, `Levels`, `BackupManager` | Business logic |
| Data | `RecoveryStateStore`, `SettingsRepository` | Persistence |

---

## 3. State & data flow

### Single source of truth
`RecoveryRepository` loads the persisted `RecoveryState` once and exposes it as a
`StateFlow`. Every mutation goes through `update()` which **saves to the encrypted
store and emits** the new state, so the whole UI reacts automatically.

### The clock & derived state (performance-critical)
`MainViewModel` runs a 1 Hz loop that updates a `now: StateFlow<Long>` and calls
`recovery.refresh(now)` to resolve any elapsed grace transitions.

To avoid recomposing whole screens every second, slowly-changing values are exposed
as **derived** flows that only emit when their value actually changes:

```kotlin
val effectiveLevel: StateFlow<Int> =
    combine(state, now) { s, t -> s.effectiveLevel(t) }
        .distinctUntilChanged()
        .stateIn(...)
// likewise: organicLevel, graceShielding
```

Only the live clock subtree (the ring + countdown, the money chip, the grace badge)
reads `now`. The Dashboard, Store, etc. read the derived flows and therefore
recompose only on meaningful change.

### Relapse flow

```
User taps "I Relapsed" → RelapseLogDialog (optional note + trigger)
        │
        ▼  MainViewModel.confirmRelapse(note, trigger)
   RecoveryRepository.relapse() → RelapseEngine.processRelapse(state, now, …)
        │ persists new state, emits StateFlow, refreshes widget
        ▼
   OutcomeDialog shows FIRST_FORGIVENESS / GRACE_STARTED / HARD_LOCK
```

---

## 4. The recovery state machine (`RelapseEngine`)

Pure and deterministic (no Android deps → unit-testable). Rules in priority order:

1. **First-time forgiveness** — if `hasUsedFirstForgiveness` is false, the first
   relapse is forgiven: the streak is **not** reset, the flag is set.
2. **Grace period** — a subsequent relapse (not already in grace) opens a grace
   timer whose duration equals the time it took to reach the user's current level
   (`Levels.graceMillisForLevel`). The streak resets to `now`; the reached level's
   rewards stay **temporarily** unlocked (`graceProtectedLevel`).
3. **Hard lock** — relapsing again during an active grace (or letting it expire via
   `evaluateExpiry`) drops protection back to the organically-earned level.

`evaluateExpiry(state, now)` (called opportunistically each tick) closes grace
either **successfully** (organic level caught back up to the protected level) or by
**expiry** (timer elapsed first). It returns the same instance when nothing changed,
so it’s cheap to call frequently and never triggers a needless save/emit.

Relapse history is capped at `MAX_LOG_ENTRIES = 500` (most recent kept).

---

## 5. Levels (`Levels` / `Level`)

- Six levels (0–5) with fixed titles: *Beginning, First Light, Momentum,
  One Week Strong, Steadfast, Transformed*.
- Day thresholds are **user-customizable** (Settings → Milestones) and sanitized to
  be strictly increasing with level 0 == 0.
- `effectiveLevel(now) = max(organicLevel, graceProtectedLevel during grace)`.
- `organicLevel(now)` = level earned purely from the current uninterrupted streak.

---

## 6. Persistence

### Recovery state — `RecoveryStateStore`
`EncryptedSharedPreferences` (AES-256-GCM values, AES-256-SIV keys) behind a
Keystore master key. Falls back to plain prefs only if the secure store can't be
created (rare device keystore issues) so the app never crashes on boot.

`RecoveryState` (persisted fields): streak start, relapse count, first-forgiveness
flag, best streak, relapse log, grace fields, onboarding flag, selected wallpaper,
`hapticsEnabled`, custom level thresholds, money (cents + currency), app-lock fields
(`appLockEnabled`, `biometricEnabled`, `pinHash`).

### Light prefs — `SettingsRepository` (DataStore)
Only `remindersEnabled`. Text size is **not** stored — it follows the device’s own
font-size setting (Compose `sp`).

### Backup — `BackupManager`
Versioned, validated JSON (`magic`/`version` checks, range clamps, catalog lookups).
**Security:** the app-lock PIN/flags are never exported and never restored — they’re
device-local. On import, `MainViewModel.importJson` re-applies the importing device’s
own lock settings to the restored state.

---

## 7. Feedback (haptics)

`FeedbackManager` provides short, distinct vibration patterns for CLICK / LEVEL_UP /
RELAPSE. **Haptics default to OFF** and are toggled in Settings. There are no UI
sounds or background music (the app ships no audio assets).

---

## 8. Notifications & background work

- `Notifications.createChannel` creates a single "Encouragement" channel.
- `ReminderWorker` (WorkManager `PeriodicWorkRequest`, ~daily, 3h initial delay)
  posts the day's motivational quote. It uses a dedicated monochrome small icon
  (`ic_notification`) and guards every step so it can never crash the app. Scheduling
  is enabled/cancelled from Settings and the permission request is wrapped defensively.

---

## 9. Theming & iconography

- `SelfReignTheme` — a single dark `colorScheme` + default `Typography` (one font,
  `sp`-based so it respects the device font scale). No in-app font option.
- `Wallpapers` — code-defined gradient `Brush`es, **cached** so they’re not
  reallocated on recomposition. Painted as the global background in `MainActivity`.
- App icon — adaptive vector (green leaf foreground on black) with a monochrome
  themed-icon layer and a pre-API-26 vector fallback. Logo design options live in
  `/logo` (SVG).

---

## 10. Complete file map

```
app/src/main/java/com/aldrenstudios/selfreign/
├── HabitApp.kt                 # Application; lazy repos + FeedbackManager; channel + refresh on start
├── MainActivity.kt             # FragmentActivity; gates Lock → Onboarding → App; paints wallpaper
│
├── audio/
│   └── FeedbackManager.kt      # Haptics only (opt-in)
│
├── data/
│   ├── Levels.kt               # Level model + customizable ladder
│   ├── RecoveryState.kt        # Persisted state + derived helpers; RelapseLogEntry; Trigger
│   ├── RecoveryStateStore.kt   # EncryptedSharedPreferences persistence
│   ├── RecoveryRepository.kt   # Single source of truth (StateFlow)
│   ├── RelapseEngine.kt        # Pure relapse/grace state machine
│   ├── SettingsRepository.kt   # DataStore prefs (reminders)
│   ├── StoreCatalog.kt         # Unlockable wallpapers
│   └── BackupManager.kt        # Versioned JSON backup + validation
│
├── ui/
│   ├── AppNavigation.kt        # Bottom nav + NavHost (fade-through transitions) + History route
│   ├── MainViewModel.kt        # Shared VM: clock, derived flows, relapse, store, lock, backup
│   ├── celebrate/CelebrationOverlay.kt
│   ├── components/ProgressRing.kt
│   ├── dashboard/DashboardScreen.kt   # No-scroll responsive hero layout
│   ├── dashboard/DetailSheets.kt      # Streak / Level bottom sheets
│   ├── dashboard/RelapseLogDialog.kt
│   ├── history/HistoryScreen.kt       # Full-page relapse history
│   ├── insights/InsightsScreen.kt     # Stats + trigger breakdown
│   ├── lock/LockScreen.kt             # PIN / biometric gate
│   ├── onboarding/OnboardingScreen.kt # Welcome + 3-step walkthrough
│   ├── panic/PanicSheet.kt            # Guided box-breathing
│   ├── rules/RulesScreen.kt
│   ├── settings/SettingsScreen.kt
│   ├── settings/SettingsViewModel.kt
│   ├── store/StoreScreen.kt           # Wallpaper rewards
│   └── theme/ (Color.kt, Theme.kt, Wallpapers.kt)
│
├── util/
│   ├── BackupIo.kt             # SAF read/write for backup files
│   ├── Notifications.kt        # Notification channel
│   ├── PinHasher.kt            # Salted PIN hashing/verification
│   ├── Quotes.kt               # Daily motivational quotes
│   ├── ReminderWorker.kt       # WorkManager daily reminder
│   └── TimeFormat.kt           # Duration / date formatting
│
└── widget/
    ├── StreakWidget.kt         # Glance widget + receiver
    └── WidgetUpdater.kt        # Best-effort updateAll()
```

---

## 11. Navigation

`AppNavigation` hosts a `Scaffold` with a `NavigationBar` (Dashboard, Insights,
Store, Rules, Settings) and a `NavHost`. **History** is a sub-page route reached from
the Dashboard "Total relapses" chip and the Insights "Total relapses" card. All
transitions use a quick fade-through + subtle scale.

`MainActivity` switches between three top-level states before navigation:
**Locked** (if app lock on) → **Onboarding** (first run) → **App**.

---

## 12. Key patterns & conventions

- **Reactive:** `Store/DataStore → StateFlow → ViewModel (derived) → collectAsStateWithLifecycle → Compose`.
- **Manual DI:** dependencies are lazy `val`s on `HabitApp`; ViewModels use nested
  `Factory` classes.
- **Recomposition scoping:** read `now` only in the smallest live composable; expose
  slow values via `distinctUntilChanged` derived flows.
- **Defensive by default:** persistence, backup import, reminder scheduling, haptics,
  and widget updates all swallow/contain failures rather than crashing.
- **No new dependencies / assets** without good reason — wallpapers and icons are code.
- **Naming:** Composables `PascalCase`, functions `camelCase`, constants `UPPER_SNAKE_CASE`.

---

## 13. Build & release

```bash
./gradlew :app:assembleDebug      # debug APK
./gradlew :app:assembleRelease    # minified (R8) release APK/AAB
```

- JDK 17, Gradle 8.7, AGP 8.5.2.
- Release is minified; keep `app/proguard-rules.pro` rules for Compose, Glance,
  WorkManager (`ListenableWorker` subclasses), and Tink.
- For Play: configure a signing key, run `lintRelease`, and upload an **AAB**.

---

## 14. Common tasks

| Task | Where |
|------|-------|
| Add a quote | `util/Quotes.kt` |
| Change level titles / count | `data/Levels.kt` |
| Tune the relapse/grace rules | `data/RelapseEngine.kt` |
| Add a new wallpaper | `data/StoreCatalog.kt` + a gradient in `ui/theme/Wallpapers.kt` |
| Add a persisted field | `RecoveryState` + `RecoveryStateStore` (load/save) + `BackupManager` |
| Add a screen | new composable in `ui/`, add a `Dest` + `composable()` in `AppNavigation.kt` |
| Change the app icon | swap the leaf path in `res/drawable/ic_launcher_foreground.xml` (+ monochrome / mipmap-anydpi / `ic_notification`); options in `/logo` |

---

*Keep this document updated when architecture changes. Last refreshed for the
1.0.0 release pass: dead audio removed, single font, no-scroll dashboard, derived-state
performance model, vector logo.*
