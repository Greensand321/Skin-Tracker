# Skin Tracker — CLAUDE.md

A guide to the codebase for AI assistants and developers working on the Skin Tracker app.

> **This doc is scoped to `android-skin/` only.** Skin Tracker is a self-contained Gradle
> project (package `com.skintracker`) that can be lifted into its own repository at any
> time. Keep this guide accurate to *this* folder.

---

## Project Overview

Skin Tracker is a **native Android** app intended for logging skin-condition symptoms,
built in **Kotlin + Jetpack Compose**. It runs fully offline, stores all data locally on
the device (no account, no server, no sync), and is designed around one principle:

> **Record everything with the fewest clicks possible.**

It is a **derivative of the Keto Tracker** native Android app, sharing its architecture
and friction-free logging workflow (time-aware starting step, auto-advancing ratings,
one-tap chips, instant auto-save, inline editing, swipe navigation).

### ⚠️ Current state — mirror + rebrand (read this first)

This module is a faithful **mirror + rebrand** of Keto Tracker:

- **Rebranded** (application identity): Kotlin package and `applicationId`/`namespace` are
  `com.skintracker`; the Gradle root project is `SkinTracker`; the app name / launcher
  label is **Skin Tracker**; the app theme style is `Theme.SkinTracker`; user-visible
  brand strings say "Skin Tracker".
- **Unchanged** (domain): the data model and 7-step wizard are still the **keto** domain
  (meals, ketone "tested" flag, per-meal keto stamps, energy/mood/portion ratings, heart
  health, supplements). Repurposing these to skin symptoms is the planned next step.
- **Intentionally retained internal names**: to keep the copy low-risk and building as-is,
  several internal Kotlin symbols and resource/DB names kept their original "Keto"
  spelling — they are not user-visible. See **Naming map** below. Don't "fix" these
  casually; the domain field names in particular are also JSON serialization keys.

**Tech stack**: Kotlin, Jetpack Compose (Material 3), Room, DataStore Preferences,
kotlinx.serialization, Coil, WorkManager.

---

## Naming map (rebranded vs. retained)

| Concept | Value in this module | Rebranded? |
|---|---|---|
| Kotlin package | `com.skintracker` | ✅ rebranded |
| `applicationId` / `namespace` | `com.skintracker` | ✅ rebranded |
| Gradle root project | `SkinTracker` | ✅ rebranded |
| App name (`app_name`) / launcher label | Skin Tracker | ✅ rebranded |
| App theme style (XML) | `Theme.SkinTracker` | ✅ rebranded |
| Root Compose wrapper | `KetoTracker(themeId) { … }` | ⬜ kept |
| Theme palette / map | `KetoColors`, `KETO_THEMES`, `LocalKetoColors`, `KetoTheme` | ⬜ kept |
| Room `@Database` / file | `KetoDatabase` / `KETO_DB_NAME = "keto_tracker.db"` | ⬜ kept |
| DataStore name | `keto_prefs` | ⬜ kept |
| Color resources | `keto_bg`, `keto_gold` | ⬜ kept |
| Domain fields | `breakfastKeto`, `lunchKeto`, `dinnerKeto`, `notInKeto`, `tested`, … | ⬜ kept (also JSON keys) |

Because these `keto_*` storage/DB names are sandboxed under `com.skintracker`, they never
collide with the Keto Tracker app — the two apps install side-by-side.

---

## File Structure

```
android-skin/app/src/main/java/com/skintracker/
├── MainActivity.kt              # Single activity; hosts the Compose tree, resolves auto-theme
├── data/
│   ├── DayEntry.kt              # Core data model (one day's log)
│   ├── DayEntrySurrogate.kt     # @Serializable surrogate + DayEntry ⇄ JSON mapping
│   ├── Heart.kt                 # Heart enum (GOOD/MILD/BAD) + manual KSerializer
│   ├── Meal.kt                  # Meal enum (BREAKFAST/LUNCH/DINNER)
│   ├── Steps.kt                 # Step enum (7-step wizard) + label/placeholder/supplement constants
│   ├── Snapshot.kt              # Snapshot metadata model (feature partially built)
│   ├── DateUtils.kt             # ISO date-key helpers (todayKey, offKey, fmtDate, isToday/isFuture, monthGrid)
│   ├── db/                      # Room: DayEntryEntity (date PK + JSON data), DayEntryDao, KetoDatabase
│   ├── repository/              # IDayRepository + DayRepository (Room) + FakeDayRepository (previews)
│   ├── prefs/PrefsStore.kt      # DataStore<Preferences> — theme id + auto-theme prefs
│   ├── photo/                   # PhotoStore (on-disk JPEGs) + CameraCapture (FileProvider target)
│   ├── notifications/           # NotificationHelper — reminder channel + builder
│   └── io/                      # DataPortability (export/import), SnapshotStore, ZipPortability, StorageStats
├── model/AppViewModel.kt        # Single ViewModel for the whole app
├── work/                        # BackupWorker (periodic JSON backup) + ReminderWorker (daily notification)
└── ui/
    ├── theme/                   # KetoTheme.kt (KetoColors, KETO_THEMES, THEME_LIST, KetoTracker()), Type.kt
    └── components/ + screens/   # Compose UI (see "UI Layer")
```

