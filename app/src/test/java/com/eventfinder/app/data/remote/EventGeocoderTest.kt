package com.eventfinder.app.data.remote

import com.eventfinder.app.data.remote.dto.OmGeocodingResponse
import com.eventfinder.app.data.remote.dto.OmGeocodingResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EventGeocoderTest {

    private class FakeGeocodingApi(
        private val results: List<OmGeocodingResult>
    ) : OpenMeteoGeocodingApi {
        override suspend fun geocode(
            name: String,
            count: Int,
            language: String,
            format: String
        ): OmGeocodingResponse = OmGeocodingResponse(results)

        override suspend fun elevation(
            latitude: String,
            longitude: String
        ): com.eventfinder.app.data.remote.dto.OmElevationResponse =
            com.eventfinder.app.data.remote.dto.OmElevationResponse(elevation = emptyList())
    }

    private fun geocoder(vararg results: OmGeocodingResult): EventGeocoder =
        EventGeocoder(FakeGeocodingApi(results.toList()))

    @Test
    fun `returns null when the api only returns a non South African result`() = runTest {
        val coder = geocoder(
            OmGeocodingResult(
                name = "Kigali", latitude = -1.94, longitude = 30.06,
                country = "Rwanda", countryCode = "RW"
            )
        )
        assertNull(coder.geocode("Kigali"))
    }

    @Test
    fun `picks the South African result from mixed results`() = runTest {
        val coder = geocoder(
            OmGeocodingResult(
                name = "Cape Girardeau", latitude = 37.30, longitude = -89.51,
                country = "United States", countryCode = "US"
            ),
            OmGeocodingResult(
                name = "Cape Town", latitude = -33.92, longitude = 18.42,
                country = "South Africa", countryCode = "ZA"
            )
        )
        val coords = coder.geocode("Cape Town")
        assertNotNull(coords)
        assertEquals(-33.92, coords!!.first, 0.001)
        assertEquals(18.42, coords.second, 0.001)
    }

    @Test
    fun `accepts a result recognised via the full country name`() = runTest {
        val coder = geocoder(
            OmGeocodingResult(
                name = "Durban", latitude = -29.86, longitude = 31.03,
                country = "South Africa", countryCode = null
            )
        )
        val coords = coder.geocode("Durban")
        assertNotNull(coords)
        assertEquals(-29.86, coords!!.first, 0.001)
    }

    @Test
    fun `accepts a result recognised via the country code`() = runTest {
        val coder = geocoder(
            OmGeocodingResult(
                name = "Pretoria", latitude = -25.75, longitude = 28.19,
                country = null, countryCode = "ZA"
            )
        )
        val coords = coder.geocode("Pretoria")
        assertNotNull(coords)
        assertEquals(-25.75, coords!!.first, 0.001)
    }

    @Test
    fun `place candidates put the town before the venue and drop the province code`() {
        val candidates = geocoder().placeCandidates("MAHEM RACEWAY, ZANDFONTEIN, PRETORIA, GP")

        assertEquals(listOf("PRETORIA", "ZANDFONTEIN", "MAHEM RACEWAY"), candidates)
    }

    @Test
    fun `place candidates strip trailing ellipsis left by the feed`() {
        val candidates = geocoder().placeCandidates("MAHEM RACEWAY, PRETORIA……")

        assertEquals(listOf("PRETORIA", "MAHEM RACEWAY"), candidates)
    }

    @Test
    fun `place candidates drop placeholder locations`() {
        assertEquals(emptyList<String>(), geocoder().placeCandidates("TBA, KZN"))
    }

    @Test
    fun `geocodes an address by falling back to its town`() = runTest {
        // The API only knows the town, not the raceway, mirroring Open-Meteo.
        val coder = geocoder(
            OmGeocodingResult(
                name = "Vereeniging", latitude = -26.67, longitude = 27.93,
                country = "South Africa", countryCode = "ZA"
            )
        )

        val coords = coder.geocode("ULTIMATE OUTLAWS, LEEUWKUIL, VEREENIGING")

        assertNotNull(coords)
        assertEquals(-26.67, coords!!.first, 0.001)
    }
}