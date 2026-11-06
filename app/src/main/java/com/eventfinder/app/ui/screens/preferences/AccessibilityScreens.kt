package com.eventfinder.app.ui.screens.preferences

import com.eventfinder.app.R
import androidx.compose.ui.res.stringResource
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Accessible
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.EventSeat
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Hearing
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.MusicOff
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.SensorOccupied
import androidx.compose.material.icons.outlined.SettingsAccessibility
import androidx.compose.material.icons.outlined.SocialDistance
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Wc
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.eventfinder.app.data.store.UserPreferences
import com.eventfinder.app.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Accessibility preferences shared by the hub and its detail screens. */
data class AccessibilityUiState(
    val wheelchairNeeded: Boolean = false,
    val loudNoiseWarning: Boolean = false,
    val noMusic: Boolean = false,
    val sensoryFriendly: Boolean = false,
    val accessibilityFirstSorting: Boolean = false,
    val noiseLevel: String = "off",
    val sensoryTriggers: Set<String> = emptySet(),
    val features: Set<String> = emptySet()
)

private class AccessibilityViewModel(private val preferences: UserPreferences) : ViewModel() {

    private val flags = combine(
        preferences.wheelchairNeeded,
        preferences.loudNoiseNeeded,
        preferences.noMusicNeeded,
        preferences.lowSensoryNeeded,
        preferences.accessibilityFirstSorting
    ) { wheelchair, loud, noMusic, sensory, sorting ->
        listOf(wheelchair, loud, noMusic, sensory, sorting)
    }

    private val details = combine(
        preferences.noiseLevel,
        preferences.sensoryTriggers,
        preferences.accessibilityFeatures
    ) { noise, triggers, features -> Triple(noise, triggers, features) }

    val state = combine(flags, details) { f, d ->
        AccessibilityUiState(
            wheelchairNeeded = f[0],
            loudNoiseWarning = f[1],
            noMusic = f[2],
            sensoryFriendly = f[3],
            accessibilityFirstSorting = f[4],
            noiseLevel = d.first,
            sensoryTriggers = d.second,
            features = d.third
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccessibilityUiState())

    fun setWheelchairNeeded(value: Boolean) = viewModelScope.launch { preferences.setWheelchairNeeded(value) }
    fun setLoudNoise(value: Boolean) = viewModelScope.launch { preferences.setLoudNoiseNeeded(value) }
    fun setNoMusic(value: Boolean) = viewModelScope.launch { preferences.setNoMusicNeeded(value) }
    fun setSensoryFriendly(value: Boolean) = viewModelScope.launch { preferences.setLowSensoryNeeded(value) }
    fun setSorting(value: Boolean) = viewModelScope.launch { preferences.setAccessibilityFirstSorting(value) }
    fun setNoiseLevel(value: String) = viewModelScope.launch { preferences.setNoiseLevel(value) }
    fun toggleFeature(feature: String) = viewModelScope.launch {
        val current = state.value.features
        preferences.setAccessibilityFeatures(
            if (feature in current) current - feature else current + feature
        )
    }

    fun toggleTrigger(trigger: String) = viewModelScope.launch {
        val current = state.value.sensoryTriggers
        preferences.setSensoryTriggers(
            if (trigger in current) current - trigger else current + trigger
        )
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { AccessibilityViewModel(container.preferences) }
        }
    }
}

/** Confirms instant-persisted changes, matching the showcase's save buttons. */
private fun confirmSaved(message: String, context: android.content.Context) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

