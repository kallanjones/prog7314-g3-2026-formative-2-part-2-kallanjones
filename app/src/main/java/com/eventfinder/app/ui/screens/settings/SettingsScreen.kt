package com.eventfinder.app.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Interests
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eventfinder.app.R
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.eventfinder.app.di.AppContainer
import com.eventfinder.app.domain.model.SupportedLanguage
import com.eventfinder.app.security.BiometricAuth
import com.eventfinder.app.security.BiometricFailure
import com.eventfinder.app.ui.components.resolve

/**
 * Screen 9 (Settings): language, biometric login and notification preferences.
 * Changing the language persists it and recreates the activity so the new
 * locale is applied everywhere (FR-08).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenAccessibility: () -> Unit = {},
    onOpenAccessibilityProfile: () -> Unit = {},
    onOpenCommute: () -> Unit = {},
    onOpenInterests: () -> Unit = {},
    onOpenModeration: () -> Unit = {},
    onOpenReportScam: () -> Unit = {},
    onOpenCollections: () -> Unit = {},
    onOpenBlockedUsers: () -> Unit = {},
    onLoggedOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container, context))
    val state by viewModel.uiState.collectAsState()
    val themeMode by container.preferences.themeMode.collectAsState(initial = "system")
    val adminMode by container.preferences.adminMode.collectAsState(initial = false)
    var unlockTaps by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val biometricSupported = BiometricAuth.isAvailable(context)
    val hostActivity = context as? androidx.fragment.app.FragmentActivity
    // Held so the prompt's callbacks stay reachable while the system dialog is up.
    var biometricPrompt by remember { mutableStateOf<BiometricAuth?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val screenScope = rememberCoroutineScope()

    var showChangePassword by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }

    // Recreate once when the persisted locale changes so the UI redraws.
    // Save the route before recreate so we can navigate back after the Activity restarts.
    val languageAtStart = remember { state.language }
    LaunchedEffect(state.language) {
        if (state.language != languageAtStart) {
            viewModel.savePendingRoute("settings")
            (context as? Activity)?.recreate()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { msg ->
            msg.resolve(context)?.let { snackbarHostState.showSnackbar(it) }
        }
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            onDismiss = { showChangePassword = false },
            onConfirm = { current, newPassword, confirm ->
                showChangePassword = false
                viewModel.changePassword(current, newPassword, confirm)
            }
        )
    }

    if (showDeleteAccount) {
        AlertDialog(
            onDismissRequest = { showDeleteAccount = false },
            title = { Text(stringResource(R.string.delete_account)) },
            text = { Text(stringResource(R.string.delete_account_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAccount = false
                    viewModel.deleteAccount(onLoggedOut)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccount = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                // The settings list is taller than the screen on small devices;
                // without scrolling the "Discovery defaults" card (default city +
                // radius) was cut off and the slider overlapped the city field.
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.change_password)) },
                        supportingContent = { Text(stringResource(R.string.change_password_hint)) },
                        leadingContent = { Image(painterResource(R.drawable.usr_lock), contentDescription = null, modifier = Modifier.size(22.dp)) },
                        modifier = Modifier.clickable { showChangePassword = true }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.clear_cache)) },
                        supportingContent = { Text(stringResource(R.string.clear_cache_hint)) },
                        leadingContent = { Image(painterResource(R.drawable.usr_eraser), contentDescription = null, modifier = Modifier.size(22.dp)) },
                        modifier = Modifier.clickable { viewModel.clearCache() }
                    )
                    // Destructive maintenance stays hidden until admin mode is
                    // unlocked from the version row below.
                    if (adminMode) {
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.repair_local_data)) },
                            supportingContent = { Text(stringResource(R.string.repair_local_data_hint)) },
                            leadingContent = { Image(painterResource(R.drawable.usr_check), contentDescription = null, modifier = Modifier.size(22.dp)) },
                            modifier = Modifier.clickable { viewModel.repairLocalData() }
                        )
                        HorizontalDivider()
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.settings_share_crash_log)) },
                            supportingContent = {
                                Text(stringResource(R.string.settings_most_recent_captured_crash_shared))
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Outlined.BugReport,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            modifier = Modifier.clickable {
                                val report =
                                    com.eventfinder.app.utils.CrashReporter.latest(context)
                                if (report == null) {
                                    android.widget.Toast.makeText(
                                        context,
                                        context.getString(R.string.settings_no_crash_reports_captured),
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val intent = android.content.Intent(
                                        android.content.Intent.ACTION_SEND
                                    ).apply {
                                        type = "text/plain"
                                        putExtra(
                                            android.content.Intent.EXTRA_SUBJECT,
                                            context.getString(R.string.settings_eventfinder_crash_report)
                                        )
                                        putExtra(
                                            android.content.Intent.EXTRA_TEXT,
                                            report.readText()
                                        )
                                    }
                                    context.startActivity(
                                        android.content.Intent.createChooser(
                                            intent,
                                            context.getString(R.string.settings_share_crash_report)
                                        )
                                    )
                                }
                            }
                        )
                    }
                    HorizontalDivider()
                    ListItem(
                        headlineContent = {
                            Text(stringResource(R.string.delete_account), color = MaterialTheme.colorScheme.error)
                        },
                        supportingContent = { Text(stringResource(R.string.delete_account_hint)) },
                        leadingContent = {
                            Icon(
                                Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        modifier = Modifier.clickable { showDeleteAccount = true }
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_analytics_consent)) },
                        supportingContent = { Text(stringResource(R.string.settings_allow_anonymous_usage_analytics)) },
                        trailingContent = {
                            Switch(
                                checked = state.analyticsConsent,
                                onCheckedChange = viewModel::setAnalyticsConsent
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_location_consent)) },
                        supportingContent = { Text(stringResource(R.string.settings_allow_location_based_discovery)) },
                        trailingContent = {
                            Switch(
                                checked = state.locationConsent,
                                onCheckedChange = viewModel::setLocationConsent
                            )
                        }
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column {
                    SettingsNavRow(
                        title = stringResource(R.string.a11y_accessibility),
                        subtitle = stringResource(R.string.a11y_make_events_more_accessible_for),
                        icon = Icons.Outlined.Accessibility,
                        onClick = onOpenAccessibility
                    )
                    HorizontalDivider()
                    SettingsNavRow(
                        title = stringResource(R.string.settings_accessibility_profile),
                        subtitle = stringResource(R.string.settings_wheelchair_noise_sensory_and_music),
                        icon = Icons.Outlined.PersonOutline,
                        onClick = onOpenAccessibilityProfile
                    )
                    HorizontalDivider()
                    SettingsNavRow(
                        title = stringResource(R.string.settings_location_commute),
                        subtitle = stringResource(R.string.settings_how_far_you_are_willing),
                        icon = Icons.Outlined.DirectionsCar,
                        onClick = onOpenCommute
                    )
                    HorizontalDivider()
                    SettingsNavRow(
                        title = stringResource(R.string.settings_interests),
                        subtitle = stringResource(R.string.settings_tune_your_event_recommendations),
                        icon = Icons.Outlined.Interests,
                        onClick = onOpenInterests
                    )
                    HorizontalDivider()
                    SettingsNavRow(
                        title = stringResource(R.string.settings_event_collections),
                        subtitle = stringResource(R.string.settings_your_saved_lists_of_events),
                        icon = Icons.Outlined.Collections,
                        onClick = onOpenCollections
                    )
                }
            }

            SmartNotificationCard(container = container)

            Card(Modifier.fillMaxWidth()) {
                Column {
                    SettingsNavRow(
                        title = stringResource(R.string.settings_moderation_tools),
                        subtitle = stringResource(R.string.settings_review_and_resolve_reported_content),
                        icon = Icons.Outlined.Shield,
                        onClick = onOpenModeration
                    )
                    HorizontalDivider()
                    SettingsNavRow(
                        title = stringResource(R.string.settings_report_a_scam_or_issue),
                        subtitle = stringResource(R.string.settings_help_keep_eventfinder_safe_for),
                        icon = Icons.Outlined.Flag,
                        onClick = onOpenReportScam
                    )
                    HorizontalDivider()
                    SettingsNavRow(
                        title = stringResource(R.string.settings_blocked_users),
                        subtitle = stringResource(R.string.settings_review_and_unblock_people_you),
                        icon = Icons.Outlined.Block,
                        onClick = onOpenBlockedUsers
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_about_eventfinder)) },
                    supportingContent = {
                        Text(
                            if (adminMode) stringResource(R.string.settings_version_admin, com.eventfinder.app.BuildConfig.VERSION_NAME)
                            else stringResource(R.string.settings_version, com.eventfinder.app.BuildConfig.VERSION_NAME)
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    // Seven taps toggles admin mode; a deliberate, harmless
                    // gesture that never fires by accident.
                    modifier = Modifier.clickable {
                        unlockTaps += 1
                        if (unlockTaps >= 7) {
                            unlockTaps = 0
                            screenScope.launch { container.preferences.setAdminMode(!adminMode) }
                        }
                    }
                )
            }

            Card(Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_wheelchair_accessible_venues_only)) },
                        trailingContent = {
                            Switch(
                                checked = state.wheelchairNeeded,
                                onCheckedChange = viewModel::setWheelchairNeeded
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_low_sensory_events_only)) },
                        trailingContent = {
                            Switch(
                                checked = state.lowSensoryNeeded,
                                onCheckedChange = viewModel::setLowSensoryNeeded
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_warn_me_about_loud_music)) },
                        supportingContent = { Text(stringResource(R.string.settings_flags_noise_heavy_events_before)) },
                        trailingContent = {
                            Switch(
                                checked = state.loudNoiseNeeded,
                                onCheckedChange = viewModel::setLoudNoiseNeeded
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_prefer_music_free_events)) },
                        supportingContent = { Text(stringResource(R.string.settings_sensory_and_religious_accessibility)) },
                        trailingContent = {
                            Switch(
                                checked = state.noMusicNeeded,
                                onCheckedChange = viewModel::setNoMusicNeeded
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_reduced_motion)) },
                        trailingContent = {
                            Switch(
                                checked = state.reducedMotion,
                                onCheckedChange = viewModel::setReducedMotion
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_high_contrast)) },
                        trailingContent = {
                            Switch(
                                checked = state.highContrast,
                                onCheckedChange = viewModel::setHighContrast
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_event_notifications)) },
                        supportingContent = { Text(stringResource(R.string.settings_consent_for_reminders_and_new)) },
                        trailingContent = {
                            Switch(
                                checked = state.notificationConsent,
                                onCheckedChange = viewModel::setNotificationConsent
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_export_my_data)) },
                        supportingContent = { Text(stringResource(R.string.settings_download_a_copy_of_your)) },
                        modifier = Modifier.clickable { viewModel.exportData() }
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_appearance)) },
                        supportingContent = { Text(stringResource(R.string.settings_choose_light_dark_or_follow)) },
                        trailingContent = {
                            var themeExpanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { themeExpanded = true }) {
                                    Text(when (themeMode) { "light" -> stringResource(R.string.settings_theme_light); "dark" -> stringResource(R.string.settings_theme_dark); else -> stringResource(R.string.settings_theme_system) })
                                }
                                DropdownMenu(expanded = themeExpanded, onDismissRequest = { themeExpanded = false }) {
                                    listOf("system" to stringResource(R.string.settings_theme_system_default), "light" to stringResource(R.string.settings_theme_light), "dark" to stringResource(R.string.settings_theme_dark)).forEach { (value, label) ->
                                        DropdownMenuItem(text = { Text(label) }, onClick = {
                                            themeExpanded = false
                                            screenScope.launch { container.preferences.setThemeMode(value) }
                                        })
                                    }
                                }
                            }
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.settings_material_you_colors)) },
                        supportingContent = { Text(stringResource(R.string.settings_palette_from_your_wallpaper_android)) },
                        trailingContent = {
                            Switch(
                                checked = state.dynamicColor,
                                onCheckedChange = viewModel::setDynamicColor
                            )
                        }
                    )
                    HorizontalDivider()
                    LanguageSelector(state.language, viewModel::setLanguage)
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.biometric_login)) },
                        supportingContent = {
                            Text(
                                if (biometricSupported) stringResource(R.string.biometric_available)
                                else stringResource(R.string.biometric_unavailable)
                            )
                        },
                        leadingContent = { Image(painterResource(R.drawable.usr_fingerprint), contentDescription = null, modifier = Modifier.size(22.dp)) },
                        trailingContent = {
                            Switch(
                                checked = state.biometricEnabled,
                                enabled = biometricSupported,
                                onCheckedChange = { wanted ->
                                    if (!wanted) {
                                        viewModel.setBiometric(false)
                                    } else if (hostActivity == null) {
                                        viewModel.setBiometric(true)
                                    } else {
                                        // Prove the prompt and Keystore key work before
                                        // promising an unlock on the next launch.
                                        val prompt = BiometricAuth(
                                            activity = hostActivity,
                                            onAuthenticated = { viewModel.setBiometric(true) },
                                            onError = { failure ->
                                                if (failure != BiometricFailure.CANCELLED) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        R.string.biometric_enable_failed,
                                                        android.widget.Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            }
                                        )
                                        biometricPrompt = prompt
                                        prompt.authenticate()
                                    }
                                }
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.event_reminders)) },
                        supportingContent = { Text(stringResource(R.string.event_reminders_hint)) },
                        leadingContent = { Image(painterResource(R.drawable.usr_bell), contentDescription = null, modifier = Modifier.size(22.dp)) },
                        trailingContent = {
                            Switch(
                                checked = state.remindersEnabled,
                                onCheckedChange = viewModel::setReminders
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.new_events_alerts)) },
                        leadingContent = { Image(painterResource(R.drawable.usr_bell), contentDescription = null, modifier = Modifier.size(22.dp)) },
                        trailingContent = {
                            Switch(
                                checked = state.newEventsAlerts,
                                onCheckedChange = viewModel::setNewEventsAlerts
                            )
                        }
                    )
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.discovery_settings),
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = state.defaultCity,
                        onValueChange = viewModel::setDefaultCity,
                        label = { Text(stringResource(R.string.default_city)) },
                        placeholder = { Text(stringResource(R.string.default_city_hint)) },
                        singleLine = true,
                        leadingIcon = { Image(painterResource(R.drawable.hdr_map_pin), contentDescription = null, modifier = Modifier.size(22.dp)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        stringResource(R.string.default_radius) +
                            ": " + stringResource(R.string.radius_km_value, state.defaultRadiusKm),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = state.defaultRadiusKm.toFloat(),
                        onValueChange = { viewModel.setDefaultRadiusKm(it.toInt()) },
                        valueRange = 5f..200f,
                        steps = 38,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Text(
                stringResource(R.string.settings_about, "EventFinder"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (current: String, newPassword: String, confirm: String) -> Unit
) {
    var current by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.password_rules_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it },
                    label = { Text(stringResource(R.string.current_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.new_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it },
                    label = { Text(stringResource(R.string.confirm_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(current, newPassword, confirm) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
/**
 * "Smart notifications" — the fine-grained alert switches from the 2026
 * showcase. Values live in DataStore and default to on.
 */
