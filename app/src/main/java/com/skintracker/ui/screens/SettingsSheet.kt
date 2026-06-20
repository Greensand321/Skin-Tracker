@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.skintracker.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.skintracker.data.DateUtils
import com.skintracker.data.SnapshotMeta
import com.skintracker.data.io.StorageStats
import com.skintracker.model.AppViewModel
import com.skintracker.model.ImportMode
import com.skintracker.model.PendingImport
import com.skintracker.ui.components.KText
import com.skintracker.ui.theme.KetoTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val APP_VERSION = "1.0-native-demo"

private enum class SettingsPage {
    MAIN, DATA_STORAGE, QUICK_SELECT, NOTIFICATIONS, APPEARANCE, ABOUT
}

@Composable
fun SettingsSheet(vm: AppViewModel, onTheme: () -> Unit, onClose: () -> Unit) {
    val c = KetoTheme.colors
    val context = LocalContext.current
    var page by remember { mutableStateOf(SettingsPage.MAIN) }

    // All SAF launchers at the top level so they're always composed and can safely
    // receive results regardless of which sub-page is currently visible.
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportAll(context, uri) }

    val importJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importFrom(context, uri) }

    val exportZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri -> if (uri != null) vm.exportFullBackup(context, uri) }

    val importZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importFromZip(context, uri) }

    var pendingSnapshotExportId by remember { mutableStateOf<Long?>(null) }
    val exportSnapshotLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val id = pendingSnapshotExportId
        if (uri != null && id != null) vm.exportSnapshot(context, uri, id)
        pendingSnapshotExportId = null
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        // Directional slide: navigating into a sub-page slides in from the right
        // (like tapping a nav row in Android Settings); the back button slides
        // in from the left. Using targetState != MAIN is a reliable proxy for
        // direction since the only possible transitions are MAIN→sub and sub→MAIN.
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                if (targetState != SettingsPage.MAIN) {
                    (slideInHorizontally(tween(300)) { it } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(260)) { -it / 3 } + fadeOut(tween(220)))
                } else {
                    (slideInHorizontally(tween(300)) { -it } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(260)) { it / 3 } + fadeOut(tween(220)))
                }
            },
            label = "settings_nav",
        ) { currentPage ->
            when (currentPage) {
                SettingsPage.MAIN -> SettingsMainPage(
                    vm = vm,
                    onClose = onClose,
                    onNavigate = { page = it },
                )
                SettingsPage.DATA_STORAGE -> SettingsDataStoragePage(
                    vm = vm,
                    onBack = { page = SettingsPage.MAIN },
                    onExportJson = { exportJsonLauncher.launch("keto-all-data-${DateUtils.todayKey()}.json") },
                    onImportJson = { importJsonLauncher.launch(arrayOf("application/json")) },
                    onExportZip = { exportZipLauncher.launch("keto-backup-${DateUtils.todayKey()}.zip") },
                    onImportZip = { importZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "*/*")) },
                    onExportSnapshot = { id ->
                        pendingSnapshotExportId = id
                        exportSnapshotLauncher.launch("keto-snapshot-${DateUtils.todayKey()}.json")
                    },
                )
                SettingsPage.QUICK_SELECT -> SettingsQuickSelectPage(
                    vm = vm,
                    onBack = { page = SettingsPage.MAIN },
                )
                SettingsPage.NOTIFICATIONS -> SettingsNotificationsPage(
                    vm = vm,
                    onBack = { page = SettingsPage.MAIN },
                )
                SettingsPage.APPEARANCE -> SettingsAppearancePage(
                    vm = vm,
                    onTheme = onTheme,
                    onBack = { page = SettingsPage.MAIN },
                )
                SettingsPage.ABOUT -> SettingsAboutPage(
                    onBack = { page = SettingsPage.MAIN },
                )
            }
        }

        // Import confirmation dialog floats above all sub-pages
        vm.pendingImport?.let { pending ->
            ImportConfirmDialog(
                pending = pending,
                onConfirm = { vm.confirmImport(it) },
                onCancel = { vm.cancelImport() },
            )
        }
    }
}

// ── Main page ─────────────────────────────────────────────────────────────────

