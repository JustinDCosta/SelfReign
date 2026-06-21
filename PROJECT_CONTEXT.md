# Bad Habit Tracker — Complete Project Context

> **Purpose:** This document provides complete technical context for AI models working on this Android app. It includes architecture, data flow, file structure, key patterns, and implementation decisions.

---

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Data Flow Diagrams](#data-flow-diagrams)
4. [Complete File Structure](#complete-file-structure)
5. [Module Documentation](#module-documentation)
6. [Key Implementation Patterns](#key-implementation-patterns)
7. [Database Schema](#database-schema)
8. [Navigation Flow](#navigation-flow)
9. [Build Configuration](#build-configuration)
10. [Common Tasks](#common-tasks)

---

## Project Overview

**App Name:** SelfReign  
**Package:** `com.aldrenstudios.selfreign`  
**Purpose:** Help users quit/reduce bad habits by tracking clean time, logging relapses with optional notes, and providing daily motivation.

**Key Characteristics:**
- 100% offline (no network permission, no cloud)
- Privacy-first (GDPR-aligned, on-device only)
- AMOLED-optimized dark themes
- Jetpack Compose UI with Material 3
- Min SDK 24, Target SDK 34

---

## Architecture

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ HomeScreen   │  │HistoryScreen │  │SettingsScreen│     │
│  │   (Timer)    │  │  (Log List)  │  │ (Preferences)│     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                  │                  │              │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────▼───────┐     │
│  │HomeViewModel │  │HistoryVM     │  │SettingsVM    │     │
│  │(1Hz ticker + │  │              │  │              │     │
│  │ combine data)│  │              │  │              │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
└─────────┼──────────────────┼──────────────────┼─────────────┘
          │                  │                  │
┌─────────▼──────────────────▼──────────────────▼─────────────┐
│                      Domain Layer                            │
│  ┌────────────────────┐          ┌────────────────────┐     │
│  │  HabitRepository   │          │ SettingsRepository │     │
│  │  • logRelapse()    │          │ • setTheme()       │     │
│  │  • bestStreak()    │          │ • setFontSize()    │     │
│  │  • clearHistory()  │          │ • setReminders()   │     │
│  └─────────┬──────────┘          └─────────┬──────────┘     │
└───────────┼─────────────────────────────────┼────────────────┘
            │                                 │
┌───────────▼─────────────────────────────────▼────────────────┐
│                       Data Layer                             │
│  ┌────────────────────┐          ┌────────────────────┐     │
│  │   Room Database    │          │   DataStore        │     │
│  │ ┌────────────────┐ │          │  (Preferences)     │     │
│  │ │ RelapseEvent   │ │          │  • theme           │     │
│  │ │ • id           │ │          │  • fontSize        │     │
│  │ │ • timestamp    │ │          │  • reminders       │     │
│  │ │ • note         │ │          │                    │     │
│  │ └────────────────┘ │          │                    │     │
│  │ ┌────────────────┐ │          │                    │     │
│  │ │  RelapseDao    │ │          │                    │     │
│  │ │  (Queries)     │ │          │                    │     │
│  │ └────────────────┘ │          │                    │     │
│  └────────────────────┘          └────────────────────┘     │
│         (habit.db)                  (settings prefs)        │
└──────────────────────────────────────────────────────────────┘
```

### Layer Responsibilities

| Layer | Components | Purpose |
|-------|------------|---------|
| **UI** | Screens (Composables) | User interaction, visual presentation |
| **ViewModel** | ViewModels | State management, reactive data streams |
| **Domain** | Repositories | Business logic, data orchestration |
| **Data** | Room DB, DataStore | Persistence, queries |

---

## Data Flow Diagrams

### 1. Timer Flow (Home Screen)

```
┌──────────────────────────────────────────────────────────────┐
│                     HomeViewModel                            │
│                                                              │
│  ticker (Flow<Long>)                                         │
│  emit every 1000ms ────┐                                     │
│                        │                                     │
│  repo.lastRelapseTimestamp (Flow<Long?>) ──┐                │
│  repo.relapseCount (Flow<Int>) ────────────┼──┐             │
│  repo.bestStreakMillis() (Flow<Long>) ─────┼──┼──┐          │
│                        │                    │  │  │          │
│                        └────────────────────┴──┴──┴──────┐   │
│                                  combine()               │   │
│                                     │                    │   │
│                                     ▼                    │   │
│                        ┌────────────────────────┐        │   │
│                        │   HomeUiState          │        │   │
│                        │ • elapsedMillis        │◄───────┘   │
│                        │ • relapseCount         │            │
│                        │ • bestStreakMillis     │            │
│                        │ • quote                │            │
│                        └────────┬───────────────┘            │
└─────────────────────────────────┼────────────────────────────┘
                                  │
                                  ▼
                          ┌───────────────┐
                          │  HomeScreen   │
                          │  (Compose UI) │
                          └───────────────┘
```

**Key insight:** Timer is derived, not stored. `elapsedMillis = now - lastRelapseTimestamp`. When a relapse is logged, the DB updates `lastRelapseTimestamp`, and the UI updates automatically via Flow.

### 2. Relapse Logging Flow

```
User taps "I Relapsed"
        │
        ▼
┌───────────────────┐
│  RelapseDialog    │ ◄── Optional note input
└────────┬──────────┘
         │ confirm
         ▼
┌─────────────────────────────────────────────┐
│ HomeViewModel.logRelapse(note)              │
│   viewModelScope.launch {                   │
│     repo.logRelapse(now(), note)            │
│   }                                         │
└────────┬────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│ HabitRepository.logRelapse(ts, note)        │
│   dao.insert(RelapseEvent(...))            │
└────────┬────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│ Room Database (relapse_events table)        │
│   INSERT new row                            │
└────────┬────────────────────────────────────┘
         │
         ▼ (Flow emits updated data)
┌─────────────────────────────────────────────┐
│ dao.observeLastTimestamp() emits new value  │
│ dao.observeCount() emits incremented count  │
└────────┬────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│ HomeViewModel's combine() recomputes state  │
│ Timer resets automatically                  │
└─────────────────────────────────────────────┘
```

### 3. Settings → Theme Flow

```
User selects "Midnight Blue"
        │
        ▼
┌───────────────────────────────────────┐
│ SettingsViewModel.setTheme(MIDNIGHT) │
│   repo.setTheme(MIDNIGHT)             │
└────────┬──────────────────────────────┘
         │
         ▼
┌────────────────────────────────────────┐
│ SettingsRepository                     │
│   dataStore.edit { it[THEME] = value } │
└────────┬───────────────────────────────┘
         │
         ▼ (Flow emits new UserSettings)
┌────────────────────────────────────────┐
│ MainActivity observes settings         │
│   settings.collectAsStateWithLifecycle │
└────────┬───────────────────────────────┘
         │
         ▼
┌────────────────────────────────────────┐
│ selfreignTheme(theme, fontSize)  │
│   MaterialTheme recomposes with new    │
│   colorScheme                          │
└────────────────────────────────────────┘
         │
         ▼
   All screens update instantly
```

---

## Complete File Structure

```
selfreign/
├── README.md                          # User documentation
├── PROJECT_CONTEXT.md                 # This file (technical context)
├── build.gradle.kts                   # Root Gradle config (plugin versions)
├── settings.gradle.kts                # Module declarations
├── gradle.properties                  # JVM args, AndroidX flag
├── .gitignore                         # Excludes build/, .idea/, etc.
│
├── gradle/wrapper/
│   └── gradle-wrapper.properties      # Gradle 8.7 distribution URL
│
├── gradlew                            # Unix Gradle wrapper
├── gradlew.bat                        # Windows Gradle wrapper
│
└── app/
    ├── build.gradle.kts               # App module config (deps, SDK versions)
    ├── proguard-rules.pro             # R8/ProGuard rules for release builds
    │
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml    # App declaration, permissions, activity
        │   │
        │   ├── res/
        │   │   ├── values/
        │   │   │   ├── strings.xml              # All UI strings (i18n-ready)
        │   │   │   ├── themes.xml               # XML theme (splash, system bars)
        │   │   │   └── ic_launcher_background.xml  # Launcher bg color
        │   │   │
        │   │   ├── drawable/
        │   │   │   └── ic_launcher_foreground.xml  # Launcher icon vector
        │   │   │
        │   │   ├── mipmap-anydpi/             # Pre-API 26 launcher icons
        │   │   │   ├── ic_launcher.xml
        │   │   │   └── ic_launcher_round.xml
        │   │   │
        │   │   ├── mipmap-anydpi-v26/         # Adaptive icons (API 26+)
        │   │   │   ├── ic_launcher.xml
        │   │   │   └── ic_launcher_round.xml
        │   │   │
        │   │   └── xml/
        │   │       └── data_extraction_rules.xml  # Disable backup (privacy)
        │   │
        │   └── java/com/example/selfreign/
        │       ├── HabitApp.kt                # Application class + DI container
        │       ├── MainActivity.kt            # Single activity, hosts Compose
        │       │
        │       ├── data/                      # Data layer
        │       │   ├── RelapseEvent.kt        # Room @Entity
        │       │   ├── RelapseDao.kt          # Room @Dao (Flow queries)
        │       │   ├── HabitDatabase.kt       # Room DB singleton
        │       │   ├── HabitRepository.kt     # Relapse data orchestration
        │       │   └── SettingsRepository.kt  # DataStore preferences wrapper
        │       │
        │       ├── ui/                        # UI layer
        │       │   ├── AppNavigation.kt       # Bottom nav + NavHost
        │       │   │
        │       │   ├── theme/                 # Compose theming
        │       │   │   ├── Color.kt           # Color palette definitions
        │       │   │   ├── Type.kt            # Typography + scaling
        │       │   │   └── Theme.kt           # selfreignTheme composable
        │       │   │
        │       │   ├── components/            # Reusable UI
        │       │   │   └── RelapseDialog.kt   # Note input dialog
        │       │   │
        │       │   ├── home/                  # Home screen + ViewModel
        │       │   │   ├── HomeViewModel.kt   # State + timer logic
        │       │   │   └── HomeScreen.kt      # UI (timer, stats, quote)
        │       │   │
        │       │   ├── history/               # History screen + ViewModel
        │       │   │   ├── HistoryViewModel.kt
        │       │   │   └── HistoryScreen.kt   # Relapse log list
        │       │   │
        │       │   └── settings/              # Settings screen + ViewModel
        │       │       ├── SettingsViewModel.kt
        │       │       └── SettingsScreen.kt  # Theme, font, reminders
        │       │
        │       └── util/                      # Utilities
        │           ├── Quotes.kt              # Motivational quote rotation
        │           ├── TimeFormat.kt          # Duration formatting
        │           ├── Notifications.kt       # Notification channel setup
        │           └── ReminderWorker.kt      # WorkManager daily reminder
        │
        └── test/java/com/example/selfreign/
            └── TimeFormatTest.kt              # Unit tests for time logic
```

---

## Module Documentation

### Data Layer

#### **RelapseEvent.kt** (Entity)
```kotlin
@Entity(tableName = "relapse_events")
data class RelapseEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,      // Epoch millis
    val note: String? = null  // Optional user note
)
```

#### **RelapseDao.kt** (Data Access)
| Method | Return | Purpose |
|--------|--------|---------|
| `insert(event)` | `Long` | Inserts relapse, returns generated ID |
| `observeAll()` | `Flow<List<RelapseEvent>>` | All relapses, newest first (for History) |
| `observeLastTimestamp()` | `Flow<Long?>` | Most recent relapse time (drives timer) |
| `observeCount()` | `Flow<Int>` | Total relapse count |
| `observeTimestampsAsc()` | `Flow<List<Long>>` | All timestamps ascending (for streak calc) |
| `clearAll()` | `suspend` | Deletes all events |

All `observe*` methods return Flows that emit new values automatically when the DB changes.

#### **HabitRepository.kt**
**Streak Computation Logic:**
```kotlin
fun bestStreakMillis(now: () -> Long): Flow<Long> =
    dao.observeTimestampsAsc().map { timestamps ->
        if (timestamps.isEmpty()) return@map 0L
        var best = 0L
        // Compare consecutive relapses
        for (i in 1 until timestamps.size) {
            best = maxOf(best, timestamps[i] - timestamps[i - 1])
        }
        // Include ongoing streak (last relapse → now)
        best = maxOf(best, now() - timestamps.last())
        best
    }
```

#### **SettingsRepository.kt**
Stores preferences as DataStore key-value pairs:
- `THEME` → `"AMOLED" | "DARK_GRAY" | "MIDNIGHT_BLUE"`
- `FONT_SIZE` → `"SMALL" | "MEDIUM" | "LARGE"`
- `REMINDERS` → `true | false`

Exposed as a single `Flow<UserSettings>` data class.

---

### UI Layer

#### **HomeViewModel.kt** — Timer Logic
```kotlin
// Ticker emits current time every second
private val ticker: Flow<Long> = flow {
    while (true) {
        emit(System.currentTimeMillis())
        delay(1000)
    }
}

// Combine ticker with DB data to derive state
val uiState: StateFlow<HomeUiState> = combine(
    ticker,
    repo.lastRelapseTimestamp,
    repo.relapseCount,
    repo.bestStreakMillis { System.currentTimeMillis() }
) { now, lastTs, count, best ->
    val elapsed = if (lastTs != null) now - lastTs else 0L
    HomeUiState(elapsed, count, best, lastTs != null, Quotes.forDay(now))
}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
```

**Key Pattern:** Timer is **derived**, not stored. The UI always recalculates `elapsed = now - lastRelapseTimestamp`, so it survives process death and never drifts.

#### **AppNavigation.kt** — Bottom Nav Structure
Three destinations:
1. **HOME** (`home`) → `HomeScreen`
2. **HISTORY** (`history`) → `HistoryScreen`
3. **SETTINGS** (`settings`) → `SettingsScreen`

Navigation uses `singleTop + popUpTo(startDestination)` to avoid stack buildup.

#### **Theme System**
Three dark color schemes (all true-dark, no light mode):
- **AMOLED** → `#000000` background (true black for max battery savings)
- **DARK_GRAY** → `#121212` background
- **MIDNIGHT_BLUE** → `#0B1020` background

Typography scales by `FontSizeOption.scale` (0.9f / 1.0f / 1.15f) applied to all text styles.

---

## Key Implementation Patterns

### 1. Reactive State Management
```
Room/DataStore → Flow → ViewModel combine() → StateFlow → Compose UI
```
- DB queries return `Flow<T>`
- ViewModels `combine()` multiple flows into UI state
- Screens use `collectAsStateWithLifecycle()` for lifecycle-aware collection
- Result: UI updates automatically when data changes, no manual refresh

### 2. Manual Dependency Injection
```kotlin
// HabitApp.kt
class HabitApp : Application() {
    val habitRepository by lazy {
        HabitRepository(HabitDatabase.get(this).relapseDao())
    }
    val settingsRepository by lazy {
        SettingsRepository(this)
    }
}

// Usage in Composables
val app = application as HabitApp
val vm: HomeViewModel = viewModel(
    factory = HomeViewModel.Factory(app.habitRepository)
)
```
No Hilt/Dagger — simple property delegation for this app size.

### 3. ViewModelProvider.Factory Pattern
Every ViewModel has a nested `Factory` class:
```kotlin
class HomeViewModel(private val repo: HabitRepository) : ViewModel() {
    // ...
    class Factory(private val repo: HabitRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(repo) as T
    }
}
```
Allows passing repository dependencies without DI framework.

### 4. Sealed Enums for Settings
```kotlin
enum class ThemeOption { AMOLED, DARK_GRAY, MIDNIGHT_BLUE }
enum class FontSizeOption(val scale: Float) {
    SMALL(0.9f), MEDIUM(1.0f), LARGE(1.15f)
}
```
Type-safe, persisted as enum names in DataStore.

---

## Database Schema

### Table: `relapse_events`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | INTEGER | PRIMARY KEY AUTOINCREMENT | Unique event ID |
| `timestamp` | INTEGER | NOT NULL | Epoch milliseconds when logged |
| `note` | TEXT | NULLABLE | Optional user note (trigger/feeling) |

**Indexes:** None (table is small; primary key index sufficient).

**Queries:**
- `SELECT * ORDER BY timestamp DESC` → History screen
- `SELECT MAX(timestamp)` → Timer calculation
- `SELECT COUNT(*)` → Total relapse count
- `SELECT timestamp ORDER BY timestamp ASC` → Best streak

---

## Navigation Flow

```
MainActivity (single activity)
    │
    └── AppNavigation (Scaffold + NavHost)
            │
            ├── home → HomeScreen
            │     ↕ (dialog)
            │   RelapseDialog (log relapse with note)
            │
            ├── history → HistoryScreen
            │     ↕ (dialog)
            │   Clear History Confirmation
            │
            └── settings → SettingsScreen
                  ↕ (permission request)
                POST_NOTIFICATIONS (Android 13+)
```

**Bottom Navigation Behavior:**
- Tap same item twice → no action
- Tap different item → navigate, pop back stack to start, save/restore state
- No deep linking currently implemented

---

## Build Configuration

### SDK Versions
```kotlin
minSdk = 24    // Android 7.0 (covers 95%+ devices)
targetSdk = 34 // Android 14 (latest)
compileSdk = 34
```

### Key Dependencies
| Dependency | Version | Purpose |
|------------|---------|---------|
| `androidx.core:core-ktx` | 1.13.1 | Kotlin extensions |
| `androidx.compose:compose-bom` | 2024.06.00 | Compose version alignment |
| `androidx.room:room-ktx` | 2.6.1 | Local database |
| `androidx.datastore:datastore-preferences` | 1.1.1 | Key-value settings |
| `androidx.work:work-runtime-ktx` | 2.9.0 | Background reminders |
| `androidx.navigation:navigation-compose` | 2.7.7 | Screen navigation |

### ProGuard/R8 Rules
```proguard
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep class androidx.compose.** { *; }
```

### Gradle JVM Args
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
```

---

## Common Tasks

### Add a New Screen
1. Create `ui/newscreen/NewScreen.kt` (composable)
2. Create `ui/newscreen/NewViewModel.kt` (+ Factory)
3. Add route to `AppNavigation.kt` enum `Dest`
4. Add `composable()` in `NavHost` in `AppNavigation.kt`
5. Add icon + string resources

### Add a Database Field
1. Modify entity in `data/RelapseEvent.kt`
2. Increment `@Database(version = X)` in `HabitDatabase.kt`
3. Provide migration or use `fallbackToDestructiveMigration()`
4. Update DAO queries if needed
5. Update repository/ViewModel to expose new field

### Add a New Setting
1. Add enum option to `data/SettingsRepository.kt`
2. Add DataStore key + read/write methods
3. Update `UserSettings` data class
4. Add UI controls in `ui/settings/SettingsScreen.kt`
5. Use setting in `MainActivity` or relevant ViewModel

### Change App Name
1. Update `app_name` in `res/values/strings.xml`
2. Update `android:label` in `AndroidManifest.xml` (or remove to use strings.xml)

### Change Package Name
**Warning:** Requires refactor across 50+ files. Use Android Studio:
1. Right-click package in Project view
2. Refactor → Rename
3. Check "Rename package" + "Search in comments/strings"
4. Update `applicationId` in `app/build.gradle.kts`
5. Update `namespace` in `app/build.gradle.kts`

---

## Privacy & Compliance Notes

**GDPR Alignment:**
- **Data minimisation:** Only timestamp + optional note stored
- **Purpose limitation:** Data used only for habit tracking
- **Storage limitation:** User can clear all data instantly
- **On-device processing:** No network calls, no third parties
- **Right to erasure:** "Clear history" permanently deletes all events
- **No consent needed:** No tracking, analytics, or ads

**Backup Disabled:**
```xml
<!-- AndroidManifest.xml -->
<application
    android:allowBackup="false"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="false">
```

```xml
<!-- data_extraction_rules.xml -->
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="root" path="." />
    </cloud-backup>
    <device-transfer>
        <exclude domain="root" path="." />
    </device-transfer>
</data-extraction-rules>
```

---

## Testing Strategy

### Unit Tests (JUnit)
- `TimeFormatTest.kt` — Duration math, edge cases
- **Coverage:** Time utilities, business logic

### UI Tests (Compose)
- None currently implemented
- **Recommended:** Test relapse dialog, navigation flows

### Manual Testing Checklist
- [ ] Timer updates every second
- [ ] Relapse resets timer
- [ ] History shows logged events with notes
- [ ] Clear history confirmation works
- [ ] Theme changes apply instantly
- [ ] Font size changes scale all text
- [ ] Reminder toggle schedules WorkManager job
- [ ] App survives process death (data persists)
- [ ] Motivational quote rotates daily

---

## Known Limitations

1. **Single habit only** — can't track multiple habits in parallel
2. **No data export** — no CSV/JSON export (future feature)
3. **No biometric lock** — app data visible to anyone with device access
4. **No widgets** — timer not visible without opening app
5. **Fixed reminder time** — WorkManager schedules roughly daily, no time control
6. **English only** — strings are i18n-ready but only EN provided

---

## Future Enhancement Ideas

Ranked by implementation effort (easiest first):

1. **Money saved counter** — multiply days × cost/day
2. **Milestone badges** — celebrate 1d/7d/30d/100d streaks
3. **Home screen widget** — show timer without opening app
4. **Multiple habits** — parallel tracking with separate timers
5. **Trigger tagging** — categorize relapses (stress/social/boredom)
6. **Data export** — encrypted backup file for user-controlled backup
7. **Adaptive reminders** — schedule based on user's risky times
8. **Panic mode** — breathing exercise or distraction on craving
9. **Biometric lock** — secure app with fingerprint/face
10. **Social accountability** — generate shareable streak image

---

## Debugging Commands

```bash
# View logs (filter by package)
adb logcat | grep "selfreign"

# Inspect database (requires root or debuggable build)
adb shell
run-as com.aldrenstudios.selfreign
cd databases
sqlite3 habit.db
.tables
SELECT * FROM relapse_events;

# Clear app data
adb shell pm clear com.aldrenstudios.selfreign

# Reinstall APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Trigger WorkManager job immediately (testing)
adb shell am broadcast -a "androidx.work.diagnostics.REQUEST_DIAGNOSTICS" \
  -p com.aldrenstudios.selfreign
```

---

## Code Style & Conventions

- **Language:** Kotlin 1.9.24, JVM target 17
- **Null safety:** Prefer `?` and `?.let {}` over `!!`
- **Immutability:** Use `val` over `var`, `data class` for DTOs
- **Async:** `suspend` + `viewModelScope.launch` for coroutines
- **Compose:** Stateless composables + hoist state to ViewModels
- **Naming:**
  - Composables: `PascalCase` (e.g., `HomeScreen`)
  - Functions: `camelCase` (e.g., `logRelapse`)
  - Constants: `UPPER_SNAKE_CASE` (e.g., `CHANNEL_ID`)

---

## Contact & Maintenance

**Build Requirements:**
- Android Studio Hedgehog+ (or IntelliJ IDEA with Android plugin)
- JDK 17
- Android SDK 34
- Gradle 8.7 (via wrapper)

**First Build Steps:**
1. Open project in Android Studio
2. Wait for Gradle sync (downloads wrapper JAR + dependencies)
3. Run on device/emulator (min API 24)

**Typical build time:** 30-60s on first sync, 5-10s incremental.

---

## Glossary

| Term | Definition |
|------|------------|
| **AMOLED** | Active Matrix Organic LED — display tech where black pixels = off = battery savings |
| **DataStore** | Jetpack library for async, type-safe key-value storage (replaces SharedPreferences) |
| **Flow** | Kotlin Coroutines reactive stream that emits values over time |
| **Room** | Jetpack library providing SQLite abstraction with compile-time query verification |
| **StateFlow** | Hot Flow that always has a value and caches the latest emission |
| **WorkManager** | Jetpack library for deferrable, guaranteed background tasks |
| **Composable** | Kotlin function annotated with `@Composable` that emits UI |
| **Recomposition** | Compose re-executing composables when state changes |

---

**End of Project Context Document**  
*Last updated: 2026-06-04*  
*For questions or clarifications, refer to inline code comments or README.md*

