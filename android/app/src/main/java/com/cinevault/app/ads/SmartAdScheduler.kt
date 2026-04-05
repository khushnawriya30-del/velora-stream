package com.cinevault.app.ads

/**
 * Smart Ad Scheduler — calculates when to show mid-roll video ads during playback.
 *
 * Rules (non-premium only):
 * ▸ Movie (2–3 hours): pre-roll + 3 mid-rolls → 3–4 total ads
 *   Placement: before start, ~20-25 min, ~40-50 min, near ending
 * ▸ Episode (40–50 min): pre-roll + 1 mid-roll at midpoint → 2 total
 * ▸ Short episode (<40 min): pre-roll + 1 mid-roll at midpoint → 2 total
 * ▸ Resume: always show pre-roll ad again
 * ▸ Premium users: no ads (checked externally)
 */
object SmartAdScheduler {

    data class AdSchedule(
        /** Show a pre-roll ad when playback starts (Watch Now / Resume). */
        val preRoll: Boolean = true,
        /** Playback positions (in ms) at which mid-roll ads should fire. */
        val midRollTimesMs: List<Long> = emptyList(),
    ) {
        val totalAds: Int get() = (if (preRoll) 1 else 0) + midRollTimesMs.size
    }

    private const val TWENTY_MIN_MS = 20L * 60 * 1000
    private const val TWENTY_FIVE_MIN_MS = 25L * 60 * 1000
    private const val FORTY_MIN_MS = 40L * 60 * 1000
    private const val FORTY_FIVE_MIN_MS = 45L * 60 * 1000
    private const val TWO_HOURS_MS = 2L * 60 * 60 * 1000

    /**
     * Calculate an [AdSchedule] for the given content.
     *
     * @param durationMs Total video duration in milliseconds.
     * @param isEpisode  true for web_series / tv_show / anime episodes.
     */
    fun calculateSchedule(durationMs: Long, isEpisode: Boolean): AdSchedule {
        if (durationMs <= 0) return AdSchedule(preRoll = true)

        return if (isEpisode) {
            calculateEpisodeSchedule(durationMs)
        } else {
            calculateMovieSchedule(durationMs)
        }
    }

    // ── Movie (2–3h): pre-roll + mid-rolls at ~22min, ~45min, near end ─────

    private fun calculateMovieSchedule(durationMs: Long): AdSchedule {
        // Don't show ads in the last 5 minutes
        val safeEnd = durationMs - 5 * 60 * 1000

        val midRollTimes = mutableListOf<Long>()

        if (durationMs >= TWO_HOURS_MS) {
            // Long movie (2h+): 3 mid-rolls
            // Ad 1: ~22 minutes in
            val first = TWENTY_MIN_MS + (TWENTY_FIVE_MIN_MS - TWENTY_MIN_MS) / 2  // 22.5 min
            if (first < safeEnd) midRollTimes.add(first)

            // Ad 2: ~45 minutes in
            if (FORTY_FIVE_MIN_MS < safeEnd) midRollTimes.add(FORTY_FIVE_MIN_MS)

            // Ad 3: ~15 minutes before end
            val nearEnd = durationMs - 15 * 60 * 1000
            if (nearEnd > (midRollTimes.lastOrNull() ?: 0) + 10 * 60 * 1000 && nearEnd < safeEnd) {
                midRollTimes.add(nearEnd)
            }
        } else {
            // Shorter movie (< 2h): 2 mid-rolls at 1/3 and 2/3
            val third = durationMs / 3
            val twoThirds = durationMs * 2 / 3
            if (third in 1..safeEnd) midRollTimes.add(third)
            if (twoThirds in 1..safeEnd) midRollTimes.add(twoThirds)
        }

        return AdSchedule(preRoll = true, midRollTimesMs = midRollTimes)
    }

    // ── Episode (40–50 min): pre-roll + 1 mid-roll at midpoint ─────────────

    private fun calculateEpisodeSchedule(durationMs: Long): AdSchedule {
        val safeEnd = durationMs - 2 * 60 * 1000

        // All episodes: pre-roll + 1 mid-roll at midpoint
        val midPoint = durationMs / 2
        return AdSchedule(
            preRoll = true,
            midRollTimesMs = if (midPoint in 1..safeEnd) listOf(midPoint) else emptyList(),
        )
    }
}
