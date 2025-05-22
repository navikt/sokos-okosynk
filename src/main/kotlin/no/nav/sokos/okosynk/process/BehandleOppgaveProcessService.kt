package no.nav.sokos.okosynk.process

import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

import kotlinx.coroutines.runBlocking

import mu.KLogger
import mu.KotlinLogging

import no.nav.oppgave.models.BrukerDto
import no.nav.oppgave.models.Oppgave
import no.nav.oppgave.models.OpprettOppgaveRequest
import no.nav.oppgave.models.OpprettOppgaveRequest.Prioritet
import no.nav.oppgave.models.PatchOppgaveRequest
import no.nav.sokos.okosynk.config.PropertiesConfig
import no.nav.sokos.okosynk.config.TEAM_LOGS_MARKER
import no.nav.sokos.okosynk.domain.BatchTypeContext
import no.nav.sokos.okosynk.domain.MeldingOppgave
import no.nav.sokos.okosynk.integration.ENHET_ID_FOR_ANDRE_EKSTERNE
import no.nav.sokos.okosynk.integration.OppgaveClientService
import no.nav.sokos.okosynk.integration.TEMA_OKONOMI_KODE
import no.nav.sokos.okosynk.metrics.Metrics

private val logger: KLogger = KotlinLogging.logger {}

private const val BATCH_SIZE = 1000

