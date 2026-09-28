package com.example.util

object GujaratiVoiceParser {

    private val GUJARATI_DIGIT_MAP = mapOf(
        '૦' to '0',
        '૧' to '1',
        '૨' to '2',
        '૩' to '3',
        '૪' to '4',
        '૫' to '5',
        '૬' to '6',
        '૭' to '7',
        '૮' to '8',
        '૯' to '9'
    )

    private val GUJARATI_WORD_TO_NUMBER = mapOf(
        "શૂન્ય" to 0L, "ઝીરો" to 0L,
        "એક" to 1L, "બે" to 2L, "ત્રણ" to 3L, "ચાર" to 4L, "પાંચ" to 5L,
        "છ" to 6L, "સાત" to 7L, "આઠ" to 8L, "નવ" to 9L, "દસ" to 10L,
        "અગિયાર" to 11L, "બાર" to 12L, "તેર" to 13L, "ચૌદ" to 14L, "પંદર" to 15L,
        "સોળ" to 16L, "સત્તર" to 17L, "અઢાર" to 18L, "ઓગણીસ" to 19L, "વીસ" to 20L,
        "પચીસ" to 25L, "ત્રીસ" to 30L, "પાંત્રીસ" to 35L, "ચાલીસ" to 40L, "પિસ્તાલીસ" to 45L,
        "પચાસ" to 50L, "પંચાવન" to 55L, "સાઠ" to 60L, "પાંસઠ" to 65L, "સિત્તેર" to 70L,
        "પંચોતેર" to 75L, "એંસી" to 80L, "પંચાસી" to 85L, "નેવું" to 90L, "પંચાણું" to 95L,
        "સો" to 100L, "એકસો" to 100L, "બસો" to 200L, "ત્રણસો" to 300L, "ચારસો" to 400L, "પાંચસો" to 500L
    )

    /**
     * Parses spoken text in Gujarati or English into a numeric Long amount.
     * Examples:
     * "૫૦૦૦" -> 5000
     * "50000 રૂપિયા" -> 50000
     * "પચાસ હજાર" -> 50000
     * "બે લાખ" -> 200000
     * "10 હજાર" -> 10000
     * "દોઢ લાખ" -> 150000
     * "અઢી હજાર" -> 2500
     */
    fun parseSpokenAmount(spokenText: String): Long {
        if (spokenText.isBlank()) return 0L

        // Replace Gujarati numerals with ASCII digits
        var normalized = spokenText.lowercase().trim()
        val sb = StringBuilder()
        for (ch in normalized) {
            sb.append(GUJARATI_DIGIT_MAP[ch] ?: ch)
        }
        normalized = sb.toString()

        // Remove common currency words
        normalized = normalized.replace("રૂપિયા", "")
            .replace("રૂ.", "")
            .replace("રૂ", "")
            .replace("rs", "")
            .replace("inr", "")
            .trim()

        // Check if it's purely digits
        val digitsOnly = normalized.filter { it.isDigit() }
        if (digitsOnly.isNotEmpty() && !normalized.contains("હજાર") && !normalized.contains("લાખ") && !normalized.contains("કરોડ") && !normalized.contains("સો")) {
            return digitsOnly.toLongOrNull() ?: 0L
        }

        // Multipliers
        var multiplier = 1L
        if (normalized.contains("કરોડ")) {
            multiplier = 10000000L
            normalized = normalized.replace("કરોડ", "").trim()
        } else if (normalized.contains("લાખ") || normalized.contains("lakh")) {
            multiplier = 100000L
            normalized = normalized.replace("લાખ", "").replace("lakh", "").trim()
        } else if (normalized.contains("હજાર") || normalized.contains("thousand")) {
            multiplier = 1000L
            normalized = normalized.replace("હજાર", "").replace("thousand", "").trim()
        } else if (normalized.contains("સો") || normalized.contains("hundred")) {
            multiplier = 100L
            normalized = normalized.replace("સો", "").replace("hundred", "").trim()
        }

        // Special Gujarati fractional phrases
        if (normalized.contains("દોઢ")) {
            return (1.5 * multiplier).toLong()
        }
        if (normalized.contains("અઢી")) {
            return (2.5 * multiplier).toLong()
        }

        // Check remaining digits
        val leadDigits = normalized.filter { it.isDigit() }
        if (leadDigits.isNotEmpty()) {
            val base = leadDigits.toLongOrNull() ?: 1L
            return base * multiplier
        }

        // Check word map
        val words = normalized.split("\\s+".toRegex())
        var wordSum = 0L
        for (w in words) {
            val num = GUJARATI_WORD_TO_NUMBER[w]
            if (num != null) {
                wordSum += num
            }
        }

        if (wordSum > 0) {
            return wordSum * multiplier
        }

        return digitsOnly.toLongOrNull() ?: 0L
    }
}
