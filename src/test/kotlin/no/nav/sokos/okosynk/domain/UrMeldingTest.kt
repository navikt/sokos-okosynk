package no.nav.sokos.okosynk.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.okosynk.util.Utils.readFromResource
import no.nav.sokos.okosynk.util.toDataClass

class UrMeldingTest : FunSpec({
    test("Les UR.INPUT og mapper til UrMelding") {
        val urInput = "sftp/UR.INPUT".readFromResource()
        val urMeldingList = urInput.split("\n").map { it.toDataClass<UrMelding>() }
        urMeldingList.size shouldBe 20

        val urMelding = urMeldingList.first()
        urMelding.gjelderId shouldBe "00003187051"
        urMelding.brukerId shouldBe ""
        urMelding.gjelderIdType shouldBe "ORGANISASJON"
        urMelding.datoForStatus.toString() shouldBe "2004-01-19"
        urMelding.venteStatus shouldBe "09"
        urMelding.totalNettoBelop shouldBe 8484.00
        urMelding.behandlendeEnhet shouldBe "0318"
        urMelding.oppdragsKode shouldBe "KREDREF"
        urMelding.kilde shouldBe "UR230"
        urMelding.datoPostert.toString() shouldBe "2004-01-15"
        urMelding.bilagsId shouldBe "134553997"
        urMelding.arsakTekst shouldBe "MOTTATT FRA FORSYSTEM"
        urMelding.mottakerId shouldBe "00003187051"
    }
})