/** "Accessibility — Make events more accessible for you". */
@Composable
fun AccessibilityScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onOpenWheelchair: () -> Unit,
    onOpenLoudNoise: () -> Unit,
    onOpenNoMusic: () -> Unit,
    onOpenSensory: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val vm: AccessibilityViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(factory = AccessibilityViewModel.factory(container))
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    PreferenceScaffold(title = stringResource(R.string.a11y_accessibility), onBack = onBack) {
        PreferenceHeading(
            title = stringResource(R.string.a11y_accessibility),
            subtitle = stringResource(R.string.a11y_make_events_more_accessible_for)
        )

        PreferenceSwitchRow(
            icon = Icons.Outlined.Accessible,
            title = stringResource(R.string.a11y_wheelchair_accessible),
            checked = state.wheelchairNeeded,
            onCheckedChange = vm::setWheelchairNeeded
        )
        PreferenceSwitchRow(
            icon = Icons.Outlined.Hearing,
            title = stringResource(R.string.a11y_loud_noise_warnings),
            checked = state.loudNoiseWarning,
            onCheckedChange = vm::setLoudNoise
        )
        PreferenceSwitchRow(
            icon = Icons.Outlined.MusicOff,
            title = stringResource(R.string.a11y_no_music),
            checked = state.noMusic,
            onCheckedChange = vm::setNoMusic
        )
        PreferenceSwitchRow(
            icon = Icons.Outlined.SensorOccupied,
            title = stringResource(R.string.a11y_sensory_friendly),
            checked = state.sensoryFriendly,
            onCheckedChange = vm::setSensoryFriendly
        )
        PreferenceSwitchRow(
            icon = Icons.Outlined.Bolt,
            title = stringResource(R.string.a11y_strobe_lights),
            checked = "strobe" in state.sensoryTriggers,
            onCheckedChange = { vm.toggleTrigger("strobe") }
        )
        PreferenceSwitchRow(
            icon = Icons.Outlined.SettingsAccessibility,
            title = stringResource(R.string.a11y_accessibility_first_event_sorting),
            checked = state.accessibilityFirstSorting,
            onCheckedChange = vm::setSorting
        )

        Spacer(Modifier.height(12.dp))
        PreferenceInfoBox(stringResource(R.string.a11y_help_us_create_a_more))

        Spacer(Modifier.height(14.dp))
        RowOfLinks(
            links = listOf(
                stringResource(R.string.a11y_wheelchair_details) to onOpenWheelchair,
                stringResource(R.string.a11y_noise_sensory) to onOpenLoudNoise,
                stringResource(R.string.a11y_no_music_2) to onOpenNoMusic,
                stringResource(R.string.a11y_sensory_warnings_2) to onOpenSensory,
                stringResource(R.string.profile_title) to onOpenProfile
            )
        )

        Spacer(Modifier.height(18.dp))
        BrandPrimaryButton(stringResource(R.string.a11y_save_preferences)) {
            confirmSaved(context.getString(R.string.a11y_accessibility_preferences_saved), context)
            onBack()
        }
        BottomActionSpacer()
    }
}

/** Compact pill row so every detail screen stays one tap away. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun RowOfLinks(links: List<Pair<String, () -> Unit>>) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        links.forEach { (label, action) ->
            androidx.compose.material3.AssistChip(
                onClick = action,
                label = { Text(label, style = MaterialTheme.typography.labelMedium) }
            )
        }
    }
}

/** "Wheelchair Accessibility — Find events with wheelchair accessibility features." */
@Composable
fun WheelchairAccessibilityScreen(
    container: AppContainer,
    onBack: () -> Unit
) {
    val vm: AccessibilityViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(factory = AccessibilityViewModel.factory(container))
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    PreferenceScaffold(title = stringResource(R.string.a11y_wheelchair_accessibility), onBack = onBack) {
        PreferenceHeroIcon(Icons.Outlined.Accessible)
        PreferenceHeading(
            title = stringResource(R.string.a11y_wheelchair_accessibility),
            subtitle = stringResource(R.string.a11y_find_events_with_wheelchair_accessibility)
        )

        PreferenceToggleBlock(
            title = stringResource(R.string.a11y_only_show_accessible_events),
            checked = state.wheelchairNeeded,
            onCheckedChange = vm::setWheelchairNeeded
        )

        Spacer(Modifier.height(18.dp))
        PreferenceSectionLabel(stringResource(R.string.a11y_accessibility_features))
        AccessibilityFeatureRow(stringResource(R.string.a11y_ramp_access), Icons.Outlined.Stairs, state, vm)
        AccessibilityFeatureRow(stringResource(R.string.a11y_accessible_toilets), Icons.Outlined.Wc, state, vm)
        AccessibilityFeatureRow(stringResource(R.string.a11y_reserved_seating), Icons.Outlined.EventSeat, state, vm)
        AccessibilityFeatureRow(stringResource(R.string.a11y_other_features), Icons.Outlined.Accessibility, state, vm)

        Spacer(Modifier.height(18.dp))
        BrandPrimaryButton(stringResource(R.string.a11y_save_settings)) {
            confirmSaved(context.getString(R.string.a11y_wheelchair_settings_saved), context)
            onBack()
        }
        BottomActionSpacer()
    }
}

