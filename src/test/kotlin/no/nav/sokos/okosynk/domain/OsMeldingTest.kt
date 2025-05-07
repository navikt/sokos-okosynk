package no.nav.sokos.okosynk.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.okosynk.util.Utils.readFromResource
import no.nav.sokos.okosynk.util.toDataClass

class OsMeldingTest : FunSpec({

    test("Les OS.INPUT og mapper til OsMelding") {
        val osInput = "sftp/OS.INPUT".readFromResource()
        val osMeldingList = osInput.split("\n").map { it.toDataClass<OsMelding>() }
        osMeldingList.size shouldBe 20

        val osMelding = osMeldingList.first()
        osMelding.gjelderId shouldBe "01037324511"
        osMelding.beregningsid shouldBe "134373036"
        osMelding.beregningsdato.toString() shouldBe "2013-06-10"
        osMelding.datoForStatus.toString() shouldBe "2013-06-10"
        osMelding.venteStatus shouldBe "FUTB"
        osMelding.brukerId shouldBe "K231B262"
        osMelding.fomPeriode.toString() shouldBe "2012-09-01"
        osMelding.tomPeriode.toString() shouldBe "2012-09-30"
        osMelding.totalNettoBelop shouldBe 0.00
        osMelding.flaggFeilKonto shouldBe "J"
        osMelding.behandlendeEnhet shouldBe "2310"
        osMelding.faggruppe shouldBe "FRIKORT"
        osMelding.utbetalesTilId shouldBe "01037324511"
        osMelding.etteroppgjor shouldBe "ETTEROPPGJØR"
        osMelding.sammenligningsDato() shouldBe osMelding.beregningsdato
        osMelding.ruleKey() shouldBe "${osMelding.faggruppe}_${osMelding.behandlendeEnhet}"
    }
})
