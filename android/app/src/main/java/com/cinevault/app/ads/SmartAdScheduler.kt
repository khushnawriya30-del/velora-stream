package com.cinevault.app.ads

/**
 * Smart Ad Scheduler — calculates when to show ads during playback.
 *
 * Rules:
 * ▸ Movie (2–2.5h): pre-roll + mid-rolls every 30 min (≥4 total)
 * ▸ Episode (≥30 min): pre-roll + 2 mid-rolls (interval = duration / 3)
 * ▸ Short episode (<30 min): pre-roll + 1 mid-roll at mid-point
 * ▸ Premium users: no ads at all (checked externally)
 */
object SmartAdScheduler {

    data class AdSchedule(
        /** Show a pre-roll ad when playback starts (Watch Now click). */
        val preRoll: Boolean = true,
        /** Playback positions (in ms) at which mid-roll ads should fire. */
        val midRollTimesMs: List<Long> = emptyList(),
    ) {
        val totalAds: Int get() = (if (preRoll) 1 else 0) + midRollTimesMs.size
    }

    private const val THIRTY_MIN_MS = 30L * 60 * 1000

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

    // ── Movie: mid-roll every 30 min ─────────────────────────────────────────

    private fun calculateMovieSchedule(durationMs: Long): AdSchedule {
        val durationMinutes = durationMs / 60_000.0

        // Number of mid-roll ads = one every 30 minutes (excluding the first 30-min mark as the first mid-roll)
        // For a 120-min movie: ads at 30, 60, 90 → 3 mid-rolls + 1 pre-roll = 4 total
        // For a 150-min movie: ads at 30, 60, 90, 120 → 4 mid-rolls + 1 pre-roll = 5 total
        val midRollCount = ((durationMinutes / 30.0).toInt()).coerceAtLeast(1)

        // Don't show ads in the last 2 minutes
        val safeEnd = durationMs - 2 * 60 * 1000

        val midRollTimes = (1..midRollCount)
            .map { i -> i * THIRTY_MIN_MS }
            .filter { it in 1..safeEnd }

        return AdSchedule(preRoll = true, midRollTimesMs = midRollTimes)
    }

    // ── Episode: dynamic intervals ───────────────────────────────────────────

    private fun calculateEpisodeSchedule(durationMs: Long): AdSchedule {
        val durationMinutes = durationMs / 60_000.0

        // Short episode (< 30 min): pre-roll + 1 mid-roll at ~midpoint
        if (durationMinutes < 30) {
            val midPoint = durationMs / 2
            return AdSchedule(preRoll = true, midRollTimesMs = listOf(midPoint))
        }

        // Regular episode (≥ 30 min): pre-roll + 2 mid-rolls
        // Dynamic interval = duration / 3
        val interval = durationMs / 3
        val safeEnd = durationMs - 2 * 60 * 1000

        val midRollTimes = listOf(interval, interval * 2).filter { it in 1..safeEnd }

        return AdSchedule(preRoll = true, midRollTimesMs = midRollTimes)
    }
}
