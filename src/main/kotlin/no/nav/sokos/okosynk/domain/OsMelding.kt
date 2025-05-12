package no.nav.sokos.okosynk.domain

import java.time.LocalDate

import no.nav.sokos.okosynk.util.CopyBookField
import no.nav.sokos.okosynk.util.CopyBookType
import no.nav.sokos.okosynk.util.Utils.toNorwegianDate

data class OsMelding(
    @CopyBookField(startIndex = 0, endIndex = 11, type = CopyBookType.GJERLDER_ID)
    override val gjelderId: String,
    @CopyBookField(startIndex = 11, endIndex = 21, type = CopyBookType.STRING)
    val beregningsid: String,
    @CopyBookField(startIndex = 21, endIndex = 31, type = CopyBookType.DATE)
    val beregningsdato: LocalDate,
    @CopyBookField(startIndex = 31, endIndex = 41, type = CopyBookType.DATE)
    override val datoForStatus: LocalDate,
    @CopyBookField(startIndex = 41, endIndex = 45, type = CopyBookType.STRING)
    override val venteStatus: String,
    @CopyBookField(startIndex = 45, endIndex = 53, type = CopyBookType.STRING)
    override val brukerId: String,
    @CopyBookField(startIndex = 53, endIndex = 63, type = CopyBookType.DATE)
    val fomPeriode: LocalDate,
    @CopyBookField(startIndex = 63, endIndex = 73, type = CopyBookType.DATE)
    val tomPeriode: LocalDate,
    @CopyBookField(startIndex = 73, endIndex = 86, type = CopyBookType.SPECIAL)
    override val totalNettoBelop: Double,
    @CopyBookField(startIndex = 86, endIndex = 87, type = CopyBookType.STRING)
    val flaggFeilKonto: String,
    @CopyBookField(startIndex = 87, endIndex = 99, type = CopyBookType.STRING)
    override val behandlendeEnhet: String,
    @CopyBookField(startIndex = 99, endIndex = 108, type = CopyBookType.STRING)
    val faggruppe: String,
    @CopyBookField(startIndex = 108, endIndex = 119, type = CopyBookType.STRING)
    val utbetalesTilId: String,
    @CopyBookField(startIndex = 119, endIndex = 131, type = CopyBookType.STRING)
    val etteroppgjor: String,
) : Melding {
    override fun sammenligningsDato(): LocalDate {
        return beregningsdato
    }

    override fun ruleKey(): String {
        return "${faggruppe}_$behandlendeEnhet"
    }

    fun beskrivelseInfo(
        totalNettoBelop: Double,
        minFomPeriode: LocalDate,
        maxTomPeriode: LocalDate,
    ): String {
        return listOf(
            venteStatus,
            "${totalNettoBelop.toInt()}kr",
            "beregningsdato/id:${beregningsdato.toNorwegianDate()}/$beregningsid",
            "periode:${minFomPeriode.toNorwegianDate()}-${maxTomPeriode.toNorwegianDate()}",
            "feilkonto:$flaggFeilKonto",
            "statusdato:${datoForStatus.toNorwegianDate()}",
            etteroppgjor,
            "UtbTil:$utbetalesTilId",
            brukerId,
        ).joinToString(FELTSEPARATOR).trim()
    }
}
