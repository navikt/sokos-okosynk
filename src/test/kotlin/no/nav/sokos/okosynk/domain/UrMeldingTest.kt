package no.nav.sokos.okosynk.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.okosynk.util.Utils.readFromResource
import no.nav.sokos.okosynk.util.toDataClass

class UrMeldingTest : FunSpec({
    test("Les UR.INPUT og mapper til UrMelding") {
        val urInput = "sftp/UR.INPUT".readFromResource()
        val urMeldingList = urInput.lines().map { it.toDataClass<UrMelding>() }
        urMeldingList.size shouldBe 20

        val urMelding = urMeldingList.first()
        urMelding.gjelderId shouldBe "80000662771"
        urMelding.brukerId shouldBe "K2300462"
        urMelding.gjelderIdType shouldBe "ORGANISASJON"
        urMelding.datoForStatus.toString() shouldBe "2024-12-30"
        urMelding.venteStatus shouldBe "25"
        urMelding.totalNettoBelop shouldBe 4200.00
        urMelding.behandlendeEnhet shouldBe "8020"
        urMelding.oppdragsKode shouldBe "KREDREF"
        urMelding.kilde shouldBe "UR230"
        urMelding.datoPostert.toString() shouldBe "2024-12-30"
        urMelding.bilagsId shouldBe "793627889"
        urMelding.arsakTekst shouldBe "KID ugyldig/mangler"
        urMelding.mottakerId shouldBe "80000662771"
    }
})
