package com.ttapp.domain

import com.ttapp.data.TimeIntervalEntity
import com.ttapp.ui.perDay
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DaySplitTest {
    @Test fun springClockChangeUsesActualLengthOfLocalDay() {
        val zone = ZoneId.of("Europe/Paris")
        val day = LocalDate.of(2026, 3, 29)
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atTime(1, 0).atZone(zone).toInstant().toEpochMilli()
        val interval = TimeIntervalEntity(
            taskId = 1, sessionId = 1, startedAt = start,
            endedAt = end, durationMs = end - start
        )
        val days = perDay(listOf(interval), zone)
        assertEquals(23 * 3_600_000L, days[day])
        assertEquals(3_600_000L, days[day.plusDays(1)])
    }

    @Test fun crossingMidnightAttributesTimeToBothDays() {
        val zone = ZoneId.of("Europe/Paris")
        val start = LocalDate.of(2026, 11, 18).atTime(23, 50)
            .atZone(zone).toInstant().toEpochMilli()
        val interval = TimeIntervalEntity(
            taskId = 1, sessionId = 1, startedAt = start,
            endedAt = start + 30 * 60_000,
            durationMs = 30 * 60_000
        )
        val days = perDay(listOf(interval), zone)
        assertEquals(10 * 60_000L, days[LocalDate.of(2026, 11, 18)])
        assertEquals(20 * 60_000L, days[LocalDate.of(2026, 11, 19)])
    }
}
