package no.nav.sokos.okosynk.process

import kotlinx.coroutines.runBlocking

import mu.KotlinLogging

import no.nav.sokos.okosynk.config.RegelverkConfig
import no.nav.sokos.okosynk.config.SECURE_LOGGER
import no.nav.sokos.okosynk.domain.BatchType
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
import no.nav.sokos.okosynk.service.BatchTypeContext
import no.nav.sokos.okosynk.util.AktoerUtil.isDnr

private val logger = KotlinLogging.logger { }
private val secureLogger = KotlinLogging.logger(SECURE_LOGGER)

class BehandleMeldingProcessService(
    private val pdlClientService: PdlClientService = PdlClientService(),
) : Chain<List<Melding>, List<MeldingOppgave>> {
    override fun process(meldingList: List<Melding>): List<MeldingOppgave> {
        val batchType = BatchTypeContext.get()!!

        logger.info { "Start BehandleMeldingProcessService " }
        val regelverkMap = if (batchType == BatchType.OS) RegelverkConfig.regelverkOSMap else RegelverkConfig.regelverkURMap
        val meldingOppgaveList =
            meldingList
                .sortedByDescending { melding -> melding.sammenligningsDato() }
                .filter { melding -> regelverkMap[melding.ruleKey()] != null }
                .groupBy { melding -> MeldingKriterier(melding) }
                .mapNotNull { (_, value) -> opprettOppgave(value, batchType, regelverkMap) }
                .distinct()
                .also { oppgave ->
                    logger.info { "Antall konvertert oppgaver for batchType: $batchType: ${oppgave.size}" }
                    Metrics.counter("konvertert_oppgave_${batchType.opprettetAv}").inc(oppgave.size.toLong())
                }

        meldingOppgaveList.filter { it.personIdent?.isDnr() ?: false }
            .forEach { secureLogger.info { "dnr found in the batch file: ${it.personIdent?.take(6)}*****" } }

        return meldingOppgaveList
    }

    private fun hentAktoer(gjelderId: String): String? {
        return runBlocking {
            runCatching {
                pdlClientService.hentIdenter(gjelderId) ?: run {
                    secureLogger.error { "Kunne ikke hente aktørid: $gjelderId" }
                    null
                }
            }.getOrElse { exception ->
                secureLogger.error(exception) { "Feil ved henting av aktørid: $gjelderId" }
                null
            }
        }
    }

    private fun opprettOppgave(
        meldingList: List<Melding>,
        batchType: BatchType,
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
                oppgavetype = batchType.oppgaveType,
                opprettetAvEnhetsnr = ENHET_ID_FOR_ANDRE_EKSTERNE,
            )

        return when (GjelderIdType.value(melding.gjelderId)) {
            GjelderIdType.AKTORID -> hentAktoer(melding.gjelderId)?.let { aktoerId -> meldingOppgave.copy(aktoerId = aktoerId) }
            GjelderIdType.ORGANISASJON -> meldingOppgave.copy(orgnr = melding.gjelderId)
            else -> meldingOppgave.copy(personIdent = melding.gjelderId)
        }
    }

    private fun Melding.beskrivelse(meldingList: List<Melding>): String? {
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
                secureLogger.error { "Ukjent meldingstype: $this" }
                null
            }
        }
    }
}
