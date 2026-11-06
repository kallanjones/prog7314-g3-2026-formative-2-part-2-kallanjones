package com.eventfinder.app.ui.screens.search

import androidx.compose.ui.res.pluralStringResource
import com.eventfinder.app.R
import androidx.compose.ui.res.stringResource
import com.eventfinder.app.ui.theme.LocalAccentText
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eventfinder.app.di.AppContainer
import com.eventfinder.app.ui.components.EmptyState
import com.eventfinder.app.ui.components.EventCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    container: AppContainer,
    onEventClick: (String) -> Unit
) {
    val viewModel: SearchViewModel =
        viewModel(factory = SearchViewModel.factory(container))

    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Text(
                    text = stringResource(R.string.search_find_your_next_experience),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.search_search_events_happening_around_south),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQueryChange,
                    placeholder = {
                        Text(stringResource(R.string.search_search_concerts_markets_festivals))
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = stringResource(R.string.search_title)
                        )
                    },
                    trailingIcon = {
                        if (state.query.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    viewModel.onQueryChange("")
                                    focusManager.clearFocus()
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.clear_search)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.commitSearch()
                            focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Save the current term so it stays pinned under "Saved searches".
                if (state.query.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    AssistChip(
                        onClick = {
                            viewModel.saveCurrentSearch()
                            focusManager.clearFocus()
                        },
                        enabled = !state.queryIsSaved,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        label = {
                            Text(if (state.queryIsSaved) stringResource(R.string.search_saved) else stringResource(R.string.search_save_this_search))
                        }
                    )
                }
            }

            if (state.savedSearches.isNotEmpty()) {

                Text(
                    text = stringResource(R.string.search_saved_searches),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(Modifier.height(8.dp))

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.savedSearches, key = { it }) { term ->
                        InputChip(
                            selected = state.query.equals(term, ignoreCase = true),
                            onClick = { viewModel.applySavedSearch(term) },
                            // Brand emerald instead of the secondary gold tint.
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.primary,
                                selectedTrailingIconColor = LocalAccentText.current
                            ),
                            label = { Text(term) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { viewModel.removeSavedSearch(term) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = stringResource(R.string.search_remove_saved_named, term),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }

            if (state.query.isBlank() && state.recentSearches.isNotEmpty()) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.recent_searches),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(onClick = viewModel::clearHistory) {
                        Text(stringResource(R.string.search_clear))
                    }
                }

                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.recentSearches) { term ->
                        InputChip(
                            selected = false,
                            onClick = {
                                viewModel.onQueryChange(term)
                                viewModel.commitSearch()
                            },
                            label = { Text(term) }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (state.query.isBlank()) {
                        stringResource(R.string.search_popular_events)
                    } else {
                        stringResource(R.string.search_search_results)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = pluralStringResource(R.plurals.search_found_count, state.results.size, state.results.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.results.isEmpty() && state.query.isNotBlank()) {
                EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = stringResource(R.string.no_events),
                    subtitle = stringResource(R.string.search_try_another_search_term)
                )
            } else if (state.results.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Search,
                    title = stringResource(R.string.search_no_popular_events_yet),
                    subtitle = stringResource(R.string.search_check_back_soon_for_new)
                )
            } else {
                // Two columns on tablets/foldables, one on phones — matches Home.
                val columns =
                    if (LocalConfiguration.current.screenWidthDp >= 600) 2 else 1

                if (columns == 1) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.results,
                            key = { it.event.id }
                        ) { eventView ->

                            EventCard(
                                view = eventView,
                                onClick = {
                                    onEventClick(eventView.event.id)
                                },
                                onFavoriteToggle = {
                                    viewModel.toggleFavorite(eventView.event.id)
                                }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(
                            top = 8.dp,
                            bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.results,
                            key = { it.event.id }
                        ) { eventView ->

                            EventCard(
                                view = eventView,
                                onClick = {
                                    onEventClick(eventView.event.id)
                                },
                                onFavoriteToggle = {
                                    viewModel.toggleFavorite(eventView.event.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