@Composable
private fun SmartNotificationCard(container: AppContainer) {
    val scope = rememberCoroutineScope()
    val prefs = container.preferences
    val group by prefs.notifyGroupUpdates.collectAsState(initial = true)
    val nearby by prefs.notifyNearbyEvents.collectAsState(initial = true)
    val picks by prefs.notifyPersonalisedPicks.collectAsState(initial = true)

    Card(Modifier.fillMaxWidth()) {
        Column {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_smart_notifications)) },
                supportingContent = { Text(stringResource(R.string.settings_only_the_updates_that_matter)) }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_group_updates)) },
                supportingContent = { Text(stringResource(R.string.settings_changes_to_plans_you_joined)) },
                trailingContent = {
                    Switch(
                        checked = group,
                        onCheckedChange = { value -> scope.launch { prefs.setNotifyGroupUpdates(value) } }
                    )
                }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_nearby_events)) },
                supportingContent = { Text(stringResource(R.string.settings_events_happening_close_to_you)) },
                trailingContent = {
                    Switch(
                        checked = nearby,
                        onCheckedChange = { value -> scope.launch { prefs.setNotifyNearbyEvents(value) } }
                    )
                }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_personalised_picks)) },
                supportingContent = { Text(stringResource(R.string.settings_recommendations_based_on_your_interests)) },
                trailingContent = {
                    Switch(
                        checked = picks,
                        onCheckedChange = { value -> scope.launch { prefs.setNotifyPersonalisedPicks(value) } }
                    )
                }
            )
        }
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageSelector(
    selected: SupportedLanguage,
    onSelect: (SupportedLanguage) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    // A plain Box + DropdownMenu is used (instead of ExposedDropdownMenuBox) so the
    // custom row reliably opens the menu on tap.
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {             Image(painterResource(R.drawable.usr_language), contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.language), style = MaterialTheme.typography.bodyLarge)
                Text(
                    selected.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(SupportedLanguage.ENGLISH.displayName) },
                onClick = {
                    onSelect(SupportedLanguage.ENGLISH)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text(SupportedLanguage.AFRIKAANS.displayName) },
                onClick = {
                    onSelect(SupportedLanguage.AFRIKAANS)
                    expanded = false
                }
            )
        }
    }
}