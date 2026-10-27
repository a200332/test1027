package com.dailybrief.app.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

class DateUtilsTest {

    private lateinit var previousZone: TimeZone

    @Before
    fun setUp() {
        previousZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(previousZone)
    }

    @Test
    fun `parses rfc 1123 dates with named and numeric zones`() {
        assertEquals(
            Instant.parse("2026-10-27T04:50:39Z").toEpochMilli(),
            DateUtils.parseFeedDate("Tue, 27 Oct 2026 04:50:39 GMT")
        )
        assertEquals(
            Instant.parse("2026-10-27T03:53:59Z").toEpochMilli(),
            DateUtils.parseFeedDate("Tue, 27 Oct 2026 11:53:59 +0800")
        )
        assertEquals(
            Instant.parse("2026-10-27T02:56:53Z").toEpochMilli(),
            DateUtils.parseFeedDate("2026-10-27 10:56:53  +0800")
        )
    }

    @Test
    fun `parses iso dates and rejects invalid values`() {
        assertEquals(
            Instant.parse("2026-10-26T08:00:00Z").toEpochMilli(),
            DateUtils.parseFeedDate("2026-10-26T08:00:00Z")
        )
        assertNull(DateUtils.parseFeedDate("not a date"))
        assertNull(DateUtils.parseFeedDate(null))
    }

    @Test
    fun `formats chinese date labels and detail time`() {
        val epoch = Instant.parse("2026-10-27T03:00:00Z").toEpochMilli()
        assertEquals("10月27日 星期二", DateUtils.todayLabel(epoch))
        assertEquals("2026年10月27日 11:00", DateUtils.detailTime(epoch))
        assertEquals("11:00", DateUtils.shortClock(epoch))
    }
}
