package no.nav.sokos.okosynk.process

import kotlinx.coroutines.runBlocking

import mu.KotlinLogging

import no.nav.sokos.okosynk.config.RegelverkConfig
import no.nav.sokos.okosynk.config.TEAM_LOGS_MARKER
import no.nav.sokos.okosynk.domain.BatchType
import no.nav.sokos.okosynk.domain.BatchTypeContext
import no.nav.sokos.okosynk.domain.GjelderIdType
import no.nav.sokos.okosynk.domain.Melding
import no.nav.sokos.okosynk.domain.MeldingKriterier
import no.nav.sokos.okosynk.domain.MeldingOppgave
import no.nav.sokos.okosynk.domain.OsMelding
import no.nav.sokos.okosynk.domain.Regelverk
import no.nav.sokos.okosynk.domain.UrMelding
import no.nav.sokos.okosynk.integration.ENHET_ID_FOR_ANDRE_EKSTERNE
import no.nav.sokos.okosynk.integration.PdlClientService
import no.nav.sokos.okosynk.metrics.Metrics
import no.nav.sokos.okosynk.util.IdentUtil.isDnr

private val logger = KotlinLogging.logger { }

class BehandleMeldingProcessService(
    private val pdlClientService: PdlClientService = PdlClientService(),
) : Chain<List<Melding>, Set<MeldingOppgave>> {
    override fun process(meldingList: List<Melding>): Set<MeldingOppgave> {
        val batchType = BatchTypeContext.get()

        logger.info { "${batchType.oppgaveType} - Start BehandleMeldingProcessService " }
        val regelverkMap = if (batchType == BatchType.OS) RegelverkConfig.regelverkOSMap else RegelverkConfig.regelverkURMap
        val meldingOppgaveSet =
            meldingList
                .sortedByDescending { melding -> melding.sammenligningsDato() }
                .filter { melding -> regelverkMap[melding.ruleKey()] != null }
                .groupBy { melding -> MeldingKriterier(melding) }
                .mapNotNull { (_, value) -> opprettMeldingOppgave(value, regelverkMap) }
                .also { oppgave ->
                    logger.info { "Antall konvertert oppgaver for batchType: $batchType: ${oppgave.size}" }
                    Metrics.counter("konvertert_oppgave_${batchType.opprettetAv}").inc(oppgave.size.toLong())
                }
                .toSet()

        meldingOppgaveSet.filter { it.personIdent?.isDnr() ?: false }
            .forEach { logger.info(marker = TEAM_LOGS_MARKER) { "dnr found in the batch file: ${it.personIdent}" } }

        return meldingOppgaveSet
    }

    private fun hentAktoer(gjelderId: String): String? {
        return runBlocking {
            runCatching {
                pdlClientService.hentIdenter(gjelderId)
            }.getOrElse { exception ->
                logger.error { "Feil ved henting av aktørid, sjekk secureLogger" }
                logger.error(marker = TEAM_LOGS_MARKER, exception) { "Feil ved henting av aktørid for gjelderId: $gjelderId" }
                null
            }
        }
    }

    private fun opprettMeldingOppgave(
        meldingList: List<Melding>,
        regelverkMap: Map<String, Regelverk>,
    ): MeldingOppgave? {
        val melding = meldingList.first()
        val regelverk = regelverkMap[melding.ruleKey()]

        val meldingOppgave =
            MeldingOppgave(
                behandlingstema = regelverk?.behandlingstema,
                behandlingstype = regelverk?.behandlingstype,
                tildeltEnhetsnr = regelverk?.ansvarligEnhetId,
                beskrivelse = melding.beskrivelse(meldingList),
                oppgavetype = BatchTypeContext.get().oppgaveType,
                opprettetAvEnhetsnr = ENHET_ID_FOR_ANDRE_EKSTERNE,
            )

        return when (GjelderIdType.value(melding.gjelderId)) {
            GjelderIdType.AKTORID -> hentAktoer(melding.gjelderId)?.let { aktoerId -> meldingOppgave.copy(aktoerId = aktoerId) }
            GjelderIdType.ORGANISASJON -> meldingOppgave.copy(orgnr = melding.gjelderId)
            GjelderIdType.SAMHANDLER -> meldingOppgave.copy(samhandlernr = melding.gjelderId)
            else -> meldingOppgave.copy(personIdent = melding.gjelderId)
        }
    }
}

fun Melding.beskrivelse(meldingList: List<Melding>): String? {
    return when (this) {
        is OsMelding -> {
            val osMeldingList = meldingList.filterIsInstance<OsMelding>()
            val totalNettoBelop = osMeldingList.sumOf { it.totalNettoBelop }
            val minFomPeriode = osMeldingList.minOf { it.fomPeriode }
            val maxTomPeriode = osMeldingList.maxOf { it.tomPeriode }
            return this.beskrivelseInfo(totalNettoBelop, minFomPeriode, maxTomPeriode)
        }

        is UrMelding -> {
            val urMeldingList = meldingList.filterIsInstance<UrMelding>()
            val totalNettoBelop = urMeldingList.sumOf { it.totalNettoBelop }
            return this.beskrivelseInfo(totalNettoBelop)
        }

        else -> {
            logger.error { "Ukjent meldingstype, sjekk secureLogger" }
            logger.error(marker = TEAM_LOGS_MARKER) { "Ukjent meldingstype: $this" }
            null
        }
    }
}
