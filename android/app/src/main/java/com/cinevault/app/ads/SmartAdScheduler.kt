package com.cinevault.app.ads

/**
 * Smart Ad Scheduler — calculates when to show forced video ads during playback.
 *
 * Rules (non-premium only):
 * ▸ Movie (2+ hours): pre-roll + mid-rolls at 30min, 60min, 90min → 4 total
 * ▸ Episode: pre-roll + mid-rolls at 15min, 30min → 3 total
 * ▸ Short episode (<20 min): pre-roll only → 1 total
 * ▸ Resume / every Watch Now click: always show ad again
 * ▸ Premium users: no ads (checked externally)
 */
object SmartAdScheduler {

    data class AdSchedule(
        val preRoll: Boolean = true,
        val midRollTimesMs: List<Long> = emptyList(),
    ) {
        val totalAds: Int get() = (if (preRoll) 1 else 0) + midRollTimesMs.size
    }

    private const val FIFTEEN_MIN_MS = 15L * 60 * 1000
    private const val THIRTY_MIN_MS = 30L * 60 * 1000
    private const val SIXTY_MIN_MS = 60L * 60 * 1000
    private const val NINETY_MIN_MS = 90L * 60 * 1000
    private const val TWENTY_MIN_MS = 20L * 60 * 1000
    private const val TWO_HOURS_MS = 2L * 60 * 60 * 1000

    fun calculateSchedule(durationMs: Long, isEpisode: Boolean): AdSchedule {
        if (durationMs <= 0) return AdSchedule(preRoll = true)

        return if (isEpisode) {
            calculateEpisodeSchedule(durationMs)
        } else {
            calculateMovieSchedule(durationMs)
        }
    }

    // ── Movie (2h+): pre-roll + mid-rolls at 30, 60, 90 min ───────────────

    private fun calculateMovieSchedule(durationMs: Long): AdSchedule {
        val safeEnd = durationMs - 5 * 60 * 1000 // No ads in last 5 min

        val midRollTimes = mutableListOf<Long>()

        if (durationMs >= TWO_HOURS_MS) {
            // Long movie: ads at 30min, 60min, 90min
            if (THIRTY_MIN_MS < safeEnd) midRollTimes.add(THIRTY_MIN_MS)
            if (SIXTY_MIN_MS < safeEnd) midRollTimes.add(SIXTY_MIN_MS)
            if (NINETY_MIN_MS < safeEnd) midRollTimes.add(NINETY_MIN_MS)
        } else {
            // Shorter movie (< 2h): ads at 30min, 60min
            if (THIRTY_MIN_MS < safeEnd) midRollTimes.add(THIRTY_MIN_MS)
            if (SIXTY_MIN_MS < safeEnd) midRollTimes.add(SIXTY_MIN_MS)
        }

        return AdSchedule(preRoll = true, midRollTimesMs = midRollTimes)
    }

    // ── Episode: pre-roll + mid-rolls at 15, 30 min ───────────────────────

    private fun calculateEpisodeSchedule(durationMs: Long): AdSchedule {
        val safeEnd = durationMs - 2 * 60 * 1000

        // Short episode (< 20 min): pre-roll only
        if (durationMs < TWENTY_MIN_MS) {
            return AdSchedule(preRoll = true, midRollTimesMs = emptyList())
        }

        // Regular episode: ads at 15min and 30min
        val midRollTimes = mutableListOf<Long>()
        if (FIFTEEN_MIN_MS < safeEnd) midRollTimes.add(FIFTEEN_MIN_MS)
        if (THIRTY_MIN_MS < safeEnd) midRollTimes.add(THIRTY_MIN_MS)

        return AdSchedule(preRoll = true, midRollTimesMs = midRollTimes)
    }
}
