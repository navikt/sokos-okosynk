package no.nav.sokos.okosynk.util

import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe

import no.nav.sokos.okosynk.util.CopyBookParseUtil.parseDouble

class CopyBookParseUtilTest :
    FunSpec({
        context("parseDouble") {
            test("should correctly parse copybook string representations to double values") {
                forAll(
                    row(-90.00, "000000000900å"),
                    row(-550.00, "000000005500å"),
                    row(0.00, "000000000000æ"),
                    row(110.00, "000000001100æ"),
                    row(56.75, "000000000567E"),
                    row(41498.91, "000000414989A"),
                    row(182298.00, "000001822980æ"),
                    row(150.75, "000000001507E"),
                    row(476.00, "000000004760æ"),
                    row(-4428.00, "000000044280å"),
                    row(8484.01, "000000084840A"),
                    row(2290.02, "000000022900B"),
                    row(2298.03, "000000022980C"),
                    row(693.04, "000000006930D"),
                    row(1966.05, "000000019660E"),
                    row(999.06, "000000009990F"),
                    row(11640.07, "000000116400G"),
                    row(2268.08, "000000022680H"),
                    row(2320.09, "000000023200I"),
                    row(-11283.01, "000000112830J"),
                    row(-1966.02, "000000019660K"),
                    row(-999.03, "000000009990L"),
                    row(-970.04, "000000009700M"),
                    row(-1269.05, "000000012690N"),
                    row(-11858.06, "000000118580O"),
                    row(-1966.07, "000000019660P"),
                    row(-6909.08, "000000069090Q"),
                    row(-999.09, "000000009990R"),
                ) { expected, input ->
                    input.parseDouble() shouldBe expected
                }
            }
        }
    })
