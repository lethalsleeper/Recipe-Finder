package com.example.recipefinder.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstructionStepsTest {

    @Test
    fun `splits on line breaks and drops blank lines`() {
        val steps = InstructionSteps.split("Preheat the oven.\r\n\r\nMix the flour.\r\nBake for 20 minutes.")

        assertEquals(listOf("Preheat the oven.", "Mix the flour.", "Bake for 20 minutes."), steps)
    }

    @Test
    fun `removes the source's own step labels so numbering is not duplicated`() {
        val steps = InstructionSteps.split("STEP 1\r\nBoil water.\r\nStep 2: Add pasta.\r\n3. Drain.\r\n4) Serve.")

        assertEquals(listOf("Boil water.", "Add pasta.", "Drain.", "Serve."), steps)
    }

    @Test
    fun `does not strip numbers that are part of the instruction`() {
        val steps = InstructionSteps.split("2.5 kg of potatoes, peeled.\n350F oven for 1 hour.")

        assertEquals(listOf("2.5 kg of potatoes, peeled.", "350F oven for 1 hour."), steps)
    }

    @Test
    fun `text without line breaks stays a single paragraph`() {
        val text = "Mix everything. Bake. Serve warm."

        assertEquals(listOf(text), InstructionSteps.split(text))
    }

    @Test
    fun `null or blank instructions give no steps`() {
        assertTrue(InstructionSteps.split(null).isEmpty())
        assertTrue(InstructionSteps.split("  \r\n ").isEmpty())
    }
}
