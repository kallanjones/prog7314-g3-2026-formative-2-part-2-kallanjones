package com.eventfinder.app.data.remote

import com.eventfinder.app.data.local.GeocodeCacheDao
import com.eventfinder.app.data.local.GeocodeCacheEntity
import com.eventfinder.app.utils.AppLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * Resolves free-text location strings (e.g. "Cape Town", "Sandton, Johannesburg")
 * into latitude/longitude coordinates using the Open-Meteo geocoding API.
 *
 * Results are cached in memory so repeated lookups for the same location string
 * (common when multiple events share a venue city) are instant, and persisted
 * in Room (via [GeocodeCacheDao]) so the cache survives process restarts and a
 * multi-event feed does not re-issue Open-Meteo requests for the same places.
 *
 * If geocoding fails or the location string is blank, null is returned and
 * the caller should handle the missing coordinates.
 */
class EventGeocoder(
    private val geocodingApi: OpenMeteoGeocodingApi,
    private val geocodeCacheDao: GeocodeCacheDao? = null
) {
    private val tag = "EventGeocoder"
    private val cache = ConcurrentHashMap<String, Pair<Double, Double>>()
    private val failedQueries = ConcurrentHashMap.newKeySet<String>()

    /**
     * Address parts that never identify a place: province abbreviations and
     * "to be announced" placeholders used by the event feeds.
     */
    private val ignoredParts = setOf(
        "gp", "wc", "kzn", "ec", "mp", "lp", "nw", "nc", "fs",
        "tba", "tbc", "n/a", "none", "various"
    )

    /**
     * Attempts to resolve [locationName] to coordinates.
     *
     * The query is sanitised and appended with ", South Africa" if not already
     * present, to avoid ambiguous matches to other countries.
     *
     * @return lat/lng pair if found, or null if the location cannot be resolved.
     */
    suspend fun geocode(locationName: String): Pair<Double, Double>? {
        val cacheKey = locationName.trim().lowercase()
        if (cacheKey.isBlank()) return null

        cache[cacheKey]?.let { return it }
        if (failedQueries.contains(cacheKey)) return null

        geocodeCacheDao?.findByKey(cacheKey)?.let { entry ->
            val coords = entry.latitude to entry.longitude
            cache[cacheKey] = coords
            return coords
        }

        // Feed addresses arrive as "VENUE, SUBURB, TOWN, PROVINCE". Open-Meteo
        // matches place names only, so the full string never resolves; try the
        // individual parts instead, starting from the town end.
        for (candidate in placeCandidates(locationName)) {
            val coords = lookupSouthAfrican(candidate) ?: continue

            cache[cacheKey] = coords
            failedQueries.remove(cacheKey)
            geocodeCacheDao?.upsert(
                GeocodeCacheEntity(
                    locationKey = cacheKey,
                    latitude = coords.first,
                    longitude = coords.second,
                    createdAt = System.currentTimeMillis()
                )
            )
            AppLogger.d(tag, "Geocoded '$locationName' via '$candidate' -> ${coords.first}, ${coords.second}")
            return coords
        }

        failedQueries.add(cacheKey)
        AppLogger.d(tag, "No South African geocoding result for '$locationName'")
        return null
    }

    /**
     * Queries Open-Meteo for [query] and returns the first explicitly South
     * African match. Without the country check a query for "Caledon" resolves
     * to Canada, placing the event in the wrong country.
     */
    private suspend fun lookupSouthAfrican(query: String): Pair<Double, Double>? = try {
        val response = geocodingApi.geocode(name = query, count = 5)

        val result = response.results?.firstOrNull { res ->
            res.countryCode?.equals("ZA", ignoreCase = true) == true ||
                res.country?.equals("South Africa", ignoreCase = true) == true
        }

        if (result?.latitude != null && result.longitude != null) {
            result.latitude to result.longitude
        } else {
            null
        }
    } catch (e: Exception) {
        AppLogger.w(tag, "Geocoding failed for '$query': ${e.message}")
        null
    }

    /**
     * Splits a free-text location into place-name candidates, ordered from the
     * broadest part (usually the town, written last) to the most specific.
     * Province codes and placeholders are dropped, and the list is capped so a
     * single unresolvable address cannot spend many Open-Meteo requests.
     */
    internal fun placeCandidates(raw: String): List<String> =
        raw.split(',')
            .map { part ->
                part.replace(Regex("\\s+"), " ")
                    .trim()
                    .trim('.', '…', '-', '–')
                    .trim()
            }
            .filter { it.length > 2 && it.lowercase() !in ignoredParts }
            .reversed()
            .distinctBy { it.lowercase() }
            .take(MAX_LOOKUPS_PER_LOCATION)

    fun clearCache() {
        cache.clear()
        failedQueries.clear()
    }

    private companion object {
        /** Caps the Open-Meteo requests spent on one address. */
        const val MAX_LOOKUPS_PER_LOCATION = 3
    }
}