@Composable
private fun AccessibilityFeatureRow(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    state: AccessibilityUiState,
    vm: AccessibilityViewModel
) {
    val key = label.lowercase().replace(" ", "_")
    PreferenceSwitchRow(
        icon = icon,
        title = label,
        checked = key in state.features,
        onCheckedChange = { vm.toggleFeature(key) }
    )
}

/** "Loud Noise Sensitivity". */
@Composable
fun LoudNoiseScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: AccessibilityViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(factory = AccessibilityViewModel.factory(container))
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    PreferenceScaffold(title = stringResource(R.string.a11y_loud_noise_sensitivity), onBack = onBack) {
        PreferenceHeroIcon(Icons.Outlined.Hearing)
        PreferenceHeading(
            title = stringResource(R.string.a11y_loud_noise_sensitivity),
            subtitle = stringResource(R.string.a11y_choose_your_preference_for_loud)
        )

        PreferenceChoiceRow(stringResource(R.string.a11y_no_preference), state.noiseLevel == "off") { vm.setNoiseLevel("off") }
        PreferenceChoiceRow(stringResource(R.string.a11y_low_noise), state.noiseLevel == "low") { vm.setNoiseLevel("low") }
        PreferenceChoiceRow(stringResource(R.string.a11y_moderate_noise), state.noiseLevel == "moderate") { vm.setNoiseLevel("moderate") }
        PreferenceChoiceRow(stringResource(R.string.a11y_high_noise_warn), state.noiseLevel == "high") { vm.setNoiseLevel("high") }

        Spacer(Modifier.height(12.dp))
        BrandPrimaryButton(stringResource(R.string.a11y_save_preference)) {
            confirmSaved(context.getString(R.string.a11y_noise_preference_saved), context)
            onBack()
        }
        BottomActionSpacer()
    }
}

/** "No Music Preference". */
@Composable
fun NoMusicScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: AccessibilityViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(factory = AccessibilityViewModel.factory(container))
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    PreferenceScaffold(title = stringResource(R.string.a11y_no_music_preference), onBack = onBack) {
        PreferenceHeroIcon(Icons.Outlined.MusicOff)
        PreferenceHeading(
            title = stringResource(R.string.a11y_no_music_preference),
            subtitle = stringResource(R.string.a11y_find_events_without_live_music)
        )

        PreferenceToggleBlock(
            title = stringResource(R.string.a11y_only_show_no_music_events),
            checked = state.noMusic,
            onCheckedChange = vm::setNoMusic
        )

        Spacer(Modifier.height(20.dp))
        BrandPrimaryButton(stringResource(R.string.a11y_save_settings)) {
            confirmSaved(context.getString(R.string.a11y_music_preference_saved), context)
            onBack()
        }
        BottomActionSpacer()
    }
}

