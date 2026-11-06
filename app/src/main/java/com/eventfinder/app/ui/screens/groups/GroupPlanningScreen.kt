package com.eventfinder.app.ui.screens.groups

import androidx.compose.ui.res.pluralStringResource
import com.eventfinder.app.R
import androidx.compose.ui.res.stringResource
import com.eventfinder.app.ui.theme.LocalAccentText
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Accessibility
import androidx.compose.material.icons.outlined.EventRepeat
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.eventfinder.app.ui.theme.AccentGreen
import com.eventfinder.app.ui.theme.ShowcaseNavy
import com.eventfinder.app.ui.theme.WarmAmber
import com.eventfinder.app.ui.theme.GoogleBlue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eventfinder.app.di.AppContainer
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.GroupPlan
import com.eventfinder.app.ui.components.BringAFriendSheet
import com.eventfinder.app.ui.components.EventFinderImage
import com.eventfinder.app.ui.components.MeetingPointSheet
import com.eventfinder.app.utils.DateTimeUtils
import kotlinx.coroutines.launch
import java.util.UUID

private val BrandGreen: Color
    /** Brand accent that stays legible: emerald on night surfaces, the
     *  darker brand green on white surfaces. */
    @Composable get() = MaterialTheme.colorScheme.primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupPlanningScreen(
    container: AppContainer,
    eventId: String,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val repository = remember { container.groupPlanningRepository }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    var plans by remember { mutableStateOf<List<GroupPlan>>(emptyList()) }
    var event by remember { mutableStateOf<Event?>(null) }
    var notificationsOn by remember { mutableStateOf(true) }
    var recurringOn by remember { mutableStateOf(false) }
    var accessibilityOn by remember { mutableStateOf(false) }
    var showMeetingSheet by remember { mutableStateOf(false) }
    var showInviteSheet by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        event = container.eventRepository.getEvent(eventId)
        repository.observePlansForEvent(eventId).collect { plans = it }
    }

    val plan = plans.firstOrNull()
    val title = event?.title ?: stringResource(R.string.group_group_plan)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.group_group_plan), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            EventHeadline(event = event, fallbackTitle = title)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarStack(count = plan?.members?.size ?: 1)
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        stringResource(R.string.group_group_rsvp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = (plan?.members?.size ?: 1).let { going ->
                            val goingText = pluralStringResource(R.plurals.group_going_count, going, going)
                            if (accessibilityOn) "$goingText • ${stringResource(R.string.group_accessible_seats)}" else goingText
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { showInviteSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandGreen)
                    ) {
                        Icon(
                            Icons.Outlined.GroupAdd,
                            contentDescription = null,
                            tint = LocalAccentText.current,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.detail_invite_friends),
                            color = LocalAccentText.current,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column {
                    GroupRow(
                        icon = Icons.Outlined.LocationOn,
                        title = stringResource(R.string.group_meeting_point),
                        subtitle = plan?.meetingPointLabel ?: stringResource(R.string.group_choose_a_convenient_spot),
                        trailing = stringResource(R.string.group_change),
                        onClick = { showMeetingSheet = true }
                    )
                    HorizontalDivider()
                    GroupRow(
                        icon = Icons.Outlined.NotificationsActive,
                        title = stringResource(R.string.group_plan_notifications),
                        subtitle = stringResource(R.string.group_get_notified_about_changes),
                        switchChecked = notificationsOn,
                        onSwitch = { notificationsOn = it }
                    )
                    HorizontalDivider()
                    GroupRow(
                        icon = Icons.Outlined.EventRepeat,
                        title = stringResource(R.string.group_recurring_event),
                        subtitle = stringResource(R.string.group_repeat_this_plan_each_week),
                        switchChecked = recurringOn,
                        onSwitch = { recurringOn = it }
                    )
                    HorizontalDivider()
                    GroupRow(
                        icon = Icons.Outlined.Accessibility,
                        title = stringResource(R.string.group_accessibility_options),
                        subtitle = stringResource(R.string.group_wheelchair_access_quiet_zones),
                        switchChecked = accessibilityOn,
                        onSwitch = { accessibilityOn = it }
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Group,
                        contentDescription = null,
                        tint = LocalAccentText.current,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (plans.isEmpty()) stringResource(R.string.group_no_plan_saved_yet_save)
                        else "${plans.size} plan${if (plans.size == 1) "" else "s"} for this event",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = {
                    if (saving) return@Button
                    saving = true
                    scope.launch {
                        val now = System.currentTimeMillis()
                        val outcome = if (plan == null) {
                            repository.createPlan(
                                GroupPlan(
                                    id = UUID.randomUUID().toString(),
                                    eventId = eventId,
                                    creatorId = "current_user",
                                    members = setOf("current_user"),
                                    meetingPointLabel = null,
                                    meetingLat = null,
                                    meetingLng = null,
                                    proposedTime = event?.startDate ?: now,
                                    createdAt = now
                                )
                            )
                        } else {
                            Result.success(Unit)
                        }
                        saving = false
                        snackbarHostState.showSnackbar(
                            if (outcome.isSuccess) context.getString(R.string.group_plan_saved) else context.getString(R.string.group_could_not_save_plan)
                        )
                    }
                },
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(stringResource(R.string.group_save_plan), fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showMeetingSheet) {
        MeetingPointSheet(
            groupName = title,
            initialLabel = plan?.meetingPointLabel,
            onDismiss = { showMeetingSheet = false },
            onConfirm = { label, lat, lng ->
                showMeetingSheet = false
                scope.launch {
                    val existing = plan
                    if (existing == null) {
                        repository.createPlan(
                            GroupPlan(
                                id = UUID.randomUUID().toString(),
                                eventId = eventId,
                                creatorId = "current_user",
                                members = setOf("current_user"),
                                meetingPointLabel = label,
                                meetingLat = lat,
                                meetingLng = lng,
                                proposedTime = event?.startDate ?: System.currentTimeMillis(),
                                createdAt = System.currentTimeMillis()
                            )
                        )
                    } else {
                        repository.updateMeetingPoint(existing.id, label, lat, lng)
                    }
                    snackbarHostState.showSnackbar(context.getString(R.string.group_meeting_point_set, label))
                }
            }
        )
    }

    if (showInviteSheet) {
        BringAFriendSheet(
            eventTitle = title,
            eventUrl = "https://eventfinder.co.za/event/$eventId",
            onDismiss = { showInviteSheet = false }
        )
    }
}

@Composable
private fun EventHeadline(event: Event?, fallbackTitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        EventFinderImage(
            imageUrl = event?.imageUrl,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            cornerRadius = 14.dp
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = event?.title ?: fallbackTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            Spacer(Modifier.height(2.dp))
            val subtitle = event?.let {
                "${DateTimeUtils.formatShortDate(it.startDate)} • ${DateTimeUtils.formatTime(it.startDate)}"
            } ?: stringResource(R.string.group_invite_friends_and_plan_together)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AvatarStack(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val shown = count.coerceIn(1, 3)
        repeat(shown) { index ->
            val palette = listOf(AccentGreen, GoogleBlue, WarmAmber)
            Box(
                modifier = Modifier
                    .padding(start = if (index == 0) 0.dp else (-10).dp)
                    .size(36.dp)
                    .background(palette[index % palette.size], CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = listOf("A", "T", "S")[index % 3],
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = ShowcaseNavy
                )
            }
        }
        if (count > shown) {
            Text(
                " +${count - shown}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GroupRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: String? = null,
    onClick: (() -> Unit)? = null,
    switchChecked: Boolean? = null,
    onSwitch: ((Boolean) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (switchChecked != null && onSwitch != null) {
            Switch(checked = switchChecked, onCheckedChange = onSwitch)
        } else if (trailing != null) {
            Text(
                trailing,
                style = MaterialTheme.typography.labelLarge,
                color = LocalAccentText.current,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
