package com.eventfinder.app.data.remote

import com.eventfinder.app.data.remote.dto.EventFinderEventDto
import com.eventfinder.app.data.remote.dto.EventFinderEventRequest
import com.eventfinder.app.data.remote.dto.EventFinderResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The EventFinder South Africa REST API — our own ASP.NET Core service, which
 * lives in `/api` in this repository (see section 5 of the Planning and Design
 * document).
 *
 * Every endpoint returns the same [EventFinderResponse] envelope so outcomes can
 * be handled consistently.
 *
 * The base URL is configured per build type in `app/build.gradle.kts`
 * (`API_BASE_URL`), which points debug builds at the emulator loopback address.
 */
interface EventFinderApi {

    /** Liveness probe, used by the Settings screen to show connection status. */
    @GET("api/health")
    suspend fun health(): EventFinderResponse<HealthDto>

    /** Lists events, optionally filtered by category or free-text query. */
    @GET("api/events")
    suspend fun getEvents(
        @Query("category") category: String? = null,
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 100
    ): EventFinderResponse<List<EventFinderEventDto>>

    @GET("api/events/{id}")
    suspend fun getEvent(@Path("id") id: String): EventFinderResponse<EventFinderEventDto>

    @POST("api/events")
    suspend fun createEvent(
        @Body request: EventFinderEventRequest
    ): EventFinderResponse<EventFinderEventDto>

    @PUT("api/events/{id}")
    suspend fun updateEvent(
        @Path("id") id: String,
        @Body request: EventFinderEventRequest
    ): EventFinderResponse<EventFinderEventDto>

    @DELETE("api/events/{id}")
    suspend fun deleteEvent(@Path("id") id: String): EventFinderResponse<String>
}

/** Payload of `GET /api/health`. */
data class HealthDto(
    val status: String?,
    val utc: String?
)