Other notable paths:
- `app/src/main/AndroidManifest.xml` — `POST_NOTIFICATIONS`, camera `<queries>`,
  FileProvider, launcher activity, `Theme.SkinTracker` splash style.
- `app/src/main/res/` — `strings.xml` (`app_name` = Skin Tracker), `themes.xml`
  (`Theme.SkinTracker`), `colors.xml` (`keto_bg`/`keto_gold`), launcher icons.
- `app/src/test/java/com/skintracker/` — JVM unit tests for the data layer.
- `app/build.gradle.kts`, `build.gradle.kts`, `settings.gradle.kts` — Gradle config.

---

## Architecture

A conventional **MVVM + Repository** structure on top of Compose. Data flows one direction
— persistence → repository → ViewModel (Compose `State`) → UI — and user actions flow
back: UI → ViewModel methods → repository (async) → persistence.

```
Room / DataStore / PhotoStore  →  IDayRepository / PrefsStore  →  AppViewModel (State)  →  Compose UI
                                                              ←  ViewModel methods  ←  user actions
```

- **Single activity, no Navigation library.** `MainActivity` hosts the whole Compose tree.
  `WizardScreen` holds an `Overlay` enum
  (`NONE/THEME/OVERVIEW/CALENDAR/SUPPLEMENTS/QUICK_SELECT/SETTINGS`) and draws the active
  overlay as a sibling `Box` on top of the wizard.
- **DI by factory.** `AppViewModel` takes an `IDayRepository` + nullable `PrefsStore`:
  - `AppViewModel.factory(application)` — production: `KetoDatabase` → `DayRepository`
    (Room) + `PrefsStore` (DataStore).
  - `AppViewModel.preview()` — design-time: `FakeDayRepository` (in-memory, seeded) and no
    `PrefsStore`, so every `@Preview` renders without touching disk.

---

## Data Model

### `DayEntry` (one day's log) — `data/DayEntry.kt`

```kotlin
data class DayEntry(
    val date: String,                       // "YYYY-MM-DD" (primary key)
    val breakfast: String = "",
    val lunch: String = "",
    val dinner: String = "",
    val energy: Int? = null,                // 1–5 or null
    val happiness: Int? = null,             // 1–5 or null
    val portion: Int? = null,               // 1–5 or null
    val notInKeto: Boolean = false,
    val tested: Boolean = false,
    val notes: String = "",
    val breakfastKeto: Boolean = false,     // per-meal "Keto" stamp
    val lunchKeto: Boolean = false,
    val dinnerKeto: Boolean = false,
    val breakfastTime: String? = null,      // timestamp set when meal stamped keto
    val lunchTime: String? = null,
    val dinnerTime: String? = null,
    val heart: Heart? = null,               // GOOD / MILD / BAD
    val heartNotes: String = "",
    val supplements: Map<String, Int> = emptyMap(),
)
```

> When repurposing to skin symptoms, this is the class to evolve. Field names here are
> **also JSON serialization keys** (via `DayEntrySurrogate`), so renaming a field changes
> the on-disk/export format — plan migrations accordingly.

`Heart` is a plain enum with a hand-written `HeartSerializer` (the generated
`@Serializable` companion breaks the Compose preview renderer).

### Persistence — Room with a JSON column (zero-migration design)

The `day_entries` table has just two columns:

```
date TEXT PRIMARY KEY     —  "2026-06-08"
data TEXT                 —  the full DayEntry, serialized to JSON
```

`DayRepository` uses `kotlinx.serialization`
(`Json { ignoreUnknownKeys = true; encodeDefaults = true }`) to convert `DayEntry ⇄ JSON`
via `DayEntrySurrogate`. Because the table schema never changes, **adding a new field to
`DayEntry` only requires giving it a default value** — no Room `Migration`, no table
rebuild. Old rows deserialize with the default for the new field.

### Preferences — DataStore — `data/prefs/PrefsStore.kt`

`PrefsStore` wraps DataStore Preferences and persists the active theme id, an
`autoThemeEnabled` flag, and separate "night"/"light" theme ids for auto mode — each a
`Flow`, collected by `AppViewModel` on init.

### Photos — on-disk JPEGs — `data/photo/`

