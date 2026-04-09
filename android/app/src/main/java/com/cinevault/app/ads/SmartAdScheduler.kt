package com.cinevault.app.ads

/**
 * Smart Ad Scheduler — calculates when to show forced video ads during playback.
 *
 * Duration-based mid-roll rules (non-premium only):
 * ▸ 0–10 min   → 1 mid-roll (at midpoint)
 * ▸ 10–20 min  → 2 mid-rolls (evenly spaced)
 * ▸ 20–30 min  → 3 mid-rolls (evenly spaced)
 * ▸ 30–60 min  → every 10 minutes
 * ▸ 60+ min    → every 15 minutes
 * ▸ Pre-roll always plays first
 * ▸ Premium users: no ads (checked externally)
 */
object SmartAdScheduler {

    data class AdSchedule(
        val preRoll: Boolean = true,
        val midRollTimesMs: List<Long> = emptyList(),
    ) {
        val totalAds: Int get() = (if (preRoll) 1 else 0) + midRollTimesMs.size
    }

    private const val ONE_MIN_MS = 60L * 1000
    private const val TEN_MIN_MS = 10L * 60 * 1000
    private const val FIFTEEN_MIN_MS = 15L * 60 * 1000
    private const val TWENTY_MIN_MS = 20L * 60 * 1000
    private const val THIRTY_MIN_MS = 30L * 60 * 1000
    private const val SIXTY_MIN_MS = 60L * 60 * 1000

    fun calculateSchedule(durationMs: Long, isEpisode: Boolean): AdSchedule {
        if (durationMs <= 0) return AdSchedule(preRoll = true)

        // No ads in the last 2 minutes
        val safeEnd = durationMs - 2 * ONE_MIN_MS
        if (safeEnd <= ONE_MIN_MS) return AdSchedule(preRoll = true)

        val midRollTimes = mutableListOf<Long>()

        when {
            // 0–10 min → 1 mid-roll at midpoint
            durationMs <= TEN_MIN_MS -> {
                val mid = durationMs / 2
                if (mid > ONE_MIN_MS && mid < safeEnd) midRollTimes.add(mid)
            }
            // 10–20 min → 2 mid-rolls evenly spaced
            durationMs <= TWENTY_MIN_MS -> {
                val interval = durationMs / 3
                for (i in 1..2) {
                    val t = interval * i
                    if (t > ONE_MIN_MS && t < safeEnd) midRollTimes.add(t)
                }
            }
            // 20–30 min → 3 mid-rolls evenly spaced
            durationMs <= THIRTY_MIN_MS -> {
                val interval = durationMs / 4
                for (i in 1..3) {
                    val t = interval * i
                    if (t > ONE_MIN_MS && t < safeEnd) midRollTimes.add(t)
                }
            }
            // 30–60 min → every 10 minutes
            durationMs <= SIXTY_MIN_MS -> {
                var t = TEN_MIN_MS
                while (t < safeEnd) {
                    midRollTimes.add(t)
                    t += TEN_MIN_MS
                }
            }
            // 60+ min → every 15 minutes
            else -> {
                var t = FIFTEEN_MIN_MS
                while (t < safeEnd) {
                    midRollTimes.add(t)
                    t += FIFTEEN_MIN_MS
                }
            }
        }

        return AdSchedule(preRoll = true, midRollTimesMs = midRollTimes)
    }
}
