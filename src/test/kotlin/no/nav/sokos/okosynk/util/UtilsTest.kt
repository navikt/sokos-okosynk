package no.nav.sokos.okosynk.util

import java.time.LocalDate
import java.time.LocalDateTime

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty

import no.nav.sokos.okosynk.util.Utils.readFromResource
import no.nav.sokos.okosynk.util.Utils.toISO
import no.nav.sokos.okosynk.util.Utils.toNorwegianDate

class UtilsTest :
    FunSpec({

        test("readFromResource should read the content of a resource file") {
            val resourceName = "sftp/OS.INPUT"

            val content = resourceName.readFromResource()
            content.shouldNotBeEmpty()
        }

        test("toNorwegianDate should format LocalDate to dd.MM.yy") {
            val date = LocalDate.of(2023, 10, 5)
            val formattedDate = date.toNorwegianDate()
            formattedDate shouldBe "05.10.23"
        }

        test("toISO should format LocalDateTime to ISO format yyyy-MM-dd'T'HH:mm:ss") {
            val dateTime = LocalDateTime.of(2023, 10, 5, 14, 30, 0)
            val formattedDateTime = dateTime.toISO()
            formattedDateTime shouldBe "2023-10-05T14:30:00"
        }
    })
