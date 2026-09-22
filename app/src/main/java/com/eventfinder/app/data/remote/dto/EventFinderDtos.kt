package com.eventfinder.app.data.remote.dto

/**
 * The response envelope returned by every EventFinder API endpoint
 * (section 5.1 of the Planning and Design document).
 */
data class EventFinderResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val message: String? = null,
    val errors: List<String>? = null
)

/** An event as returned by `GET /api/events`. */
data class EventFinderEventDto(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val venueName: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    val isPublic: Boolean = true,
    val organizerId: String? = null,
    val organizerName: String? = null,
    val attendeeCount: Int = 0
)

/** Body sent to `POST /api/events` and `PUT /api/events/{id}`. */
data class EventFinderEventRequest(
    val title: String,
    val description: String,
    val category: String,
    val startDate: Long,
    val endDate: Long,
    val venueName: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?,
    val imageUrl: String?,
    val isPublic: Boolean,
    val organizerId: String,
    val organizerName: String,
    val attendeeCount: Int
)