Photos live outside the `DayEntry` JSON, as files:
- **`PhotoStore`** owns `filesDir/photos/` and stores compressed JPEGs named
  `{date}_{meal}_{timestamp}.jpg` (unique-forever names so Coil's path cache never serves
  stale bytes).
- **Capture**: `createCaptureTarget()` hands the system camera app a `content://` URI via
  `FileProvider` + `ActivityResultContracts.TakePicture()` — **no `CAMERA` permission**.
  Temp files live in `cacheDir/captures/`; `clearStaleCaptures()` sweeps on launch.
- **Compression**: `PhotoStore.addFromCapture()` decodes with a memory-safe `inSampleSize`,
  corrects EXIF orientation, downscales to ≤900 px long edge, re-encodes JPEG at quality
  75. Max 5 photos per meal.
- **State**: `AppViewModel` exposes a `photoTick` counter the UI reads to re-list a meal's
  photos after add/remove.

---

## Application State — `AppViewModel`

One `ViewModel` drives the whole app, holding Compose `State`:

| State | Description |
|---|---|
| `viewedKey: String` | Currently viewed date key (`YYYY-MM-DD`) |
| `stepIndex: Int` | Current wizard step index |
| `entry: DayEntry` | In-memory copy of the viewed day being edited |
| `allEntries: Map<String, DayEntry>` | Full log, kept in memory for Overview / calendar |
| `themeId: String` | Active theme id (plus auto-theme state) |
| `pendingImport: PendingImport?` | Import confirmation summary (counts only) |
| `messages` | `Channel<String>` → Compose `SnackbarHost` |

All UI-driving fields are `by mutableStateOf(...)`. The core cycle is **mutate → save
(async) → recompose (automatic)**: `update { transform }` applies the change to `entry`
immediately and launches a `viewModelScope` coroutine to persist via the repository.

---

## Wizard Steps — `data/Steps.kt`

A **7-step** daily logging wizard, driven by the `Step` enum.

| Index | `Step` | Field(s) | Behaviour |
|---|---|---|---|
| 0 | `BREAKFAST` | `breakfast` (+ photo, keto stamp) | Free text; Next / Keto / Skip |
| 1 | `LUNCH` | `lunch` (+ photo, keto stamp) | Free text; Next / Keto / Skip |
| 2 | `DINNER` | `dinner` (+ photo, keto stamp) | Free text; Next / Keto / Skip |
| 3 | `RATINGS` | `energy`, `happiness`, `portion` | 1–5 each; auto-advance ~380 ms after the last |
| 4 | `HEART` | `heart`, `heartNotes` | Good/Mild/Bad; auto-advances on "Good", notes otherwise |
| 5 | `FLAGS` | `notInKeto`, `tested`, `supplements`, `notes` | Combined Flags & Notes page |
| 6 | `SUMMARY` | — | Read-only recap with inline per-field edit |

`Step.dotted` is every step except `SUMMARY`. Rating labels, placeholders, and default
supplement chips also live in `Steps.kt`.

**Behaviour rules**: meal steps show Next / Keto / Skip (the photo area renders below the
action row so buttons stay visible with the keyboard open); ratings auto-advance; heart
auto-advances on "Good"; text fields are always skippable; viewing a past day jumps to the
read-only summary; smart-start opens the time-relevant step and the first incomplete field.

---

## UI Layer — Jetpack Compose

### Theming — `ui/theme/KetoTheme.kt`
- `KetoColors` — palette data class (`bg`, `surf`, `accent`, `gold`, `red`, `blue`, `txt`, …).
- `KETO_THEMES` — map of all **14 themes** (8 dark + 6 light) to `KetoColors`.
- `THEME_LIST` — ordered `ThemeInfo(id, emoji, label, dark)` for the picker grid.
- `LocalKetoColors` — `CompositionLocal` exposing `KetoTheme.colors.xyz` anywhere.
- `KetoTracker(themeId) { … }` — root wrapper providing colors + a matching Material 3
  scheme. `MainActivity` resolves auto-theme (via `isSystemInDarkTheme()`) first.

### Components — `ui/components/`
`Common.kt` (`KetoCard`, `StepHeading`, `Dots`, `KText`, `ketoBorder`), `Header.kt`
(`HeaderBar`), `Buttons.kt` (`PrimaryButton`, `BackButton`, `SkipButton`, `KetoButton`),
`StepBodies.kt` (`MealBody`, `RatingsBody`, `HeartBody`, `FlagsBody`, `KetoTextArea`),
`Summary.kt` (`SummaryCard` + `PhotoIndicator`), `CalendarPanel.kt` (month grid +
`CalMonthYearPicker`), `PhotoComponents.kt` (`MealPhotoArea`, `PhotoViewer`,
`PhotoIndicator`), `ThemePanel.kt` (bottom-sheet theme picker + auto toggle).

