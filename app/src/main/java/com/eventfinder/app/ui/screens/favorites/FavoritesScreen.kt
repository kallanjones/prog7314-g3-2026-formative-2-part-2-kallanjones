package com.eventfinder.app.ui.screens.favorites

import androidx.compose.ui.res.pluralStringResource
import com.eventfinder.app.R
import androidx.compose.ui.res.stringResource
import com.eventfinder.app.ui.theme.LocalAccentText
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eventfinder.app.di.AppContainer
import com.eventfinder.app.ui.components.EmptyState
import com.eventfinder.app.ui.components.EventCard

@Composable
fun FavoritesScreen(
    container: AppContainer,
    onEventClick: (String) -> Unit
) {
    val viewModel: FavoritesViewModel =
        viewModel(factory = FavoritesViewModel.factory(container))

    val events by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        if (events.isEmpty()) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
                Spacer(Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.favorites_your_favourites),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.favorites_keep_the_events_you_love),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                EmptyState(
                    icon = Icons.Outlined.FavoriteBorder,
                    title = stringResource(R.string.favorites_no_saved_events_yet),
                    subtitle = stringResource(R.string.favorites_explore_events_and_tap_the),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }

        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.favorites_your_favourites),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = stringResource(R.string.favorites_events_you_ve_saved_for),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = pluralStringResource(R.plurals.favorites_saved_count, events.size, events.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = LocalAccentText.current,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = events,
                        key = { it.event.id }
                    ) { eventView ->

                        EventCard(
                            view = eventView,
                            onClick = {
                                onEventClick(eventView.event.id)
                            },
                            onFavoriteToggle = {
                                viewModel.onRemoveFavorite(eventView.event.id)
                            }
                        )
                    }
                }
            }
        }
    }
}
