package no.nav.sokos.okosynk.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

import no.nav.sokos.okosynk.util.CopyBookParseUtil.parseDouble

class CopyBookParseUtilTest :
    FunSpec({
        context("parseDouble") {
            data class TestCase(
                val expected: Double,
                val input: String,
            )

            context("should correctly parse copybook string representations to double values") {
                withData(
                    TestCase(-90.00, "000000000900å"),
                    TestCase(-550.00, "000000005500å"),
                    TestCase(0.00, "000000000000æ"),
                    TestCase(110.00, "000000001100æ"),
                    TestCase(56.75, "000000000567E"),
                    TestCase(41498.91, "000000414989A"),
                    TestCase(182298.00, "000001822980æ"),
                    TestCase(150.75, "000000001507E"),
                    TestCase(476.00, "000000004760æ"),
                    TestCase(-4428.00, "000000044280å"),
                    TestCase(8484.01, "000000084840A"),
                    TestCase(2290.02, "000000022900B"),
                    TestCase(2298.03, "000000022980C"),
                    TestCase(693.04, "000000006930D"),
                    TestCase(1966.05, "000000019660E"),
                    TestCase(999.06, "000000009990F"),
                    TestCase(11640.07, "000000116400G"),
                    TestCase(2268.08, "000000022680H"),
                    TestCase(2320.09, "000000023200I"),
                    TestCase(-11283.01, "000000112830J"),
                    TestCase(-1966.02, "000000019660K"),
                    TestCase(-999.03, "000000009990L"),
                    TestCase(-970.04, "000000009700M"),
                    TestCase(-1269.05, "000000012690N"),
                    TestCase(-11858.06, "000000118580O"),
                    TestCase(-1966.07, "000000019660P"),
                    TestCase(-6909.08, "000000069090Q"),
                    TestCase(-999.09, "000000009990R"),
                ) { (expected, input) ->
                    input.parseDouble() shouldBe expected
                }
            }
        }
    })
