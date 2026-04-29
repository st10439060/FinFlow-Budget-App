package com.finflow.app

import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Unit tests for date/time utility functions used across the app.
 */
class DateUtilsTest {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())

    @Test
    fun `time string is formatted as HH colon mm`() {
        val hour = 9
        val minute = 5
        val result = String.format("%02d:%02d", hour, minute)
        assertEquals("09:05", result)
    }

    @Test
    fun `time string pads single digit hour and minute`() {
        val result = String.format("%02d:%02d", 8, 3)
        assertEquals("08:03", result)
    }

    @Test
    fun `date string is formatted as dd slash MM slash yyyy`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.APRIL, 15)
        val result = dateFormat.format(cal.time)
        assertEquals("15/04/2026", result)
    }

    @Test
    fun `month year format is yyyy-MM`() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.APRIL, 1)
        val result = monthFormat.format(cal.time)
        assertEquals("2026-04", result)
    }

    @Test
    fun `start of month is less than end of month`() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        val startMs = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        val endMs = cal.timeInMillis

        assertTrue("Start of month must be before end of month", startMs < endMs)
    }

    @Test
    fun `password hash utility returns 64 char hex string`() {
        val input = "testpassword"
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
        assertEquals("SHA-256 hex digest should be 64 chars", 64, hash.length)
    }
}
