package com.example.recipefinder.domain.util

/**
 * Splits TheMealDB instructions into steps for numbered display, without rewriting them.
 *
 * TheMealDB separates steps with line breaks (`\r\n`). Some recipes also include their own
 * numbering ("STEP 1", "1.", "Step 2:") which would clash with ours, so those labels are removed.
 * The text of each step is otherwise kept exactly as returned (only trimmed).
 *
 * If the text has no line breaks it cannot be split reliably, and a single-element list is
 * returned so the UI shows it as one paragraph instead of guessing at sentence boundaries.
 */
object InstructionSteps {

    /** A line that is nothing but a step label, e.g. "STEP 1" or "Step 2:". */
    private val standaloneStepLabel = Regex("""^step\s*\d+\s*[:.)\-]?$""", RegexOption.IGNORE_CASE)

    /**
     * A step label at the start of a line, e.g. "Step 3: Mix", "4. Bake" or "5) Serve".
     * A number must be followed by "." or ")" and a space, so "2.5 kg" or "350F" are left alone.
     */
    private val leadingStepLabel = Regex("""^(?:step\s*\d+\s*[:.)\-]?\s*|\d{1,2}[.)]\s+)""", RegexOption.IGNORE_CASE)

    fun split(instructions: String?): List<String> {
        if (instructions.isNullOrBlank()) return emptyList()

        return instructions
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !standaloneStepLabel.matches(it) }
            .map { line -> line.replaceFirst(leadingStepLabel, "").ifBlank { line } }
            .filter { it.isNotBlank() }
    }
}
