package com.cinevault.app.ads

/**
 * Smart Ad Scheduler — calculates when to show mid-roll ads during playback.
 *
 * Rules (non-premium only):
 * ▸ Movie (2+ hours): pre-roll + mid-roll every 30 min → minimum 4 total ads
 * ▸ Episode (40–60 min): pre-roll + ads at 15 min and 30 min → 3 total
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

    private const val FIFTEEN_MIN_MS = 15L * 60 * 1000
    private const val THIRTY_MIN_MS = 30L * 60 * 1000
    private const val FORTY_MIN_MS = 40L * 60 * 1000

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

    // ── Movie: mid-roll every 30 min, minimum 4 total ads ──────────────────

    private fun calculateMovieSchedule(durationMs: Long): AdSchedule {
        // Don't show ads in the last 2 minutes
        val safeEnd = durationMs - 2 * 60 * 1000

        // Generate mid-rolls every 30 minutes
        val midRollTimes = mutableListOf<Long>()
        var time = THIRTY_MIN_MS
        while (time < safeEnd) {
            midRollTimes.add(time)
            time += THIRTY_MIN_MS
        }

        // Ensure minimum 3 mid-rolls (+ 1 pre-roll = 4 total) for 2h+ movies
        // If not enough natural 30-min intervals, add extra at equal spacing
        if (midRollTimes.size < 3 && durationMs >= 2 * 60 * 60 * 1000) {
            val interval = durationMs / 4
            midRollTimes.clear()
            for (i in 1..3) {
                val t = interval * i
                if (t < safeEnd) midRollTimes.add(t)
            }
        }

        return AdSchedule(preRoll = true, midRollTimesMs = midRollTimes)
    }

    // ── Episode: 15 min & 30 min, or midpoint for short ────────────────────

    private fun calculateEpisodeSchedule(durationMs: Long): AdSchedule {
        val safeEnd = durationMs - 2 * 60 * 1000

        // Short episode (< 40 min): pre-roll + 1 mid-roll at midpoint
        if (durationMs < FORTY_MIN_MS) {
            val midPoint = durationMs / 2
            return AdSchedule(
                preRoll = true,
                midRollTimesMs = if (midPoint in 1..safeEnd) listOf(midPoint) else emptyList(),
            )
        }

        // Regular episode (40–60 min): pre-roll + ads at 15 min and 30 min
        val midRollTimes = listOf(FIFTEEN_MIN_MS, THIRTY_MIN_MS).filter { it in 1..safeEnd }

        return AdSchedule(preRoll = true, midRollTimesMs = midRollTimes)
    }
}
