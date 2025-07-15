package no.nav.sokos.okosynk.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

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
        }.getOrDefault(0.00).let { "%.2f".format(it).toDouble() }
    }

    fun String.toLocalDate(): LocalDate? =
        runCatching {
            val value =
                this.ifBlank { null }.let {
                    val formatter =
                        DateTimeFormatterBuilder()
                            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                            .appendOptional(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            .toFormatter()
                    this
                        .takeIf { it.contains("T") }
                        ?.let { LocalDateTime.parse(this, formatter).toLocalDate() }
                        ?: LocalDate.parse(this, formatter)
                }
            value
        }.getOrNull()
}
