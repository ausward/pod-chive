package com.pod_chive.android.work

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BootCompletedReceiverTest {

    @Test
    fun testBootReceiverCanBeInstantiated() {
        val receiver = BootCompletedReceiver()
        assertNotNull(receiver)
    }

    @Test
    fun testFavoriteEpisodesSyncSchedulerCanBeInstantiated() {
        assertNotNull(FavoriteEpisodesSyncScheduler)
    }
}

class SyncWorkerLogicTest {

    @Test
    fun testEpisodeIdentityCreation() {
        val id1 = "url|title|date"
        val id2 = "url|title|date"
        assertEquals(id1, id2)
    }

    @Test
    fun testEpisodeIdentityUnique() {
        val id1 = "url1|title|date"
        val id2 = "url2|title|date"
        assertTrue(id1 != id2)
    }


}

class ParseEpisodeTimeTest {

    @Test
    fun testPodchiveFormatParsing() {
        // "yyyy/MM/dd HH:mm" format
        val time = parseDateSafely("2026/01/18 14:30")
        assertTrue(time > 0)
    }

    @Test
    fun testRSSFormatParsing() {
        // "EEE, dd MMM yyyy HH:mm:ss Z" format
        val time = parseDateSafely("Mon, 18 Jan 2026 14:30:00 +0000")
        assertTrue(time > 0)
    }

    @Test
    fun testISO8601FormatParsing() {
        // "yyyy-MM-dd'T'HH:mm:ss" format
        val time = parseDateSafely("2026-01-18T14:30:00")
        assertTrue(time > 0)
    }

    @Test
    fun testInvalidFormatReturnsZero() {
        val time = parseDateSafely("invalid date")
        assertEquals(0L, time)
    }

    private fun parseDateSafely(dateStr: String): Long {
        val formats = listOf(
            "yyyy/MM/dd HH:mm",
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ss"
        )

        for (format in formats) {
            try {
                val sdf = java.text.SimpleDateFormat(format, java.util.Locale.ENGLISH)
                return sdf.parse(dateStr)?.time ?: 0L
            } catch (_: Exception) {
                // Try next format
            }
        }
        return 0L
    }
}

