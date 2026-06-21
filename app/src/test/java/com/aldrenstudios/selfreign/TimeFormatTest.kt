package com.aldrenstudios.selfreign

import com.aldrenstudios.selfreign.util.TimeFormat
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class TimeFormatTest {

    @Test
    fun parts_splitsDurationCorrectly() {
        val duration = TimeUnit.DAYS.toMillis(2) +
            TimeUnit.HOURS.toMillis(3) +
            TimeUnit.MINUTES.toMillis(4) +
            TimeUnit.SECONDS.toMillis(5)
        val p = TimeFormat.parts(duration)
        assertEquals(2, p.days)
        assertEquals(3, p.hours)
        assertEquals(4, p.minutes)
        assertEquals(5, p.seconds)
    }

    @Test
    fun parts_negativeIsClampedToZero() {
        val p = TimeFormat.parts(-5000)
        assertEquals(0, p.days)
        assertEquals(0, p.hours)
        assertEquals(0, p.minutes)
        assertEquals(0, p.seconds)
    }

    @Test
    fun shortDuration_picksLargestUnit() {
        assertEquals("2d 3h", TimeFormat.shortDuration(
            TimeUnit.DAYS.toMillis(2) + TimeUnit.HOURS.toMillis(3)
        ))
        assertEquals("0s", TimeFormat.shortDuration(0))
    }
}
