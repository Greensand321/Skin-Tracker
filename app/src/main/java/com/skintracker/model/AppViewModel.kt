package com.skintracker.model

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import com.skintracker.data.DateUtils
import com.skintracker.data.DayEntry
import com.skintracker.data.Flare
import com.skintracker.data.Meal
import com.skintracker.data.SymptomSnapshot
import com.skintracker.data.SnapshotMeta
import com.skintracker.data.Step
import com.skintracker.data.db.KetoDatabase
import com.skintracker.data.io.DataPortability
import com.skintracker.data.io.SnapshotStore
import com.skintracker.data.io.StorageStats
import com.skintracker.data.io.StorageUsage
import com.skintracker.data.io.ZipPortability
import com.skintracker.data.notifications.NotificationHelper
import com.skintracker.data.photo.MAX_MEAL_PHOTOS
import com.skintracker.data.photo.MealPhoto
import com.skintracker.data.photo.PhotoSaveResult
import com.skintracker.data.photo.PhotoStore
import com.skintracker.data.prefs.PrefsStore
import com.skintracker.data.repository.DayRepository
import com.skintracker.data.repository.FakeDayRepository
import com.skintracker.data.repository.IDayRepository
import com.skintracker.work.BackupWorker
import com.skintracker.work.ReminderWorker
import java.io.File
import java.time.LocalTime

