package no.nav.sokos.okosynk.util

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.okosynk.util.IdentUtil.isBnr
import no.nav.sokos.okosynk.util.IdentUtil.isDnr

class IdentUtilTest :
    FunSpec({
        context("isDnr") {
            test("should return true when first character is greater than 3 and length is 11") {
                "41010199999".isDnr() shouldBe true
                "50101099999".isDnr() shouldBe true
                "99999999999".isDnr() shouldBe true
            }

            test("should return false when first character is less than or equal to 3") {
                "01010199999".isDnr() shouldBe false
                "30101099999".isDnr() shouldBe false
            }

            test("should return false when length is not 11") {
                "4101019999".isDnr() shouldBe false
                "410101999999".isDnr() shouldBe false
                "".isDnr() shouldBe false
            }
        }

        context("isBnr") {
            test("should return true when characters at positions 2-3 represent a number between 21 and 32") {
                "00219999999".isBnr() shouldBe true
                "00329999999".isBnr() shouldBe true
                "00259999999".isBnr() shouldBe true
            }

            test("should return false when characters at positions 2-3 represent a number outside 21-32") {
                "00209999999".isBnr() shouldBe false
                "00339999999".isBnr() shouldBe false
                "00009999999".isBnr() shouldBe false
                "00999999999".isBnr() shouldBe false
            }

            test("should return false for inputs that are too short") {
                "01".isBnr() shouldBe false
            }

            test("should throw exception for inputs with non-numeric characters at positions 2-3") {
                shouldThrow<NumberFormatException> {
                    "0AB99999999".isBnr()
                }
            }
        }
    })
