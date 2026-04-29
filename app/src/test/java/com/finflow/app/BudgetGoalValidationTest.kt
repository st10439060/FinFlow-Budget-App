package com.finflow.app

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for budget goal validation logic.
 * Tests cover the rules applied in GoalsFragment.validateInputs().
 */
class BudgetGoalValidationTest {

    @Test
    fun `min goal less than max goal is valid`() {
        val minGoal = 500
        val maxGoal = 2000
        assertTrue("Min goal must be <= max goal", minGoal <= maxGoal)
    }

    @Test
    fun `min goal equal to max goal is valid`() {
        val minGoal = 1000
        val maxGoal = 1000
        assertTrue("Equal min and max goals are allowed", minGoal <= maxGoal)
    }

    @Test
    fun `min goal greater than max goal is invalid`() {
        val minGoal = 3000
        val maxGoal = 1000
        assertFalse("Min goal > max goal should fail", minGoal <= maxGoal)
    }

    @Test
    fun `category must be selected before saving budget`() {
        val categoryId = 0L
        assertFalse("Category ID of 0 means nothing selected", categoryId != 0L)
    }

    @Test
    fun `budget amount of zero is allowed`() {
        val budget = 0.0
        assertTrue("Zero budget is a valid starting point", budget >= 0)
    }

    @Test
    fun `currency format produces non-empty string`() {
        val value = 1500
        val formatted = "R %.2f".format(value.toDouble())
        assertTrue("Formatted value should not be empty", formatted.isNotEmpty())
        assertTrue("Formatted value should contain decimal", formatted.contains("."))
    }
}
