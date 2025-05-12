package no.nav.sokos.okosynk.domain

import java.time.LocalDate

import no.nav.sokos.okosynk.util.CopyBookField
import no.nav.sokos.okosynk.util.CopyBookType
import no.nav.sokos.okosynk.util.Utils.toNorwegianDate

data class UrMelding(
    @CopyBookField(startIndex = 0, endIndex = 11, type = CopyBookType.GJERLDER_ID)
    override val gjelderId: String,
    @CopyBookField(startIndex = 11, endIndex = 23, type = CopyBookType.STRING)
    val gjelderIdType: String,
    @CopyBookField(startIndex = 23, endIndex = 42, type = CopyBookType.DATE)
    override val datoForStatus: LocalDate?,
    @CopyBookField(startIndex = 42, endIndex = 44, type = CopyBookType.STRING)
    override val venteStatus: String,
    @CopyBookField(startIndex = 44, endIndex = 54, type = CopyBookType.STRING)
    override val brukerId: String,
    @CopyBookField(startIndex = 54, endIndex = 69, type = CopyBookType.SPECIAL)
    override val totalNettoBelop: Double,
    @CopyBookField(startIndex = 69, endIndex = 73, type = CopyBookType.STRING)
    override val behandlendeEnhet: String,
    @CopyBookField(startIndex = 73, endIndex = 80, type = CopyBookType.STRING)
    val oppdragsKode: String,
    @CopyBookField(startIndex = 80, endIndex = 85, type = CopyBookType.STRING)
    val kilde: String,
    @CopyBookField(startIndex = 85, endIndex = 95, type = CopyBookType.DATE)
    val datoPostert: LocalDate,
    @CopyBookField(startIndex = 95, endIndex = 104, type = CopyBookType.STRING)
    val bilagsId: String,
    @CopyBookField(startIndex = 104, endIndex = 154, type = CopyBookType.STRING)
    val arsakTekst: String,
    @CopyBookField(startIndex = 154, endIndex = 165, type = CopyBookType.STRING)
    val mottakerId: String,
) : Melding {
    override fun sammenligningsDato(): LocalDate {
        return datoPostert
    }

    override fun ruleKey(): String {
        return "${oppdragsKode}_$behandlendeEnhet"
    }

    fun beskrivelseInfo(totalNettoBelop: Double): String {
        return listOf(
            venteStatus,
            arsakTekst,
            "postert/bilagsnummer:${datoPostert.toNorwegianDate()}/$bilagsId",
            "${totalNettoBelop.toInt()}kr",
            "statusdato:${datoForStatus?.toNorwegianDate()}",
            "UtbTil:$mottakerId",
            brukerId,
        ).joinToString(FELTSEPARATOR)
    }
}