### Screens — `ui/screens/`
`WizardScreen.kt` (wizard + header + overlay routing + swipe gestures + `BackHandler`),
`Sheets.kt` (`OverviewSheet`, `SupplementsSheet`, `QuickSelectSheet`), `SettingsSheet.kt`
(about, theme shortcut, export/import, storage stats, snapshots placeholder),
`Previews.kt` (`@Preview` gallery).

### Interaction details
- **Recomposition isolation**: `key(vm.stepIndex, vm.viewedKey)` wraps `StepContent`.
- **Gestures**: horizontal drags accumulate as `totalDrag`, evaluated on `onDragEnd`
  (>50 dp triggers step/day navigation).
- **System back**: `BackHandler` walks back through UI state (photo viewer → overlay →
  today → one wizard step) before falling through to the system default.

---

## Calendar — `ui/components/CalendarPanel.kt`

Bottom-anchored overlay opened from the header date chip. `DateUtils.monthGrid(year,
month)` returns a 42-cell (6×7, Sunday-first) layout. Colour priority (highest wins),
evaluated against in-memory `vm.allEntries`: **blue** = `tested && !notInKeto`, **green** =
≥2 keto meals, **gold** = any entry, none = nothing logged. Gold ring = today; white ring =
viewed day (viewed wins). `CalMonthYearPicker` provides two snap-scrolled wheels for fast
travel. `vm.jumpTo()` falls back to a blank `DayEntry`, so the calendar doubles as a
"jump to any date" picker.

---

## Export / Import — `data/io/DataPortability.kt`

Pure JSON encode/decode/merge lives in `DataPortability`; `SettingsSheet` owns the Storage
Access Framework pickers (`CreateDocument`/`OpenDocument`, JSON) and the confirm dialog.
Format is a flat, pretty-printed `{ "YYYY-MM-DD": { ...DayEntry... } }` map.
`AppViewModel.importFrom` parses off-thread and stores a `PendingImport(newCount, dupCount)`
summary; `confirmImport(mode)` applies merge (fill empty fields only), overwrite, or skip
for duplicates (new days always written) and persists via `IDayRepository.saveAll()`.
Dialogs are custom `Dialog` composables — the codebase uses **no Material3 `AlertDialog`**.

---

## Background Work, Notifications & Storage Stats

- **`work/BackupWorker.kt`** — WorkManager periodic JSON backup to
  `getExternalFilesDir("backups")` (internal fallback), keeping the last 7 files.
- **`work/ReminderWorker.kt`** — WorkManager daily reminder notification.
- **`data/notifications/NotificationHelper.kt`** — notification channel + builder (gold
  tint via `R.color.keto_gold`).
- **`data/io/StorageStats.kt`** — `StorageUsage.compute()` sizes the Room DB file
  (`getDatabasePath(KETO_DB_NAME)`) + sums photo JPEGs, against a 512 MB display ceiling.
- **Permissions**: only `POST_NOTIFICATIONS` (Android 13+). No camera or storage permission.

---

## Versioning

Two places must stay in sync on meaningful changes:
1. **`APP_VERSION`** in `ui/screens/SettingsSheet.kt` — shown in Settings.
2. **`versionName` / `versionCode`** in `app/build.gradle.kts` — installable build identity
   (bump `versionCode` for any release a device must treat as an update).

---

## Common Patterns & Conventions

- **Add a data-model field**: add to `DayEntry` *with a default*, add to
  `DayEntrySurrogate`, then surface in the relevant `StepBodies` composable, `SummaryCard`,
  and `DataPortability.merge`. No Room migration needed.
- **Add a wizard step**: add a `Step` enum entry, handle it in `StepContent()`
  (`WizardScreen`), update smart-start/first-incomplete logic, add it to `SummaryCard`.
- **Add a theme**: add a `KetoColors` entry to `KETO_THEMES` and a `ThemeInfo` to
  `THEME_LIST`.

---

## Known Constraints

- **Local-only**: all data is on the device; use Export/Import (and periodic backup) to move it.
- **Photos are device-local files**: sized into storage stats, **not** in JSON exports.
- **No `AlertDialog`**: custom `Dialog` composables styled like `KetoCard`/`ThemePanel`.
- **Snapshots** (named in-app backups) are **partially built** — disabled placeholder in
  Settings; scaffolding (`Snapshot.kt`, `SnapshotStore.kt`) exists.
- **Domain is still keto**: the skin-symptom rework has not happened yet — see
  "Current state" above before changing the data model.
- **Min SDK 26**, target/compile SDK 35; Kotlin 2.0, Compose BOM, KSP for Room.
</content>
