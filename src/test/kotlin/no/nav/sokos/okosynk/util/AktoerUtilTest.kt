package no.nav.sokos.okosynk.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.okosynk.util.AktoerUtil.isBnr
import no.nav.sokos.okosynk.util.AktoerUtil.isDnr

class AktoerUtilTest : FunSpec({
    test("isDnr should return true for valid Dnr") {
        "40000000000".isDnr() shouldBe true
    }

    test("isDnr should return false for invalid Dnr") {
        "30000000000".isDnr() shouldBe false
        "4000000000".isDnr() shouldBe false
    }

    test("isBnr should return true for valid Bnr") {
        "12302100000".isBnr() shouldBe true
    }

    test("isBnr should return false for invalid Bnr") {
        "12301100000".isBnr() shouldBe false
        "12300000000".isBnr() shouldBe false
    }
})
