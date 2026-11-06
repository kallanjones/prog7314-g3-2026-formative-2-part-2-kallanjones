package com.eventfinder.app.ui.screens.tickets

import com.eventfinder.app.R
import androidx.compose.ui.res.stringResource
import com.eventfinder.app.ui.theme.LocalAccentText
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.eventfinder.app.ui.theme.AccentGreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eventfinder.app.di.AppContainer
import com.eventfinder.app.domain.model.Event
import com.eventfinder.app.domain.model.EventTicket
import com.eventfinder.app.ui.components.EventFinderImage
import com.eventfinder.app.ui.components.QrImage
import com.eventfinder.app.ui.screens.preferences.BrandTextAction
import com.eventfinder.app.ui.screens.preferences.PreferenceHeading
import com.eventfinder.app.ui.screens.preferences.PreferenceHeroIcon
import com.eventfinder.app.ui.screens.preferences.PreferenceScaffold
import com.eventfinder.app.utils.DateTimeUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val BrandGreen: Color
    @Composable get() = LocalAccentText.current

/**
 * "My Tickets" — every ticket the signed-in user holds, with its QR code.
 *
 * Ticket data comes from the on-device Room store, so tickets stay available
 * at the gate with no connectivity. The store is app-private but not
 * additionally encrypted (no SQLCipher in this build).
 */
@Composable
fun MyTicketsScreen(
    container: AppContainer,
    onBack: () -> Unit,
    onEventClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { container.ticketRepository }

    val tickets by produceState(initialValue = emptyList<EventTicket>(), container) {
        val user = container.authRepository.currentUser.first()
        if (user == null) return@produceState
        repository.observeTicketsForUser(user.id)
            .map { it.sortedByDescending { ticket -> ticket.issuedAt } }
            .collect { value = it }
    }

    val events by produceState(initialValue = emptyMap<String, Event>(), container) {
        container.eventRepository.observeAllEvents()
            .map { list -> list.associateBy { it.id } }
            .collect { value = it }
    }

    PreferenceScaffold(title = stringResource(R.string.tickets_my_tickets), onBack = onBack) {
        PreferenceHeroIcon(Icons.Outlined.ConfirmationNumber)
        PreferenceHeading(
            title = stringResource(R.string.tickets_my_tickets),
            subtitle = stringResource(R.string.tickets_show_the_qr_code_at)
        )

        if (tickets.isEmpty()) {
            Spacer(Modifier.height(20.dp))
            Icon(
                Icons.Outlined.ConfirmationNumber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(46.dp)
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.tickets_no_tickets_yet),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.tickets_rsvp_to_an_event_and),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        tickets.forEach { ticket ->
            val event = events[ticket.eventId]
            TicketCard(
                ticket = ticket,
                event = event,
                onShare = {
                    val text = buildString {
                        append(context.getString(R.string.tickets_my_eventfinder_ticket))
                        event?.let { append(" for ${it.title}") }
                        append(": ${ticket.ticketCode}")
                    }
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.tickets_share_ticket_2)))
                },
                onView = { event?.let { onEventClick(it.id) } }
            )
        }
        Spacer(Modifier.height(20.dp))
        BrandTextAction(stringResource(R.string.back)) { onBack() }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun TicketCard(
    ticket: EventTicket,
    event: Event?,
    onShare: () -> Unit,
    onView: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(22.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(22.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EventFinderImage(
                imageUrl = event?.imageUrl,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                cornerRadius = 12.dp
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.fillMaxWidth(0.82f)) {
                Text(
                    event?.title ?: stringResource(R.string.tickets_event_ticket),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                event?.let {
                    Text(
                        DateTimeUtils.formatFullDateTime(it.startDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            QrImage(
                value = ticket.ticketCode,
                contentDescription = stringResource(R.string.ticket_qr_desc, ticket.ticketCode),
                modifier = Modifier.size(210.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.tickets_tap_to_enlarge_at_the),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (ticket.checkedInAt != null) {
                Icon(
                    Icons.Outlined.QrCode2,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.size(6.dp))
            }
            Text(
                if (ticket.checkedInAt != null) stringResource(R.string.tickets_checked_in_code, ticket.ticketCode) else stringResource(R.string.scanner_ticket_named, ticket.ticketCode),
                style = MaterialTheme.typography.labelMedium,
                color = BrandGreen,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onShare,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.IosShare, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.tickets_share_ticket), fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onView,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Outlined.Wallet, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.detail_view_event))
            }
        }
    }
}
