package no.nav.sokos.okosynk.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

object CopyBookParseUtil {
    private val KODER_FOR_POSITIVT_FORTEGN: List<Char> = mutableListOf('æ', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I')
    private val KODER_FOR_NEGATIVT_FORTEGN: List<Char> = mutableListOf('å', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R')

    val fortegnMap: Map<Char, Int> =
        buildMap {
            (0..9).forEach { value ->
                put(KODER_FOR_POSITIVT_FORTEGN[value], value)
                put(KODER_FOR_NEGATIVT_FORTEGN[value], value)
            }
        }

    fun String.parseDouble(): Double {
        if (this.isEmpty()) return 0.00

        return runCatching {
            val fortegn = this.last()
            val lastDigitValue =
                fortegnMap[fortegn] ?: let {
                    logger.error("Ugyldig input: $this. Siste tegn angir ingen tallverdi.")
                    throw NumberFormatException("Ugyldig input: $this. Siste tegn angir ingen tallverdi.")
                }
            val integerPart = this.dropLast(2).toIntOrNull() ?: 0
            val decimalPart =
                this
                    .dropLast(1)
                    .lastOrNull()
                    ?.toString()
                    ?.toIntOrNull() ?: 0

            val result = integerPart + (decimalPart * 10 + lastDigitValue) / 100.0

            if (KODER_FOR_NEGATIVT_FORTEGN.contains(fortegn)) {
                -result
            } else {
                result
            }
        }.getOrDefault(0.00).let { BigDecimal(it).setScale(2, RoundingMode.HALF_UP).toDouble() }
    }

    fun String.toLocalDate(): LocalDate? {
        val input = trim()
        if (input.isEmpty()) return null

        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        return runCatching {
            if ('T' in input) {
                LocalDateTime.parse(input, dateTimeFormatter).toLocalDate()
            } else {
                LocalDate.parse(input, dateFormatter)
            }
        }.getOrNull()
    }
}
