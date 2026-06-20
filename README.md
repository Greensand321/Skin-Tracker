# Skin Tracker — Native Android

A native **Android** app for logging skin-condition symptoms day-to-day, built in
**Kotlin + Jetpack Compose**. It runs fully offline, stores all data locally on the
device (no account, no server, no sync), and is built around one principle:

> **Record everything with the fewest clicks possible.**

Skin Tracker is a derivative of the **Keto Tracker** native Android app and shares its
architecture and friction-free logging workflow. It lives in its own self-contained
module (package `com.skintracker`, app id `com.skintracker`) so it can evolve — and
eventually move into its own repository — independently.

> ### ⚠️ Current state — mirror + rebrand
> This is a faithful **mirror + rebrand** of Keto Tracker. The application *identity*
> (package, app name, launcher label, theme style) is fully rebranded to Skin Tracker,
> but the underlying **domain model and wizard are still the keto data model**
> (meals, ketone "tested" flag, per-meal keto stamps, energy/mood/portion ratings,
> heart health). Repurposing the tracked fields to skin symptoms (e.g. itch, redness,
> flare severity) while keeping meal tracking is the planned next step.
>
> As part of keeping the copy low-risk and building as-is, several **internal code
> symbols were intentionally left with their original "Keto" names** — they are not
> user-visible and renaming them would add churn and risk. See "Naming notes" below.

---

## Ethos — record everything with the fewest clicks possible

A tracker only works if you actually use it, and friction is what kills the habit. Every
design choice removes taps:

- **Opens where you already are** — the wizard starts on the time-of-day-relevant step
  and skips to the first incomplete field.
- **Ratings advance themselves** — tap a 1–5 rating and the app moves on automatically.
- **One-tap common answers** — quick-select and supplement chips fill a field instantly.
- **Nothing needs saving** — every change is persisted immediately; there is no save button.
- **Skipping is first-class** — any field can be left blank; a partial day is still a logged day.
- **Inline editing** — the summary has an edit button on every field.
- **Swipe to move** — swipe to change steps, or days on the summary.

---

## Features

(Inherited from Keto Tracker — the domain is still keto until the skin-symptom rework.)

- **7-step daily wizard** — three meals (+ optional photo, + "Keto" stamp), daily ratings
  (energy / mood / portion), heart health, flags & notes, then a summary with inline edit.
- **Time-aware smart start** — opens at the relevant step and jumps to the first gap.
- **Meal photos** — capture via the system camera (no `CAMERA` permission), auto-rotated
  (EXIF), downscaled to ≤900 px, JPEG-compressed on-device, up to 5 per meal.
- **Calendar** — colour-coded month grid + month/year wheel picker; jump to any date.
- **Overview** — full list of logged days; tap to jump.
- **14 themes** (8 dark + 6 light) + **auto-theme** that follows the system dark/light setting.
- **Local-first storage** — Room database + on-disk photos; instant auto-save; offline by default.
- **Export / Import** — single `.json` file via the system file picker, with
  merge / overwrite / skip resolution for duplicate days.
- **Periodic backup + reminder notification** (WorkManager) and **storage usage stats**.

---

## Build & run

Requires **Android Studio** (Ladybug or newer) or the Android command-line SDK. This
folder (`android-skin/`) is a complete, self-contained Gradle project.

### Android Studio (recommended)
1. `File → Open` and select this `android-skin/` folder.
2. Let Gradle sync (first sync downloads AGP / Compose / Room / KSP / DataStore /
   serialization and may take a while).
3. Pick an emulator or device and hit **Run ▶**.

### Command line
```bash
# from this android-skin/ folder
echo "sdk.dir=/path/to/Android/sdk" > local.properties
./gradlew assembleDebug      # build debug APK -> app/build/outputs/apk/debug/
./gradlew installDebug       # build + install on a connected device
./gradlew testDebugUnitTest  # run the JVM unit tests (data layer)
```

### Design-time previews (no device needed)
Open `app/src/main/java/com/skintracker/ui/screens/Previews.kt` in Split/Design view —
previews cover every wizard step, overlay, and a sample of themes, backed by in-memory
demo data.

---

## Naming notes (mirror + rebrand artifacts)

| Aspect | Value |
|---|---|
| Kotlin package | `com.skintracker` |
| `applicationId` / `namespace` | `com.skintracker` |
| Gradle root project | `SkinTracker` |
| App name / launcher label | **Skin Tracker** |
| App theme style | `Theme.SkinTracker` |
| Root composable | `KetoTracker()` *(internal name kept)* |
| Theme types | `KetoTheme`, `KetoColors`, `KETO_THEMES` *(internal names kept)* |
| Room database class / file | `KetoDatabase` / `keto_tracker.db` *(internal names kept)* |
| Color resources | `keto_bg`, `keto_gold` *(internal names kept)* |
| Domain fields | `breakfastKeto`, `lunchKeto`, `dinnerKeto`, `notInKeto`, `tested`, … *(keto data model, intentionally unchanged)* |

The internal "Keto" symbols and the `keto_*` resource/DB names are sandboxed under the
`com.skintracker` application id, so they never collide with the Keto Tracker app — both
can be installed side-by-side. They'll be renamed if/when the domain is reworked to skin
symptoms.

See [`CLAUDE.md`](CLAUDE.md) in this folder for the full architecture and codebase guide.
</content>