/** "Sensory Preferences — Sensory Warnings". */
@Composable
fun SensoryPreferencesScreen(container: AppContainer, onBack: () -> Unit) {
    val vm: AccessibilityViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(factory = AccessibilityViewModel.factory(container))
    val state by vm.state.collectAsState()
    val context = LocalContext.current

    PreferenceScaffold(title = stringResource(R.string.a11y_sensory_preferences), onBack = onBack) {
        PreferenceHeroIcon(Icons.Outlined.SensorOccupied)
        PreferenceHeading(
            title = stringResource(R.string.a11y_sensory_warnings),
            subtitle = stringResource(R.string.a11y_get_warnings_for_events_with)
        )

        PreferenceSwitchRow(
            icon = Icons.Outlined.LightMode,
            title = stringResource(R.string.a11y_flashing_lights),
            checked = "flashing" in state.sensoryTriggers,
            onCheckedChange = { vm.toggleTrigger("flashing") }
        )
        PreferenceSwitchRow(
            icon = Icons.Outlined.Bolt,
            title = stringResource(R.string.a11y_strobe_effects),
            checked = "strobe" in state.sensoryTriggers,
            onCheckedChange = { vm.toggleTrigger("strobe") }
        )
        PreferenceSwitchRow(
            icon = Icons.Outlined.Groups,
            title = stringResource(R.string.a11y_loud_crowds),
            checked = "crowds" in state.sensoryTriggers,
            onCheckedChange = { vm.toggleTrigger("crowds") }
        )
        PreferenceSwitchRow(
            icon = Icons.Outlined.SocialDistance,
            title = stringResource(R.string.a11y_other_sensory_triggers),
            checked = "other" in state.sensoryTriggers,
            onCheckedChange = { vm.toggleTrigger("other") }
        )

        Spacer(Modifier.height(20.dp))
        BrandPrimaryButton(stringResource(R.string.a11y_save_preferences)) {
            confirmSaved(context.getString(R.string.a11y_sensory_preferences_saved), context)
            onBack()
        }
        BottomActionSpacer()
    }
}

/** "Accessibility Profile — Customize your accessibility preferences for a better experience." */
@Composable
fun AccessibilityProfileScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val vm: AccessibilityViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel(factory = AccessibilityViewModel.factory(container))
    val state by vm.state.collectAsState()

    PreferenceScaffold(title = stringResource(R.string.a11y_accessibility_profile), onBack = onBack) {
        PreferenceHeroIcon(Icons.Outlined.Accessibility)
        PreferenceHeading(
            title = stringResource(R.string.a11y_accessibility_profile),
            subtitle = stringResource(R.string.a11y_customize_your_accessibility_preferences_for)
        )

        PreferenceSummaryRow(
            icon = Icons.Outlined.Accessible,
            title = stringResource(R.string.a11y_wheelchair_access),
            value = if (state.wheelchairNeeded) stringResource(R.string.a11y_on) else stringResource(R.string.a11y_off)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PreferenceSummaryRow(
            icon = Icons.Outlined.Hearing,
            title = stringResource(R.string.a11y_loud_noise),
            value = when (state.noiseLevel) {
                "high" -> stringResource(R.string.a11y_warn_me)
                "moderate" -> stringResource(R.string.a11y_moderate)
                "low" -> stringResource(R.string.a11y_low)
                else -> if (state.loudNoiseWarning) stringResource(R.string.a11y_on) else stringResource(R.string.a11y_off)
            }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PreferenceSummaryRow(
            icon = Icons.Outlined.MusicOff,
            title = stringResource(R.string.a11y_no_music),
            value = if (state.noMusic) stringResource(R.string.a11y_on) else stringResource(R.string.a11y_off)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PreferenceSummaryRow(
            icon = Icons.Outlined.SensorOccupied,
            title = stringResource(R.string.a11y_sensory_warnings),
            value = if (state.sensoryTriggers.isEmpty()) stringResource(R.string.a11y_off) else "${state.sensoryTriggers.size} active"
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        PreferenceSummaryRow(
            icon = Icons.Outlined.Route,
            title = stringResource(R.string.a11y_commute_preference),
            value = if (state.accessibilityFirstSorting) stringResource(R.string.a11y_accessible_first) else stringResource(R.string.avatar_style_default)
        )

        Spacer(Modifier.height(20.dp))
        BrandPrimaryButton(stringResource(R.string.a11y_update_profile), onClick = onEdit)
        BottomActionSpacer()
    }
}