class AppViewModel(
    private val repo: IDayRepository,
    private val prefs: PrefsStore?,
    private val photoStore: PhotoStore? = null,
    private val snapshotStore: SnapshotStore? = null,
) : ViewModel() {

    var themeId by mutableStateOf("midnight")
        private set

    // Auto-theme — native counterpart to the web app's kt_theme_auto/
    // kt_theme_dark_auto/kt_theme_light_auto (CLAUDE.md "Theme System"). When
    // [autoThemeEnabled], the UI resolves the active theme from these two IDs
    // based on the system's dark/light setting (see `resolveAutoTheme`)
    // instead of using [themeId] directly.
    var autoThemeEnabled by mutableStateOf(false)
        private set
    var darkAutoThemeId by mutableStateOf("midnight")
        private set
    var lightAutoThemeId by mutableStateOf("pearl")
        private set

    var viewedKey by mutableStateOf(DateUtils.todayKey())
        private set

    var stepIndex by mutableStateOf(0)
        private set

    var entry by mutableStateOf(DayEntry(date = DateUtils.todayKey()))
        private set

    // All logged entries kept in memory for the overview/history screens.
    var allEntries by mutableStateOf<Map<String, DayEntry>>(emptyMap())
        private set

    val step: Step get() = Step.entries[stepIndex]
    val isToday: Boolean get() = DateUtils.isToday(viewedKey)
    val isFuture: Boolean get() = DateUtils.isFuture(viewedKey)

    /**
     * One-shot, user-facing notifications — failures ("couldn't save your
     * entry") as well as plain confirmations ("Photo saved ✓"), mirroring the
     * web app's toast(message, isError). A Channel — not a state — so each
     * message is delivered exactly once and doesn't replay on
     * recomposition/rotation. The UI collects this as a Snackbar.
     */
    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages: Flow<String> = _messages.receiveAsFlow()

    // Set when the home-screen widget deep-links in to log a flare
    // (FlareWidgetProvider.ACTION_LOG_FLARE). The wizard observes this, opens
    // the flare sheet, then calls consumeOpenFlareRequest(). A plain idempotent
    // state flag is enough — re-opening an already-open sheet is a no-op.
    var openFlareRequested by mutableStateOf(false)
        private set

    // Bumped whenever photos for the active day change, so `mealPhotos()` —
    // a plain disk read with no Compose State of its own — re-triggers reads
    // on recomposition instead of going stale after add/remove.
    private var photoTick by mutableStateOf(0)

    // Set by `importFrom` once a file is parsed, so the UI can show a
    // merge/overwrite/skip choice (native counterpart of the web app's
    // chained confirm() dialogs — see CLAUDE.md "Import"). The raw decoded
    // entries stay private; the UI only ever sees the summary counts.
    var pendingImport by mutableStateOf<PendingImport?>(null)
        private set
    private var pendingNewEntries: Map<String, DayEntry> = emptyMap()
    private var pendingDupEntries: Map<String, DayEntry> = emptyMap()
    // Non-empty when the import came from a ZIP file — photos are restored after entries are committed.
    private var pendingZipPhotos: Map<String, ByteArray> = emptyMap()

    // Populated on demand by `loadStorageStats` — native counterpart of the
    // web app's `getStorageStats()` (see CLAUDE.md "Data Access"). Sizing the
    // database file and walking the photo directory does real disk I/O, so we
    // compute it lazily when Settings opens rather than keeping it live.
    var storageStats by mutableStateOf<StorageStats?>(null)
        private set

    // ── Snapshots ────────────────────────────────────────────────────────────
    var snapshots by mutableStateOf<List<SnapshotMeta>>(emptyList())
        private set

    // ── Quick-select ─────────────────────────────────────────────────────────
    var quickSelectItems by mutableStateOf(DEFAULT_QUICK_FOODS)
        private set

    // ── Periodic backup ───────────────────────────────────────────────────────
    var backupEnabled by mutableStateOf(false)
        private set
    var backupFrequency by mutableStateOf("daily")
        private set

    // ── Notifications ─────────────────────────────────────────────────────────
    var notificationsEnabled by mutableStateOf(false)
        private set
    var notificationHour by mutableStateOf(20)
        private set

    init {
        // Observe the persisted theme preferences.
        if (prefs != null) {
            viewModelScope.launch { prefs.theme.collect { id -> themeId = id } }
            viewModelScope.launch { prefs.autoThemeEnabled.collect { on -> autoThemeEnabled = on } }
            viewModelScope.launch { prefs.darkAutoTheme.collect { id -> darkAutoThemeId = id } }
            viewModelScope.launch { prefs.lightAutoTheme.collect { id -> lightAutoThemeId = id } }
            viewModelScope.launch { prefs.snapshots.collect { snaps -> snapshots = snaps } }
            viewModelScope.launch {
                prefs.quickSelectItems.collect { items ->
                    quickSelectItems = items ?: DEFAULT_QUICK_FOODS
                }
            }
            viewModelScope.launch { prefs.backupEnabled.collect { on -> backupEnabled = on } }
            viewModelScope.launch { prefs.backupFrequency.collect { freq -> backupFrequency = freq } }
            viewModelScope.launch { prefs.notificationsEnabled.collect { on -> notificationsEnabled = on } }
            viewModelScope.launch { prefs.notificationHour.collect { h -> notificationHour = h } }
        }

        // Load the full log ONCE. allEntries is then a plain in-memory cache
        // that `update()` keeps in sync directly — we deliberately do NOT
        // reactively re-query+re-decode the whole table on every write (that
        // would mean every keystroke triggers an O(total days logged) reload).
        viewModelScope.launch {
            val all = repo.loadAll().associateBy { it.date }
            allEntries = all
            val today = DateUtils.todayKey()
            val todayEntry = loadDateEntry(today)
            entry = todayEntry
            stepIndex = defStep(todayEntry)
        }
    }

    // ── Companion: factories ─────────────────────────────────────────────────

    companion object {
        /** Default quick-select food chips — mirrors QUICK_FOODS in Sheets.kt. */
        val DEFAULT_QUICK_FOODS = listOf(
            "Eggs", "Bacon", "Chicken", "Steak", "Salmon", "Avocado", "Cheddar", "HM Mayo",
            "Sourdough", "Broccoli", "Cauliflower", "Almonds", "Coffee", "Butter", "Cream", "Olive Oil",
        )

        /** Matches the web app's `maxlength="40"` on the quick-select add input. */
        private const val MAX_QUICK_SELECT_ITEM_LENGTH = 40

        /** Production factory — wires Room + DataStore. */
        fun factory(app: Application): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val db = KetoDatabase.get(app)
                val repo = DayRepository(db.dayEntryDao())
                val prefs = PrefsStore(app)
                val photoStore = PhotoStore(app)
                val snapshotStore = SnapshotStore(app)
                AppViewModel(repo, prefs, photoStore, snapshotStore)
            }
        }

        /** Preview factory — in-memory repo, no DataStore, seeded with demo data. */
        fun preview(): AppViewModel {
            val repo = FakeDayRepository()
            val vm = AppViewModel(repo, null)
            // Populate state synchronously so Compose Previews have data to render.
            val today = DateUtils.todayKey()
            vm.allEntries = repo.allSync()
            vm.entry = repo.loadSync(today)
            vm.stepIndex = vm.defStep(vm.entry)
            return vm
        }
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    fun loggedKeys(): List<String> = allEntries.keys.sortedDescending()
    fun hasEntry(key: String): Boolean = allEntries.containsKey(key)
    fun entryFor(key: String): DayEntry = allEntries[key] ?: DayEntry(date = key)

    // ── Theme ────────────────────────────────────────────────────────────────

    fun setTheme(id: String) {
        themeId = id
        if (prefs != null) {
            viewModelScope.launch {
                runCatching { prefs.setTheme(id) }
                    .onFailure { reportError("Couldn't save theme choice", it) }
            }
        }
    }

    /** Toggles auto-theme mode — mirrors the web app's `toggleAutoTheme()`. */
    fun toggleAutoTheme() {
        val enabled = !autoThemeEnabled
        autoThemeEnabled = enabled
        if (prefs != null) {
            viewModelScope.launch {
                runCatching { prefs.setAutoThemeEnabled(enabled) }
                    .onFailure { reportError("Couldn't save theme choice", it) }
            }
        }
    }

    /** Sets the night/day theme auto-theme switches between (CLAUDE.md "Theme System"). */
    fun setAutoThemeChoice(forDark: Boolean, id: String) {
        if (forDark) darkAutoThemeId = id else lightAutoThemeId = id
        if (prefs != null) {
            viewModelScope.launch {
                runCatching { if (forDark) prefs.setDarkAutoTheme(id) else prefs.setLightAutoTheme(id) }
                    .onFailure { reportError("Couldn't save theme choice", it) }
            }
        }
    }

    // ── Day navigation ────────────────────────────────────────────────────────

    fun goToday() = jumpTo(DateUtils.todayKey())

    fun changeDay(delta: Long) = jumpTo(DateUtils.offKey(viewedKey, delta))

    /**
     * allEntries is fully loaded at startup and kept in sync locally by
     * `update()`, so every date's data is already in memory — no DB read needed.
     */
    fun jumpTo(key: String) {
        if (DateUtils.isFuture(key)) return
        viewedKey = key
        entry = loadDateEntry(key)
        stepIndex = defStep(entry)
    }

    // ── Step navigation ───────────────────────────────────────────────────────

    fun next() { if (stepIndex < Step.entries.lastIndex) stepIndex++ }
    fun back() { if (stepIndex > 0) stepIndex-- }
    fun skip() = next()
    fun editAt(index: Int) { stepIndex = index.coerceIn(0, Step.entries.lastIndex) }

    // ── Field updates ─────────────────────────────────────────────────────────

    /**
     * Mutates the active entry, updates the in-memory cache immediately (so the
     * UI and `allEntries` never disagree), then persists asynchronously. We
     * update `allEntries` here — directly, from the value we already have —
     * rather than re-reading the whole table after every save.
     */
    private fun update(transform: (DayEntry) -> DayEntry) {
        val updated = transform(entry)
        entry = updated
        allEntries = allEntries + (updated.date to updated)
        viewModelScope.launch {
            runCatching { repo.save(updated) }
                .onFailure { reportError("Couldn't save your entry", it) }
        }
    }

    private fun notify(message: String) {
        _messages.trySend(message)
    }

    private fun reportError(message: String, cause: Throwable) {
        notify("$message — ${cause.message ?: "unknown error"}")
    }

    private fun nowHm(): String = LocalTime.now().let { "%02d:%02d".format(it.hour, it.minute) }

    /**
     * Applies a change to [meal]'s data and, the *first* time the meal gains any
     * content on today's entry, stamps it with the current time (for the
     * "X hours after a meal" timeline). Editing a past day never stamps "now" —
     * that would record tonight's clock time as when a historical meal happened.
     */
    private fun mutateMeal(meal: Meal, transform: (DayEntry) -> DayEntry) = update { e0 ->
        val e = transform(e0)
        if (isToday && e.mealTime(meal) == null && !e.mealEmpty(meal)) {
            e.withMealTime(meal, nowHm())
        } else {
            e
        }
    }

    fun setMealText(meal: Meal, value: String) = mutateMeal(meal) { it.withMealText(meal, value) }

    fun setMealSymptom(meal: Meal, field: SymptomField, value: Int) = mutateMeal(meal) {
        val s = it.mealSymptoms(meal)
        val updated = when (field) {
            SymptomField.ITCH -> s.copy(itch = value)
            SymptomField.REDNESS -> s.copy(redness = value)
            SymptomField.BUMPS -> s.copy(bumps = value)
        }
        it.withMealSymptoms(meal, updated)
    }

    fun setMealTouch(meal: Meal, value: String) = mutateMeal(meal) {
        it.withMealSymptoms(meal, it.mealSymptoms(meal).copy(touch = value))
    }

    /** Sets [zone]'s swelling severity (1–3) for [meal]; a severity ≤ 0 clears it. */
    fun setMealSwelling(meal: Meal, zone: String, severity: Int) = mutateMeal(meal) {
        val m = it.mealSymptoms(meal).swelling.toMutableMap()
        if (severity <= 0) m.remove(zone) else m[zone] = severity.coerceIn(1, 3)
        it.withMealSymptoms(meal, it.mealSymptoms(meal).copy(swelling = m))
    }

    /** Appends a standalone flare-up (Workflow B) to the day being viewed. */
    fun addFlare(symptoms: SymptomSnapshot) = update {
        it.copy(flares = it.flares + Flare(time = nowHm(), symptoms = symptoms))
    }

    /**
     * Entry point for the home-screen widget. A flare is always logged "now",
     * so we snap to today first, then raise [openFlareRequested] for the UI to
     * open the flare sheet.
     */
    fun requestOpenFlare() {
        if (!isToday) goToday()
        openFlareRequested = true
    }

    fun consumeOpenFlareRequest() { openFlareRequested = false }

    // ── Photos ────────────────────────────────────────────────────────────────
    // Native counterpart to the web app's getMealPhotos/addMealPhoto/
    // removeMealPhotoAt (see CLAUDE.md "IndexedDB (photo store)"). Files live
    // outside the JSON-column day entry entirely, so reads/writes go straight
    // to PhotoStore — `photoTick` is the recomposition signal that keeps the
    // UI in sync after an add/remove.

    /** Photos for [meal] on the day currently being viewed, oldest first. */
    fun mealPhotos(meal: Meal): List<MealPhoto> {
        photoTick
        return photoStore?.listPhotos(viewedKey, meal.field) ?: emptyList()
    }

    /**
     * Compresses and stores the freshly-captured photo [file] for [meal] on
     * the day being viewed *right now* — captured before the async hop so a
     * same-second day change can't misfile the photo under the wrong date.
     * [PhotoStore.addFromCapture] always deletes [file] when it's done with it.
     */
    fun addPhoto(meal: Meal, file: File) {
        val store = photoStore ?: return
        val date = viewedKey
        viewModelScope.launch {
            when (store.addFromCapture(date, meal.field, file)) {
                PhotoSaveResult.SAVED -> { photoTick++; notify("Photo saved ✓") }
                PhotoSaveResult.LIMIT_REACHED -> notify("Max $MAX_MEAL_PHOTOS photos per meal")
                PhotoSaveResult.FAILED -> notify("Could not save photo")
            }
        }
    }

    fun removePhoto(photo: MealPhoto) {
        val store = photoStore ?: return
        viewModelScope.launch {
            store.delete(photo)
            photoTick++
            notify("Photo removed")
        }
    }

    // ── Export / Import ───────────────────────────────────────────────────────
    // Native counterpart to the web app's exportAll/handleImport (CLAUDE.md
    // "Export / Import"). The UI hands us a SAF Uri obtained via
    // CreateDocument/OpenDocument; we own the JSON encode/decode and the
    // merge/overwrite/skip resolution, surfacing only a summary for the UI to
    // confirm via `pendingImport`.

    fun deleteDay(key: String) {
        allEntries = allEntries - key
        if (viewedKey == key) goToday()
        viewModelScope.launch {
            runCatching { repo.delete(key) }
                .onFailure { reportError("Couldn't delete entry", it) }
        }
    }

    fun exportAll(context: Context, uri: Uri) {
        val snapshot = allEntries
        viewModelScope.launch {
            val text = DataPortability.encode(snapshot)
            val ok = DataPortability.write(context, uri, text)
            if (ok) notify("Exported ${snapshot.size} day${if (snapshot.size != 1) "s" else ""} ✓")
            else notify("Export failed")
        }
    }

    fun importFrom(context: Context, uri: Uri) {
        viewModelScope.launch {
            val text = DataPortability.read(context, uri)
            if (text == null) { notify("Could not read file"); return@launch }
            val decoded = DataPortability.decode(text)
            if (decoded.isEmpty()) { notify("No valid entries found in file"); return@launch }
            val newEntries = decoded.filterKeys { it !in allEntries }
            val dupEntries = decoded.filterKeys { it in allEntries }
            pendingNewEntries = newEntries
            pendingDupEntries = dupEntries
            pendingImport = PendingImport(newCount = newEntries.size, dupCount = dupEntries.size)
        }
    }

    // ── Full ZIP backup ───────────────────────────────────────────────────────

    fun exportFullBackup(context: Context, uri: Uri) {
        val store = photoStore ?: return
        val snapshot = allEntries
        viewModelScope.launch {
            val ok = ZipPortability.export(context, uri, snapshot, store)
            if (ok) notify("Full backup exported — ${snapshot.size} day(s) + photos ✓")
            else notify("Export failed")
        }
    }

    fun importFromZip(context: Context, uri: Uri) {
        viewModelScope.launch {
            val result = ZipPortability.import(context, uri)
            if (result == null) { notify("Could not read backup file"); return@launch }
            if (result.entries.isEmpty()) { notify("No valid entries found in backup"); return@launch }
            pendingZipPhotos = result.photos
            val newEntries = result.entries.filterKeys { it !in allEntries }
            val dupEntries = result.entries.filterKeys { it in allEntries }
            pendingNewEntries = newEntries
            pendingDupEntries = dupEntries
            pendingImport = PendingImport(newCount = newEntries.size, dupCount = dupEntries.size)
        }
    }

    /**
     * Resolves the pending import with the chosen [mode] for duplicate days
     * (new days are always written, mirroring the web app), bulk-persists the
     * result, and refreshes in-memory state — including the active `entry` if
     * the day being viewed was among those just written.
     */
    fun confirmImport(mode: ImportMode) {
        val newEntries = pendingNewEntries
        val dupEntries = pendingDupEntries
        val zipPhotos = pendingZipPhotos
        pendingImport = null
        pendingNewEntries = emptyMap()
        pendingDupEntries = emptyMap()
        pendingZipPhotos = emptyMap()

        viewModelScope.launch {
            val toWrite = LinkedHashMap<String, DayEntry>(newEntries)
            when (mode) {
                ImportMode.MERGE -> dupEntries.forEach { (key, imported) ->
                    toWrite[key] = DataPortability.merge(allEntries[key] ?: DayEntry(date = key), imported)
                }
                ImportMode.OVERWRITE -> toWrite += dupEntries
                ImportMode.SKIP -> Unit
            }
            if (toWrite.isEmpty()) { notify("No days imported"); return@launch }

            runCatching { repo.saveAll(toWrite.values.toList()) }
                .onSuccess {
                    allEntries = allEntries + toWrite
                    toWrite[viewedKey]?.let { entry = it; stepIndex = defStep(it) }
                    val n = toWrite.size
                    val note = when {
                        dupEntries.isEmpty() -> ""
                        mode == ImportMode.MERGE -> " (merged)"
                        mode == ImportMode.OVERWRITE -> " (overwritten)"
                        else -> " · ${dupEntries.size} duplicate${if (dupEntries.size != 1) "s" else ""} skipped"
                    }
                    notify("Imported $n day${if (n != 1) "s" else ""}$note ✓")

                    // Restore photos from ZIP import
                    if (zipPhotos.isNotEmpty() && photoStore != null) {
                        val restored = zipPhotos.count { (filename, bytes) ->
                            photoStore.restorePhoto(filename, bytes)
                        }
                        if (restored > 0) { photoTick++; notify("Restored $restored photo(s) ✓") }
                    }
                }
                .onFailure { reportError("Import failed", it) }
        }
    }

    fun cancelImport() {
        pendingImport = null
        pendingNewEntries = emptyMap()
        pendingDupEntries = emptyMap()
        pendingZipPhotos = emptyMap()
    }

    /**
     * Sizes the Room database file and the on-disk photo directory — native
     * counterpart of the web app's `getStorageStats()`. Triggered when the
     * Settings sheet opens; not kept continuously live since it touches disk.
     */
    fun loadStorageStats(context: Context) {
        val store = photoStore ?: return
        viewModelScope.launch {
            storageStats = StorageUsage.compute(context, store, allEntries.size)
        }
    }

    // ── Snapshots ─────────────────────────────────────────────────────────────
    // Named point-in-time backups (up to 25), mirroring the web app's snapshot
    // system (CLAUDE.md "Snapshot Schema"). Metadata is persisted in DataStore;
    // full entry data lives as separate files in filesDir/snapshots/.

    fun saveSnapshot(name: String) {
        val store = snapshotStore ?: return
        viewModelScope.launch {
            val entries = allEntries
            val id = System.currentTimeMillis()
            val saved = store.save(id, entries)
            if (!saved) { notify("Could not save snapshot"); return@launch }
            val label = name.trim().ifEmpty { "Snapshot" }
            val meta = SnapshotMeta(id = id, name = label, ts = id, days = entries.size)
            val updated = (snapshots + meta).takeLast(25)
            runCatching { prefs?.setSnapshots(updated) }
            snapshots = updated
            notify("Snapshot \"$label\" saved — ${entries.size} day(s) ✓")
        }
    }

    /** Replaces all current day data with the snapshot. The UI must confirm before calling this. */
    fun restoreSnapshot(id: Long) {
        val store = snapshotStore ?: return
        viewModelScope.launch {
            val entries = store.load(id)
            if (entries == null) { notify("Snapshot data not found"); return@launch }
            runCatching { repo.deleteAll() }
                .onFailure { reportError("Could not clear existing data", it); return@launch }
            runCatching { repo.saveAll(entries.values.toList()) }
                .onSuccess {
                    allEntries = entries
                    // Navigate to today so defStep has the right viewedKey context.
                    val today = DateUtils.todayKey()
                    viewedKey = today
                    val todayEntry = loadDateEntry(today)
                    entry = todayEntry
                    stepIndex = defStep(todayEntry)
                    notify("Restored ${entries.size} day(s) ✓")
                }
                .onFailure { reportError("Restore failed", it) }
        }
    }

    fun deleteSnapshot(id: Long) {
        val store = snapshotStore ?: return
        viewModelScope.launch {
            store.delete(id)
            val updated = snapshots.filter { it.id != id }
            runCatching { prefs?.setSnapshots(updated) }
            snapshots = updated
            notify("Snapshot deleted")
        }
    }

    fun exportSnapshot(context: Context, uri: Uri, id: Long) {
        val store = snapshotStore ?: return
        viewModelScope.launch {
            val ok = store.writeTo(context, id, uri)
            if (ok) notify("Snapshot exported ✓") else notify("Export failed")
        }
    }

    // ── Quick-select ──────────────────────────────────────────────────────────

    /**
     * Adds [food] to the quick-select list, ignoring blank input and
     * duplicates of an existing item regardless of case/whitespace (so
     * "eggs" doesn't sit alongside "Eggs" as a near-identical chip).
     * Mirrors the web app's `addQsItemInline` (index.html ~L1549).
     */
    fun addQuickSelectItem(food: String) {
        val trimmed = food.trim().take(MAX_QUICK_SELECT_ITEM_LENGTH)
        if (trimmed.isEmpty()) return
        if (quickSelectItems.any { it.equals(trimmed, ignoreCase = true) }) return
        val updated = quickSelectItems + trimmed
        quickSelectItems = updated
        persistQuickSelectItems(updated)
    }

    fun removeQuickSelectItem(food: String) {
        val updated = quickSelectItems - food
        quickSelectItems = updated
        persistQuickSelectItems(updated)
    }

    fun resetQuickSelectDefaults() {
        quickSelectItems = DEFAULT_QUICK_FOODS
        persistQuickSelectItems(DEFAULT_QUICK_FOODS)
    }

    /**
     * Persists [items] and reports failures back to the user — a save that
     * silently fails would otherwise leave the in-memory list (with the new
     * item visible everywhere this session) out of sync with disk, so the
     * edit quietly reverts the next time the app starts.
     */
    private fun persistQuickSelectItems(items: List<String>) {
        viewModelScope.launch {
            runCatching { prefs?.setQuickSelectItems(items) }
                .onFailure { reportError("Couldn't save quick-select items", it) }
        }
    }

    // ── Periodic backup ───────────────────────────────────────────────────────

    fun setBackupEnabled(context: Context, enabled: Boolean) {
        backupEnabled = enabled
        viewModelScope.launch {
            runCatching { prefs?.setBackupEnabled(enabled) }
            if (enabled) BackupWorker.schedule(context, if (backupFrequency == "weekly") 7L else 1L)
            else BackupWorker.cancel(context)
        }
    }

    fun setBackupFrequency(context: Context, freq: String) {
        backupFrequency = freq
        viewModelScope.launch {
            runCatching { prefs?.setBackupFrequency(freq) }
            if (backupEnabled) BackupWorker.schedule(context, if (freq == "weekly") 7L else 1L)
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    /**
     * Enables or disables the daily reminder. When enabling, creates the
     * notification channel (safe to call repeatedly — Android no-ops it) and
     * schedules the WorkManager reminder. Caller must have already obtained
     * POST_NOTIFICATIONS permission on Android 13+ before calling with true.
     */
    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        notificationsEnabled = enabled
        viewModelScope.launch {
            runCatching { prefs?.setNotificationsEnabled(enabled) }
            if (enabled) {
                NotificationHelper.createChannel(context)
                ReminderWorker.schedule(context, notificationHour)
            } else {
                ReminderWorker.cancel(context)
            }
        }
    }

    /** Changes the reminder hour, rescheduling the worker if notifications are on. */
    fun setNotificationHour(context: Context, hour: Int) {
        notificationHour = hour
        viewModelScope.launch {
            runCatching { prefs?.setNotificationHour(hour) }
            if (notificationsEnabled) ReminderWorker.schedule(context, hour)
        }
    }

    // ── Smart step logic ──────────────────────────────────────────────────────

    private fun smartStep(): Step {
        val h = LocalTime.now().hour
        return when {
            h < 10 -> Step.BREAKFAST
            h < 14 -> Step.LUNCH
            else -> Step.DINNER
        }
    }

    internal fun isIncomplete(s: Step, e: DayEntry): Boolean = when (s) {
        Step.BREAKFAST -> e.mealEmpty(Meal.BREAKFAST)
        Step.LUNCH -> e.mealEmpty(Meal.LUNCH)
        Step.DINNER -> e.mealEmpty(Meal.DINNER)
        else -> false
    }

    internal fun defStep(e: DayEntry): Int {
        if (!DateUtils.isToday(viewedKey)) return Step.SUMMARY.ordinal
        val mealSteps = listOf(Step.BREAKFAST, Step.LUNCH, Step.DINNER)
        val incomplete = mealSteps.filter { isIncomplete(it, e) }
        if (incomplete.isEmpty()) return Step.SUMMARY.ordinal

        val ss = smartStep()
        // Don't skip past an unfilled lunch just because it's dinner time — lunch
        // must be explicitly completed (or skipped by the user) before dinner.
        val effective = if (ss == Step.DINNER && isIncomplete(Step.LUNCH, e)) Step.LUNCH else ss

        if (effective in incomplete) return effective.ordinal
        for (i in 0..effective.ordinal) {
            if (Step.entries[i] in incomplete) return i
        }
        return incomplete.first().ordinal
    }

    private fun loadDateEntry(key: String): DayEntry = allEntries[key] ?: DayEntry(date = key)
}

/** The three per-meal skin symptom inputs. */
enum class SymptomField { ITCH, REDNESS, BUMPS }

/** Summary counts shown by the import-confirmation dialog (CLAUDE.md "Import"). */
data class PendingImport(val newCount: Int, val dupCount: Int)

/** How to resolve duplicate days during import — mirrors the web app's three confirm() choices. */
enum class ImportMode { MERGE, OVERWRITE, SKIP }
