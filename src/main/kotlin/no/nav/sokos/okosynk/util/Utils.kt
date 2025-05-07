package no.nav.sokos.okosynk.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Utils {
    fun String.readFromResource(): String {
        val clazz = {}::class.java.classLoader
        val resource = clazz.getResource(this)
        requireNotNull(resource) { "Resource $this not found." }
        return resource.readText()
    }

    fun LocalDate.toNorwegianDate(): String = DateTimeFormatter.ofPattern("dd.MM.yy").format(this)

    fun LocalDateTime.toISO(): String = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").format(this)
}