@Composable
private fun SettingsMainPage(
    vm: AppViewModel,
    onClose: () -> Unit,
    onNavigate: (SettingsPage) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        SettingsHeader(title = "⚙️ Settings", onAction = onClose, actionLabel = "✕")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NavRow(
                icon = "📁",
                label = "Data & Storage",
                sub = "${vm.loggedKeys().size} day(s) · ${vm.snapshots.size} snapshot(s)",
            ) { onNavigate(SettingsPage.DATA_STORAGE) }
            NavRow(
                icon = "⚡",
                label = "Quick-Select Foods",
                sub = "${vm.quickSelectItems.size} item(s)",
            ) { onNavigate(SettingsPage.QUICK_SELECT) }
            NavRow(
                icon = "🔔",
                label = "Notifications",
                sub = if (vm.notificationsEnabled) "On — ${hourLabel(vm.notificationHour)}" else "Off",
            ) { onNavigate(SettingsPage.NOTIFICATIONS) }
            NavRow(
                icon = "🎨",
                label = "Appearance",
                sub = if (vm.autoThemeEnabled) "Auto" else vm.themeId,
            ) { onNavigate(SettingsPage.APPEARANCE) }
            NavRow(
                icon = "ℹ️",
                label = "About",
                sub = "v$APP_VERSION",
            ) { onNavigate(SettingsPage.ABOUT) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Data & Storage page (merged) ──────────────────────────────────────────────

@Composable
private fun SettingsDataStoragePage(
    vm: AppViewModel,
    onBack: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    onExportZip: () -> Unit,
    onImportZip: () -> Unit,
    onExportSnapshot: (Long) -> Unit,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { vm.loadStorageStats(context) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var confirmDeleteSnap by remember { mutableStateOf<SnapshotMeta?>(null) }
    var confirmRestoreSnap by remember { mutableStateOf<SnapshotMeta?>(null) }

    Column(Modifier.fillMaxSize()) {
        SettingsSubHeader("📁 Data & Storage", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {

            // ── Storage bar at the top ────────────────────────────────────────
            SettingsSection("Storage") {
                StorageBar(vm.storageStats)
            }

            // ── JSON export / import ──────────────────────────────────────────
            SettingsSection("JSON Export / Import") {
                SettingsDivider("${vm.loggedKeys().size} day(s) logged")
                SettingsButton("📋 Export Data", subtitle = "Save all entries as a .json file") {
                    onExportJson()
                }
                SettingsButton("📥 Import Data", subtitle = "Merge entries from a .json file") {
                    onImportJson()
                }
            }

            // ── Full ZIP backup ───────────────────────────────────────────────
            SettingsSection("Full Backup") {
                InfoBanner("Includes all entries AND meal photos in a single .zip archive.")
                SettingsButton("📦 Export Full Backup", subtitle = "Save data + photos as a .zip") {
                    onExportZip()
                }
                SettingsButton("📥 Import from Backup", subtitle = "Restore data and photos from a .zip") {
                    onImportZip()
                }
            }

            // ── Snapshots ─────────────────────────────────────────────────────
            SettingsSection("Snapshots") {
                SettingsButton(
                    "💾 Save Snapshot",
                    subtitle = "Name and save a point-in-time backup (up to 25)",
                ) {
                    nameInput = ""; showSaveDialog = true
                }
                if (vm.snapshots.isEmpty()) {
                    SettingsDivider("No snapshots yet")
                } else {
                    vm.snapshots.sortedByDescending { it.ts }.forEach { snap ->
                        SnapshotRow(
                            snap = snap,
                            onRestore = { confirmRestoreSnap = snap },
                            onExport = { onExportSnapshot(snap.id) },
                            onDelete = { confirmDeleteSnap = snap },
                        )
                    }
                }
            }

            // ── Periodic backup ───────────────────────────────────────────────
            SettingsSection("Periodic Backup") {
                SettingsButton(
                    title = "🔄 Auto-Backup",
                    subtitle = if (vm.backupEnabled)
                        "On — ${vm.backupFrequency.replaceFirstChar { it.uppercase() }}"
                    else "Off — tap to enable",
                ) { vm.setBackupEnabled(context, !vm.backupEnabled) }

                if (vm.backupEnabled) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SelectChip("Daily", vm.backupFrequency == "daily", Modifier.weight(1f)) {
                            vm.setBackupFrequency(context, "daily")
                        }
                        SelectChip("Weekly", vm.backupFrequency == "weekly", Modifier.weight(1f)) {
                            vm.setBackupFrequency(context, "weekly")
                        }
                    }
                    InfoBanner("Backup files are saved to Skin Tracker/backups/ on your device. The last 7 files are kept.")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── Save Snapshot dialog ──────────────────────────────────────────────────
    if (showSaveDialog) {
        val c = KetoTheme.colors
        Dialog(onDismissRequest = { showSaveDialog = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.surf)
                    .border(1.dp, c.bdI, RoundedCornerShape(18.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KText("💾 Save Snapshot", size = 17, color = c.gold, weight = FontWeight.Bold)
                KText("Give this snapshot a name so you can find it later.", size = 14, color = c.txtM)
                SingleLineInput(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    placeholder = "e.g. Pre-holiday backup",
                )
                DialogOption("Save", "Snapshot ${vm.loggedKeys().size} day(s)") {
                    vm.saveSnapshot(nameInput)
                    showSaveDialog = false
                }
                DialogCancelButton { showSaveDialog = false }
            }
        }
    }

    // ── Restore confirmation dialog ───────────────────────────────────────────
    confirmRestoreSnap?.let { snap ->
        val c = KetoTheme.colors
        Dialog(onDismissRequest = { confirmRestoreSnap = null }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.surf)
                    .border(1.dp, c.bdI, RoundedCornerShape(18.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KText("⚠️ Restore Snapshot?", size = 17, color = c.gold, weight = FontWeight.Bold)
                KText(
                    "\"${snap.name}\" — ${snap.days} day(s)\nThis will replace ALL current data.",
                    size = 14, color = c.txt,
                )
                DialogOption("Restore", "Replace current data with this snapshot") {
                    vm.restoreSnapshot(snap.id)
                    confirmRestoreSnap = null
                }
                DialogCancelButton { confirmRestoreSnap = null }
            }
        }
    }

    // ── Delete confirmation dialog ────────────────────────────────────────────
    confirmDeleteSnap?.let { snap ->
        val c = KetoTheme.colors
        Dialog(onDismissRequest = { confirmDeleteSnap = null }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.surf)
                    .border(1.dp, c.bdI, RoundedCornerShape(18.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                KText("🗑️ Delete Snapshot?", size = 17, color = c.gold, weight = FontWeight.Bold)
                KText("\"${snap.name}\" — ${snap.days} day(s)", size = 14, color = c.txt)
                DialogOption("Delete", "This cannot be undone") {
                    vm.deleteSnapshot(snap.id)
                    confirmDeleteSnap = null
                }
                DialogCancelButton { confirmDeleteSnap = null }
            }
        }
    }
}

@Composable
private fun SnapshotRow(
    snap: SnapshotMeta,
    onRestore: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = KetoTheme.colors
    val date = remember(snap.ts) {
        Instant.ofEpochMilli(snap.ts)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.surf)
            .border(1.dp, c.bd, RoundedCornerShape(13.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                KText(snap.name, size = 15, color = c.txt, weight = FontWeight.SemiBold)
                KText("$date · ${snap.days} day(s)", size = 12, color = c.txtD)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallActionButton("Restore", c.accent) { onRestore() }
            SmallActionButton("Export", c.gold) { onExport() }
            SmallActionButton("Delete", c.red) { onDelete() }
        }
    }
}

// ── Quick-Select page ─────────────────────────────────────────────────────────

@Composable
private fun SettingsQuickSelectPage(vm: AppViewModel, onBack: () -> Unit) {
    val c = KetoTheme.colors
    var newItemInput by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        SettingsSubHeader("⚡ Quick-Select Foods", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            KText(
                "These chips appear in the Quick-Select panel when logging meals. Tap × to remove.",
                size = 13, color = c.txtM,
            )
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vm.quickSelectItems.forEach { food ->
                    RemovableChip(food) { vm.removeQuickSelectItem(food) }
                }
            }
            SettingsSection("Add Item") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.inp)
                            .border(1.dp, c.bd, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        if (newItemInput.isEmpty()) {
                            KText("New food item…", size = 15, color = c.txtD)
                        }
                        BasicTextField(
                            value = newItemInput,
                            onValueChange = { newItemInput = it },
                            singleLine = true,
                            textStyle = TextStyle(color = c.txt, fontSize = 15.sp),
                            cursorBrush = SolidColor(c.accent),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(c.accent)
                            .clickable {
                                if (newItemInput.isNotBlank()) {
                                    vm.addQuickSelectItem(newItemInput)
                                    newItemInput = ""
                                }
                            }
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        KText("Add", size = 15, color = Color.White, weight = FontWeight.SemiBold)
                    }
                }
            }
            SettingsButton(
                "↩ Restore Defaults",
                subtitle = "Reset to the original ${AppViewModel.DEFAULT_QUICK_FOODS.size} items",
            ) { vm.resetQuickSelectDefaults() }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Notifications page ────────────────────────────────────────────────────────

@Composable
private fun SettingsNotificationsPage(vm: AppViewModel, onBack: () -> Unit) {
    val c = KetoTheme.colors
    val context = LocalContext.current

    // Android 13+ requires explicit POST_NOTIFICATIONS permission at runtime.
    // On older versions the system grants it automatically, so we skip the request.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) vm.setNotificationsEnabled(context, true)
        // If denied, the toggle stays off — the user can re-enable from Android system settings.
    }

    fun requestEnableNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS); return }
        }
        vm.setNotificationsEnabled(context, true)
    }

    Column(Modifier.fillMaxSize()) {
        SettingsSubHeader("🔔 Notifications", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {

            // ── Enable toggle ─────────────────────────────────────────────────
            SettingsSection("Daily Reminder") {
                // Main on/off row with an inline toggle indicator
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(13.dp))
                        .background(c.surf)
                        .border(1.dp, c.bd, RoundedCornerShape(13.dp))
                        .clickable {
                            if (vm.notificationsEnabled) vm.setNotificationsEnabled(context, false)
                            else requestEnableNotifications()
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        KText("🔔 Daily Reminder", size = 15, color = c.txt, weight = FontWeight.SemiBold)
                        KText(
                            if (vm.notificationsEnabled) "On — reminds you to log your meals"
                            else "Off — tap to enable",
                            size = 12, color = c.txtD,
                        )
                    }
                    TogglePill(on = vm.notificationsEnabled)
                }

                // When a notification fires and the log is incomplete, the body is
                // context-aware — it changes based on which meals are missing.
                InfoBanner(
                    "If all three meals are already logged for the day, the reminder is automatically suppressed — no unnecessary pings.",
                )
            }

            // ── Time selector (only shown when enabled) ───────────────────────
            if (vm.notificationsEnabled) {
                SettingsSection("Reminder Time") {
                    KText(
                        "Choose when you'd like to be nudged each day.",
                        size = 13, color = c.txtM,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TimePreset(
                            icon = "🌅",
                            label = "Morning",
                            sub = "8:00 AM",
                            selected = vm.notificationHour == 8,
                        ) { vm.setNotificationHour(context, 8) }
                        TimePreset(
                            icon = "☀️",
                            label = "Afternoon",
                            sub = "2:00 PM",
                            selected = vm.notificationHour == 14,
                        ) { vm.setNotificationHour(context, 14) }
                        TimePreset(
                            icon = "🌙",
                            label = "Evening",
                            sub = "8:00 PM",
                            selected = vm.notificationHour == 20,
                        ) { vm.setNotificationHour(context, 20) }
                    }
                }

                // ── Notification preview card ─────────────────────────────────
                SettingsSection("Preview") {
                    NotificationPreviewCard(vm.notificationHour)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Visual mock of how the notification will appear in the notification shade. */
@Composable
private fun NotificationPreviewCard(hour: Int) {
    val c = KetoTheme.colors
    val previewBody = when {
        hour <= 10 -> "Almost there — just log tonight's dinner to wrap up the day 🍽️"
        hour <= 15 -> "Don't forget lunch! A quick entry keeps your log complete 🥗"
        else       -> "Open Skin Tracker to log today's meals and keep your streak going 💪"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surf2)
            .border(1.dp, c.bd, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Mock system notification header bar
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // App icon placeholder
                Box(
                    Modifier
                        .size(14.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(c.gold),
                )
                KText("KETO TRACKER", size = 10, color = c.txtM, weight = FontWeight.Bold, letterSpacing = 0.8f)
            }
            KText(hourLabel(hour), size = 10, color = c.txtD)
        }
        // Notification body
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            KText("Skin Tracker", size = 14, color = c.txt, weight = FontWeight.Bold)
            KText(previewBody, size = 13, color = c.txtM)
        }
        // Action button row
        Row {
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(c.gold.copy(alpha = 0.15f))
                    .border(1.dp, c.gold.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                KText("Log Now", size = 12, color = c.gold, weight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TimePreset(icon: String, label: String, sub: String, selected: Boolean, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) c.accent.copy(alpha = 0.12f) else c.surf)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) c.accent else c.bd,
                shape = RoundedCornerShape(13.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KText(icon, size = 20)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                KText(label, size = 15, color = if (selected) c.accent else c.txt, weight = FontWeight.SemiBold)
                KText(sub, size = 12, color = if (selected) c.accent.copy(alpha = 0.7f) else c.txtD)
            }
        }
        if (selected) {
            Box(
                Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(50))
                    .background(c.accent),
                contentAlignment = Alignment.Center,
            ) {
                KText("✓", size = 12, color = Color.White, weight = FontWeight.Bold)
            }
        }
    }
}

/** Sliding toggle pill — knob springs to position, track fades between colours. */
@Composable
private fun TogglePill(on: Boolean) {
    val c = KetoTheme.colors
    val trackColor by animateColorAsState(
        targetValue = if (on) c.accent else c.bdI,
        animationSpec = tween(220),
        label = "pill_track",
    )
    // 48dp track, 3dp padding each side, 20dp knob → travel = 48 - 6 - 20 = 22dp.
    val knobX by animateDpAsState(
        targetValue = if (on) 22.dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pill_knob",
    )
    Box(
        Modifier
            .size(width = 48.dp, height = 26.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(trackColor)
            .padding(3.dp),
    ) {
        Box(
            Modifier
                .offset(x = knobX)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

// ── Appearance page ───────────────────────────────────────────────────────────

@Composable
private fun SettingsAppearancePage(vm: AppViewModel, onTheme: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SettingsSubHeader("🎨 Appearance", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsSection("Theme") {
                SettingsButton(
                    "🎨 Choose Theme",
                    subtitle = if (vm.autoThemeEnabled) "Auto — follows system dark/light mode"
                    else "Currently: ${vm.themeId}",
                ) { onTheme() }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── About page ────────────────────────────────────────────────────────────────

@Composable
private fun SettingsAboutPage(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        SettingsSubHeader("ℹ️ About", onBack)
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            SettingsSection("About") {
                SettingsRow(label = "App", value = "Skin Tracker")
                SettingsRow(label = "Version", value = APP_VERSION)
                SettingsRow(label = "Platform", value = "Native Android (Compose)")
                InfoBanner("Data is stored locally on this device using Room. Your log survives app restarts and updates. Themes are persisted via DataStore.")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Import confirmation ───────────────────────────────────────────────────────

@Composable
private fun ImportConfirmDialog(
    pending: PendingImport,
    onConfirm: (ImportMode) -> Unit,
    onCancel: () -> Unit,
) {
    val c = KetoTheme.colors
    Dialog(onDismissRequest = onCancel) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(c.surf)
                .border(1.dp, c.bdI, RoundedCornerShape(18.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            KText("📥 Import data?", size = 17, color = c.gold, weight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (pending.newCount > 0)
                    KText("• ${pending.newCount} new day(s) will be added", size = 14, color = c.txt)
                if (pending.dupCount > 0)
                    KText("• ${pending.dupCount} day(s) already exist — choose how to handle them below", size = 14, color = c.txt)
            }
            if (pending.dupCount > 0) {
                DialogOption("Merge — fill empty fields only", "Existing values are kept; gaps are filled from the import") { onConfirm(ImportMode.MERGE) }
                DialogOption("Overwrite", "Imported data replaces the existing duplicate days") { onConfirm(ImportMode.OVERWRITE) }
                DialogOption("Skip duplicates", "Keep existing data; only add the new days") { onConfirm(ImportMode.SKIP) }
            } else {
                DialogOption("Import", "Add ${pending.newCount} day(s) to your log") { onConfirm(ImportMode.SKIP) }
            }
            DialogCancelButton { onCancel() }
        }
    }
}

@Composable
private fun DialogOption(title: String, subtitle: String, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.inp)
            .border(1.dp, c.bd, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        KText(title, size = 14, color = c.txt, weight = FontWeight.SemiBold)
        KText(subtitle, size = 12, color = c.txtM)
    }
}

@Composable
private fun DialogCancelButton(onCancel: () -> Unit) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clickable { onCancel() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        KText("Cancel", size = 14, color = c.txtM, weight = FontWeight.SemiBold)
    }
}

// ── Shared header components ──────────────────────────────────────────────────

@Composable
private fun SettingsHeader(title: String, onAction: () -> Unit, actionLabel: String) {
    val c = KetoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, c.bd, RoundedCornerShape(0.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        KText(title, size = 16, color = c.gold, weight = FontWeight.Bold)
        Box(Modifier.clickable { onAction() }.padding(4.dp)) {
            KText(actionLabel, size = 18, color = c.txtM)
        }
    }
}

@Composable
private fun SettingsSubHeader(title: String, onBack: () -> Unit) {
    val c = KetoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, c.bd, RoundedCornerShape(0.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.clickable { onBack() }.padding(end = 12.dp)) {
            KText("‹", size = 22, color = c.gold, weight = FontWeight.Bold)
        }
        KText(title, size = 16, color = c.gold, weight = FontWeight.Bold)
    }
}

// ── Reusable sub-components ───────────────────────────────────────────────────

@Composable
private fun NavRow(icon: String, label: String, sub: String?, onClick: () -> Unit) {
    val c = KetoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.surf)
            .border(1.dp, c.bd, RoundedCornerShape(13.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KText(icon, size = 20)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                KText(label, size = 15, color = c.txt, weight = FontWeight.SemiBold)
                if (sub != null) KText(sub, size = 12, color = c.txtD)
            }
        }
        KText("›", size = 18, color = c.txtM)
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    val c = KetoTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        KText(
            title.uppercase(),
            size = 11,
            color = c.txtM,
            weight = FontWeight.Bold,
            letterSpacing = 1.8f,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        content()
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    val c = KetoTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.surf)
            .border(1.dp, c.bd, RoundedCornerShape(13.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KText(label, size = 15, color = c.txtM)
        KText(value, size = 15, color = c.txt, weight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingsButton(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = KetoTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.surf)
            .border(1.dp, c.bd, RoundedCornerShape(13.dp))
            .then(if (enabled) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        KText(title, size = 15, color = if (enabled) c.txt else c.txtD, weight = FontWeight.SemiBold)
        if (subtitle != null) KText(subtitle, size = 12, color = c.txtD)
    }
}

@Composable
private fun SettingsDivider(note: String) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.surf2)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        KText(note, size = 13, color = c.txtM)
    }
}

@Composable
private fun InfoBanner(text: String) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.gold.copy(alpha = 0.08f))
            .border(1.dp, c.gold.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
            .padding(14.dp),
    ) {
        KText(text, size = 13, color = c.gold)
    }
}

@Composable
private fun SelectChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = KetoTheme.colors
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) c.accent.copy(alpha = 0.15f) else c.surf)
            .border(1.5.dp, if (selected) c.accent else c.bdI, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        KText(label, size = 14, color = if (selected) c.accent else c.txtM, weight = FontWeight.SemiBold)
    }
}

@Composable
private fun RemovableChip(label: String, onRemove: () -> Unit) {
    val c = KetoTheme.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.surf)
            .border(1.dp, c.bd, RoundedCornerShape(20.dp))
            .padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        KText(label, size = 14, color = c.txt)
        Box(
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(50))
                .background(c.bdI)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center,
        ) {
            KText("×", size = 14, color = c.txtM, weight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SmallActionButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        KText(label, size = 13, color = color, weight = FontWeight.SemiBold)
    }
}

@Composable
private fun SingleLineInput(value: String, onValueChange: (String) -> Unit, placeholder: String) {
    val c = KetoTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.inp)
            .border(1.dp, c.bd, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        if (value.isEmpty()) KText(placeholder, size = 15, color = c.txtD)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = c.txt, fontSize = 15.sp),
            cursorBrush = SolidColor(c.accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StorageBar(stats: StorageStats?) {
    val c = KetoTheme.colors
    // Animate the fill from 0 → actual percentage when the page opens.
    var targetFraction by remember { mutableFloatStateOf(0f) }
    val animatedPct by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "storage_fill",
    )
    LaunchedEffect(stats?.pct) { targetFraction = stats?.pct ?: 0f }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(c.surf)
            .border(1.dp, c.bd, RoundedCornerShape(13.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            KText("Storage", size = 14, color = c.txtM)
            KText(
                if (stats == null) "Calculating…" else "${formatKB(stats.totalKB)} used",
                size = 13,
                color = c.txtD,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(c.surf2),
        ) {
            if (animatedPct > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(animatedPct)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(c.accent),
                )
            }
        }
        if (stats != null) {
            KText(
                "${stats.days} day(s) logged · ${stats.photoCount} photo(s) · " +
                    "${formatKB(stats.dbKB)} log data + ${formatKB(stats.photoKB)} photos",
                size = 12,
                color = c.txtD,
            )
        }
    }
}

// ── Utilities ─────────────────────────────────────────────────────────────────

private fun formatKB(kb: Int): String =
    if (kb >= 1024) "%.1f MB".format(kb / 1024f) else "$kb KB"

private fun hourLabel(hour: Int): String = when (hour) {
    8 -> "Morning (8 AM)"
    14 -> "Afternoon (2 PM)"
    20 -> "Evening (8 PM)"
    else -> "%02d:00".format(hour)
}