class BehandleOppgaveProcessService(
    private val oppgaveClientService: OppgaveClientService = OppgaveClientService(),
) : Chain<Set<MeldingOppgave>, Unit> {
    private val ferdigstiltCounter = AtomicInteger(0)
    private val oppdaterCounter = AtomicInteger(0)
    private val opprettCounter = AtomicInteger(0)

    override fun process(meldingOppgaveSet: Set<MeldingOppgave>) {
        resetCounters()

        logger.info { "Start BehandleOppgaveProcessService " }
        runCatching {
            val oppgaveSet = hentOppgaveSet()

            opprettOppgave(oppgaveSet, meldingOppgaveSet)
            oppdaterOppgaveState(oppgaveSet, meldingOppgaveSet)

            logger.info { "Oppretter: ${opprettCounter.get()}, Oppdater: ${oppdaterCounter.get()}, Ferdigstilt: ${ferdigstiltCounter.get()} oppgaver" }
        }.onFailure { exception ->
            logger.error(exception) { "Feil ved behandling av oppgaver av type: ${BatchTypeContext.get().oppgaveType}" }
        }
    }

    private fun hentOppgaveSet(): Set<Oppgave> {
        val batchType = BatchTypeContext.get()
        return runBlocking {
            logger.info { "Starter søk og evt. inkrementell henting av oppgaver av type: ${batchType.oppgaveType}" }

            val oppgaveSet: MutableSet<Oppgave> = mutableSetOf()
            var offset = 0

            while (true) {
                val response = oppgaveClientService.sokOppgaver(oppgavetype = batchType.oppgaveType, limit = BATCH_SIZE, offset = offset)
                oppgaveSet.addAll(response.oppgaver ?: emptyList())
                if (response.oppgaver?.isEmpty() == true) {
                    break
                }
                offset += BATCH_SIZE
            }

            val filteredOppgaveSet = oppgaveSet.filter { it.opprettetAv == batchType.opprettetAv || it.opprettetAv == PropertiesConfig.Configuration().naisAppName }.toSet()
            logger.info { "Total: ${filteredOppgaveSet.size} oppgaver av type: ${batchType.oppgaveType} funnet" }
            logger.info { "Antall duplikater oppgaver: ${findDuplicateOppgave(oppgaveSet)}" }

            filteredOppgaveSet
        }
    }

    private fun opprettOppgave(
        oppgaveSet: Set<Oppgave>,
        meldingOppgaveSet: Set<MeldingOppgave>,
    ) {
        val batchType = BatchTypeContext.get()
        runBlocking {
            meldingOppgaveSet
                .filterNot { meldingOppgave -> oppgaveSet.any { oppgave -> oppgave.matches(meldingOppgave) } }
                .forEach { meldingOppgave ->
                    val request =
                        OpprettOppgaveRequest(
                            aktivDato = LocalDate.now(),
                            behandlingstema = meldingOppgave.behandlingstema,
                            behandlingstype = meldingOppgave.behandlingstype,
                            fristFerdigstillelse = LocalDate.now().plusDays(batchType.antallDagerFrist),
                            oppgavetype = meldingOppgave.oppgavetype,
                            opprettetAvEnhetsnr = meldingOppgave.opprettetAvEnhetsnr,
                            orgnr = meldingOppgave.orgnr,
                            aktoerId = meldingOppgave.aktoerId,
                            personident = meldingOppgave.personIdent,
                            samhandlernr = meldingOppgave.samhandlernr,
                            prioritet = Prioritet.LAV,
                            tema = TEMA_OKONOMI_KODE,
                            tildeltEnhetsnr = meldingOppgave.tildeltEnhetsnr,
                            beskrivelse = meldingOppgave.beskrivelse,
                        )

                    runCatching {
                        oppgaveClientService.opprettOppgave(request)

                        opprettCounter.incrementAndGet()
                        Metrics.counter("opprett_oppgave_${BatchTypeContext.get().opprettetAv}").inc()
                    }.onFailure { exception ->
                        logger.error(exception) { "Feil ved opprettelse av oppgave, sjekk secureLogger" }
                        logger.error(marker = TEAM_LOGS_MARKER, exception) { "Feil ved opprettelse av oppgave: $request" }
                    }
                }
        }
    }

    private fun oppdaterOppgaveState(
        oppgaveSet: Set<Oppgave>,
        meldingOppgaveSet: Set<MeldingOppgave>,
    ) {
        val batchType = BatchTypeContext.get()

        runBlocking {
            oppgaveSet.forEach { oppgave ->
                val matchingMeldingOppgave = meldingOppgaveSet.firstOrNull { meldingOppgave -> oppgave.matches(meldingOppgave) }
                val request =
                    if (matchingMeldingOppgave == null) {
                        PatchOppgaveRequest(
                            endretAvEnhetsnr = ENHET_ID_FOR_ANDRE_EKSTERNE,
                            versjon = oppgave.versjon,
                            status = PatchOppgaveRequest.Status.FERDIGSTILT,
                        )
                    } else {
                        val beskrivelse = updateBeskrivelseMedKode(oppgave.beskrivelse.orEmpty(), matchingMeldingOppgave.beskrivelse.orEmpty())
                        PatchOppgaveRequest(
                            endretAvEnhetsnr = ENHET_ID_FOR_ANDRE_EKSTERNE,
                            versjon = oppgave.versjon,
                            beskrivelse = beskrivelse,
                        )
                    }

                runCatching {
                    oppgaveClientService.oppdaterOppgave(oppgave.id, request)

                    if (matchingMeldingOppgave == null) {
                        ferdigstiltCounter.incrementAndGet()
                        Metrics.counter("ferdigstilt_oppgave_${batchType.opprettetAv}").inc()
                    } else {
                        oppdaterCounter.incrementAndGet()
                        Metrics.counter("oppdater_oppgave_${batchType.opprettetAv}").inc()
                    }
                }.onFailure { exception ->
                    logger.error(exception) { "Feil ved oppdatering av oppgaveId: ${oppgave.id}, sjekk secureLogger" }
                    logger.error(marker = TEAM_LOGS_MARKER, exception) { "Feil ved oppdatering av oppgaveId: ${oppgave.id}, request: $request" }
                }
            }
        }
    }

    fun Oppgave.matches(meldingOppgave: MeldingOppgave): Boolean {
        return this.behandlingstema == meldingOppgave.behandlingstema &&
            this.behandlingstype == meldingOppgave.behandlingstype &&
            this.tildeltEnhetsnr == meldingOppgave.tildeltEnhetsnr &&
            this.opprettetAvEnhetsnr == meldingOppgave.opprettetAvEnhetsnr &&
            this.matchesBrukerOrAktoerId(meldingOppgave) &&
            this.orgnr == meldingOppgave.orgnr
    }

    fun findDuplicateOppgave(oppgaveSet: Set<Oppgave>): Int {
        val duplicates =
            oppgaveSet.groupBy { oppgave ->
                listOf(
                    oppgave.behandlingstema,
                    oppgave.behandlingstype,
                    oppgave.tildeltEnhetsnr,
                    oppgave.opprettetAvEnhetsnr,
                    oppgave.aktoerId ?: oppgave.bruker?.ident,
                    oppgave.orgnr,
                )
            }.filter { (_, group) -> group.size > 1 }
        return duplicates.values.sumOf { it.size - 1 }
    }

    private fun Oppgave.matchesBrukerOrAktoerId(meldingOppgave: MeldingOppgave): Boolean {
        return when {
            this.aktoerId != null -> this.aktoerId == meldingOppgave.aktoerId
            this.bruker?.type == BrukerDto.Type.SAMHANDLER -> this.bruker.ident == meldingOppgave.samhandlernr
            else -> this.bruker?.ident == meldingOppgave.personIdent
        }
    }

    private fun resetCounters() {
        ferdigstiltCounter.set(0)
        oppdaterCounter.set(0)
        opprettCounter.set(0)
    }

    private fun updateBeskrivelseMedKode(
        beskrivelseFromOppgave: String,
        beskrivelseFromMeldingOppgave: String,
    ): String {
        val beskrivelseFelter = beskrivelseFromOppgave.split(";")
        val kode =
            if (beskrivelseFelter.size > 2) {
                beskrivelseFelter[1].take(10)
            } else {
                ""
            }
        return beskrivelseFromMeldingOppgave.replaceFirst(";;", ";$kode;")
    }
}
