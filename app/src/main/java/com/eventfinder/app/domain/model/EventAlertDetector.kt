package com.eventfinder.app.domain.model

/**
 * Pure diffing logic behind the "new event" and "favourite updated" alerts.
 *
 * Kept free of Android dependencies so it can be unit-tested on the JVM. The
 * repository feeds it the catalogue before and after a sync, plus the user's
 * favourite ids, and an optional clock so tests stay deterministic.
 */
object EventAlertDetector {

    /** Which alerts a sync produced. Both lists only contain future events. */
    data class Alerts(
        val newEvents: List<Event>,
        val updatedFavorites: List<Event>
    ) {
        val isEmpty: Boolean get() = newEvents.isEmpty() && updatedFavorites.isEmpty()
    }

    /** Built-in demo events seeded by SampleEventsProvider. */
    private fun isSample(event: Event): Boolean = event.id.startsWith("sample-")

    /** Events that changed enough to be worth re-notifying about. */
    private fun materiallyChanged(previous: Event, current: Event): Boolean =
        previous.title != current.title ||
            previous.venueName != current.venueName ||
            previous.startDate != current.startDate

    /**
     * @param previous   events that were already in the cache, keyed by id
     * @param current    the freshly synced catalogue
     * @param favoriteIds ids the user has saved
     * @param now        current time (injected for testability)
     */
    fun detect(
        previous: Map<String, Event>,
        current: List<Event>,
        favoriteIds: Set<String>,
        now: Long = System.currentTimeMillis()
    ): Alerts {
        val future = current.filter { it.startDate > now }
        // An event is only "new" if it comes from a source whose events were already
        // in the previous catalogue. Feeds are saved one at a time, so on a fresh
        // install the first feed (or the built-in samples) would otherwise make every
        // later feed's events look new and post a burst of alerts on first launch.
        val knownSources = previous.values
            .filterNot { isSample(it) }
            .mapTo(HashSet()) { it.organizerId }
        val newEvents = future.filter {
            it.id !in previous &&
                it.organizerId in knownSources &&
                !isSample(it) &&
                !it.isCreatedByUser
        }
        val updatedFavorites = future.filter { event ->
            event.id in favoriteIds &&
                previous[event.id]?.let { materiallyChanged(it, event) } == true
        }
        return Alerts(newEvents = newEvents, updatedFavorites = updatedFavorites)
    }

    /** Convenience overload for callers that only have the pre-sync list. */
    fun detect(
        previous: List<Event>,
        current: List<Event>,
        favoriteIds: Set<String>,
        now: Long = System.currentTimeMillis()
    ): Alerts = detect(previous.associateBy { it.id }, current, favoriteIds, now)
}
