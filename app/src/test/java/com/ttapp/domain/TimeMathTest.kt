package com.ttapp.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeMathTest {
    @Test fun sameBootUsesMonotonicElapsedRatherThanWallClock() {
        val anchor = TimeAnchor(1_000_000, 200_000, 4)
        val now = ClockSample(300_000, 203_567, 4)
        assertEquals(3_567, TimeMath.elapsedMs(anchor, now))
    }
    @Test fun rebootFallsBackToWallClock() {
        val anchor = TimeAnchor(1_000_000, 200_000, 4)
        val now = ClockSample(1_012_345, 2_000, 5)
        assertEquals(12_345, TimeMath.elapsedMs(anchor, now))
    }
    @Test fun badClockChangeCannotProduceNegativeDuration() {
        val anchor = TimeAnchor(1_000_000, 200_000, 4)
        assertEquals(0, TimeMath.elapsedMs(anchor, ClockSample(500_000, 1_000, 5)))
    }
    @Test fun pauseDurationFormat() {
        assertEquals("00:00:00", TimeMath.format(999))
        assertEquals("01:02:03", TimeMath.format(3_723_000))
        assertEquals("27:00:00", TimeMath.format(97_200_000))
    }
    @Test fun negativeIntervalClamped() {
        assertEquals(0, TimeMath.safeDuration(2000, 1000))
    }
}
