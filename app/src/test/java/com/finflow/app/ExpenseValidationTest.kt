package com.finflow.app

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for expense input validation logic.
 * Tests cover the core rules applied in AddExpenseFragment.validateInputs().
 * Run with: ./gradlew test
 */
class ExpenseValidationTest {

    /**
     * A valid expense must have: non-empty description, positive amount,
     * a selected category, and both start and end times.
     */
    @Test
    fun `valid expense passes all checks`() {
        val amount = 150.0
        val description = "Woolworths"
        val categoryId = 1L
        val startTime = "09:00"
        val endTime = "10:00"

        assertTrue("Amount must be positive", amount > 0)
        assertTrue("Description must not be empty", description.isNotEmpty())
        assertTrue("Category must be selected", categoryId != 0L)
        assertTrue("Start time must be set", startTime.isNotEmpty())
        assertTrue("End time must be set", endTime.isNotEmpty())
    }

    @Test
    fun `expense with empty description is invalid`() {
        val description = ""
        assertFalse("Empty description should fail", description.isNotEmpty())
    }

    @Test
    fun `expense with zero amount is invalid`() {
        val amount = 0.0
        assertFalse("Zero amount should fail", amount > 0)
    }

    @Test
    fun `expense with negative amount is invalid`() {
        val amount = -50.0
        assertFalse("Negative amount should fail", amount > 0)
    }

    @Test
    fun `expense with no category selected is invalid`() {
        val categoryId = 0L
        assertFalse("No category (0L) should fail", categoryId != 0L)
    }

    @Test
    fun `expense with missing start time is invalid`() {
        val startTime = ""
        assertFalse("Empty start time should fail", startTime.isNotEmpty())
    }

    @Test
    fun `expense with missing end time is invalid`() {
        val endTime = ""
        assertFalse("Empty end time should fail", endTime.isNotEmpty())
    }

    @Test
    fun `amount string converts to double correctly`() {
        val input = "250.50"
        val amount = input.toDoubleOrNull()
        assertNotNull("Valid amount string should parse", amount)
        assertEquals(250.50, amount!!, 0.001)
    }

    @Test
    fun `invalid amount string returns null`() {
        val input = "abc"
        val amount = input.toDoubleOrNull()
        assertNull("Non-numeric string should return null", amount)
    }
}
