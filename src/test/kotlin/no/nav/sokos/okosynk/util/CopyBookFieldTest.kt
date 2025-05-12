package no.nav.sokos.okosynk.util

import java.time.LocalDate

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class CopyBookFieldTest : FunSpec({

    data class TestDataClass(
        @CopyBookField(name = "integerField", startIndex = 0, endIndex = 5, type = CopyBookType.INTEGER)
        val integerField: Int?,
        @CopyBookField(name = "stringField", startIndex = 5, endIndex = 15, type = CopyBookType.STRING)
        val stringField: String,
        @CopyBookField(name = "dateField", startIndex = 15, endIndex = 25, type = CopyBookType.DATE)
        val dateField: LocalDate?,
        @CopyBookField(name = "decimalField", startIndex = 25, endIndex = 34, type = CopyBookType.DECIMAL)
        val decimalField: Double?,
        @CopyBookField(name = "specialField", startIndex = 34, endIndex = 42, type = CopyBookType.SPECIAL)
        val specialField: Double?,
        @CopyBookField(name = "organizationField", startIndex = 42, endIndex = 53, type = CopyBookType.GJERLDER_ID)
        val organizationField: String,
        @CopyBookField(name = "gjelderIdField", startIndex = 53, endIndex = 64, type = CopyBookType.GJERLDER_ID)
        val gjelderIdField: String,
    )

    test("Test all CopyBookType fields") {
        val input = "12345HelloWorld2022-01-01123456789042000æ000GjelderId01024728764"

        val result = input.toDataClass<TestDataClass>()

        result.integerField shouldBe 12345
        result.stringField shouldBe "HelloWorld"
        result.dateField shouldBe LocalDate.of(2022, 1, 1)
        result.decimalField shouldBe 1234567.89
        result.specialField shouldBe 0.0
        result.organizationField shouldBe "GjelderId"
        result.gjelderIdField shouldBe "01024728764"
    }
})
