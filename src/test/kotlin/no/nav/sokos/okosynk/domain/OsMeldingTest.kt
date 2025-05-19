package no.nav.sokos.okosynk.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.okosynk.util.Utils.readFromResource
import no.nav.sokos.okosynk.util.toDataClass

class OsMeldingTest : FunSpec({

    test("Les OS.INPUT og mapper til OsMelding") {
        val osInput = "sftp/OS.INPUT".readFromResource()
        val osMeldingList = osInput.lines().map { it.toDataClass<OsMelding>() }
        osMeldingList.size shouldBe 20

        val osMelding = osMeldingList.first()
        osMelding.gjelderId shouldBe "10075122948"
        osMelding.beregningsid shouldBe "454028226"
        osMelding.beregningsdato.toString() shouldBe "2022-03-04"
        osMelding.datoForStatus.toString() shouldBe "2022-03-08"
        osMelding.venteStatus shouldBe "AVVE"
        osMelding.brukerId shouldBe "K124096"
        osMelding.fomPeriode.toString() shouldBe "2022-03-01"
        osMelding.tomPeriode.toString() shouldBe "2022-03-31"
        osMelding.totalNettoBelop shouldBe 3093.00
        osMelding.flaggFeilKonto shouldBe ""
        osMelding.behandlendeEnhet shouldBe "4819"
        osMelding.faggruppe shouldBe "PEN"
        osMelding.utbetalesTilId shouldBe "10075122948"
        osMelding.etteroppgjor shouldBe ""
        osMelding.sammenligningsDato() shouldBe osMelding.beregningsdato
        osMelding.ruleKey() shouldBe "${osMelding.faggruppe}_${osMelding.behandlendeEnhet}"
    }
})
