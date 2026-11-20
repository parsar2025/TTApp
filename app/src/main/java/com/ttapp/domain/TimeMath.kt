package com.ttapp.domain

import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

/** SystemClock.elapsedRealtime, unlike wall time, survives clock/timezone edits within a boot. */
data class ClockSample(val wallMs: Long, val elapsedMs: Long, val bootId: Int)
data class TimeAnchor(val wallMs: Long, val elapsedMs: Long, val bootId: Int)

object TimeMath {
    fun elapsedMs(anchor: TimeAnchor, now: ClockSample): Long {
        val sameBoot = when {
            anchor.bootId >= 0 && now.bootId >= 0 -> anchor.bootId == now.bootId
            else -> {
                // Fallback for vendors that hide BOOT_COUNT: compare inferred boot epochs.
                abs((anchor.wallMs - anchor.elapsedMs) - (now.wallMs - now.elapsedMs)) < 120_000L
            }
        }
        val delta = if (sameBoot && now.elapsedMs >= anchor.elapsedMs)
            now.elapsedMs - anchor.elapsedMs
        else now.wallMs - anchor.wallMs // reboot: powered-off time is unobservable
        return max(0L, delta)
    }

    fun format(ms: Long): String {
        val seconds = max(0L, ms) / 1000L
        return String.format(Locale.US, "%02d:%02d:%02d",
            seconds / 3600L, (seconds / 60L) % 60L, seconds % 60L)
    }

    /** Total measured time in an interval, never negative. */
    fun safeDuration(startMs: Long, endMs: Long): Long = max(0L, endMs - startMs)
}
