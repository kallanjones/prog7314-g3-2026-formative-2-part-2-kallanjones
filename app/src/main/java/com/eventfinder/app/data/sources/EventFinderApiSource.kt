package com.eventfinder.app.data.sources

import com.eventfinder.app.data.remote.EventFinderApi
import com.eventfinder.app.data.remote.model.RemoteEvent
import com.eventfinder.app.utils.AppLogger

/**
 * Reads events from our own EventFinder REST API (the ASP.NET Core service in
 * `/api`) and presents them as an [EventSource], so API events are cached in
 * Room by the same ingestion pipeline as the public feeds.
 *
 * Events without a title or start date are skipped rather than failing the whole
 * refresh, matching how the public feed sources behave.
 */
class EventFinderApiSource(
    private val api: EventFinderApi,
    override val id: String = "eventfinder-api",
    override val displayName: String = "EventFinder API"
) : EventSource {

    private val tag = "EventFinderApiSource"

    override suspend fun fetchEvents(): List<RemoteEvent> {
        val response = api.getEvents()

        if (!response.success) {
            AppLogger.w(tag, "API returned success=false: ${response.message}")
            return emptyList()
        }

        val events = response.data.orEmpty().mapNotNull { dto ->
            val eventId = dto.id ?: return@mapNotNull null
            val title = dto.title?.trim().orEmpty()
            val start = dto.startDate

            if (title.isBlank() || start == null || start <= 0L) {
                AppLogger.d(tag, "Skipping event '$eventId' with no title or start date")
                return@mapNotNull null
            }

            RemoteEvent(
                source = id,
                sourceId = eventId,
                title = title,
                description = dto.description.orEmpty(),
                category = dto.category?.ifBlank { "OTHER" } ?: "OTHER",
                startDate = start,
                endDate = dto.endDate ?: (start + DEFAULT_DURATION_MS),
                venueName = dto.venueName.orEmpty().ifBlank { title },
                address = dto.address.orEmpty(),
                latitude = dto.latitude,
                longitude = dto.longitude,
                imageUrl = dto.imageUrl?.ifBlank { null },
                sourceUrl = null,
                organizerName = dto.organizerName?.ifBlank { null }
            )
        }

        AppLogger.i(tag, "Fetched ${events.size} events from the EventFinder API")
        return events
    }

    private companion object {
        /** Assumed event length when the API returns no end date. */
        const val DEFAULT_DURATION_MS = 3 * 60 * 60 * 1000L
    }
}
