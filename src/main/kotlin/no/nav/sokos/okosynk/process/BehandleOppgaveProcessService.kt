package no.nav.sokos.okosynk.process

import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

import kotlinx.coroutines.runBlocking

import mu.KLogger
import mu.KotlinLogging

import no.nav.oppgave.models.Oppgave
import no.nav.oppgave.models.OpprettOppgaveRequest
import no.nav.oppgave.models.OpprettOppgaveRequest.Prioritet
import no.nav.oppgave.models.PatchOppgaveRequest
import no.nav.sokos.okosynk.config.SECURE_LOGGER
import no.nav.sokos.okosynk.domain.BatchTypeContext
import no.nav.sokos.okosynk.domain.MeldingOppgave
import no.nav.sokos.okosynk.integration.ENHET_ID_FOR_ANDRE_EKSTERNE
import no.nav.sokos.okosynk.integration.OppgaveClientService
import no.nav.sokos.okosynk.integration.TEMA_OKONOMI_KODE
import no.nav.sokos.okosynk.metrics.Metrics

private val secureLogger = KotlinLogging.logger(SECURE_LOGGER)
private val logger: KLogger = KotlinLogging.logger {}

private const val BATCH_SIZE = 1000

class BehandleOppgaveProcessService(
    private val oppgaveClientService: OppgaveClientService = OppgaveClientService(),
) : Chain<List<MeldingOppgave>, Unit> {
    private val ferdigstiltCounter = AtomicInteger(0)
    private val oppdaterCounter = AtomicInteger(0)
    private val opprettCounter = AtomicInteger(0)

    override fun process(meldingOppgaveList: List<MeldingOppgave>) {
        resetCounters()

        logger.info { "Start BehandleOppgaveProcessService " }
        runCatching {
            val oppgaveList = hentOppgaveList()

            opprettOppgave(oppgaveList, meldingOppgaveList)
            oppdaterOppgaveState(oppgaveList, meldingOppgaveList)

            logger.info { "Oppretter: ${opprettCounter.get()}, Oppdater: ${oppdaterCounter.get()}, Ferdigstilt: ${ferdigstiltCounter.get()} oppgaver" }
        }.onFailure { exception ->
            logger.error(exception) { "Feil ved behandling av oppgaver av type: ${BatchTypeContext.get().oppgaveType}" }
        }
    }

    private fun hentOppgaveList(): Set<Oppgave> {
        val batchType = BatchTypeContext.get()
        return runBlocking {
            logger.info { "Starter søk og evt. inkrementell henting av oppgaver av type: ${batchType.oppgaveType}" }

            val oppgaveList: MutableSet<Oppgave> = mutableSetOf()
            var offset = 0

            while (true) {
                val response = oppgaveClientService.sokOppgaver(opprettetAv = batchType.opprettetAv, limit = BATCH_SIZE, offset = offset)
                oppgaveList.addAll(response.oppgaver ?: emptyList())
                if (response.oppgaver?.isEmpty() == true) {
                    break
                }
                offset += BATCH_SIZE
            }
            logger.info { "Total: ${oppgaveList.size} oppgaver av type: ${batchType.oppgaveType} funnet" }
            logger.info { "Antall duplikater oppgaver: ${findDuplicateOppgave(oppgaveList)}" }

            oppgaveList
        }
    }

    private fun opprettOppgave(
        oppgaveList: Set<Oppgave>,
        meldingOppgaveList: List<MeldingOppgave>,
    ) {
        runBlocking {
            meldingOppgaveList
                .filterNot { meldingOppgave -> oppgaveList.any { oppgave -> oppgave.matches(meldingOppgave) } }
                .forEach { meldingOppgave ->
                    val request =
                        OpprettOppgaveRequest(
                            aktivDato = LocalDate.now(),
                            behandlingstema = meldingOppgave.behandlingstema,
                            behandlingstype = meldingOppgave.behandlingstype,
                            fristFerdigstillelse = LocalDate.now(),
                            oppgavetype = meldingOppgave.oppgavetype,
                            opprettetAvEnhetsnr = meldingOppgave.opprettetAvEnhetsnr,
                            orgnr = meldingOppgave.orgnr,
                            personident = meldingOppgave.aktoerId ?: meldingOppgave.personIdent,
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
                        secureLogger.error(exception) { "Feil ved opprettelse av oppgave: $request" }
                    }
                }
        }
    }

    private fun oppdaterOppgaveState(
        oppgaveList: Set<Oppgave>,
        meldingOppgaveList: List<MeldingOppgave>,
    ) {
        val batchType = BatchTypeContext.get()

        runBlocking {
            oppgaveList.forEach { oppgave ->
                val ferdigstilt = !meldingOppgaveList.any { meldingOppgave -> oppgave.matches(meldingOppgave) }

                val request =
                    PatchOppgaveRequest(
                        endretAvEnhetsnr = ENHET_ID_FOR_ANDRE_EKSTERNE,
                        versjon = oppgave.versjon,
                        status = if (ferdigstilt) PatchOppgaveRequest.Status.FERDIGSTILT else null,
                    )

                runCatching {
                    oppgaveClientService.oppdaterOppgave(oppgave.id, request)

                    if (ferdigstilt) {
                        ferdigstiltCounter.incrementAndGet()
                        Metrics.counter("ferdigstilt_oppgave_${batchType.opprettetAv}").inc()
                    } else {
                        oppdaterCounter.incrementAndGet()
                        Metrics.counter("oppdater_oppgave_${batchType.opprettetAv}").inc()
                    }
                }.onFailure { exception ->
                    logger.error(exception) { "Feil ved oppdatering av oppgaveId: ${oppgave.id}, sjekk secureLogger" }
                    secureLogger.error(exception) { "Feil ved oppdatering av oppgaveId: ${oppgave.id}, request: $request" }
                }
            }
        }
    }

    private fun resetCounters() {
        ferdigstiltCounter.set(0)
        oppdaterCounter.set(0)
        opprettCounter.set(0)
    }

    fun Oppgave.matches(meldingOppgave: MeldingOppgave): Boolean {
        return this.behandlingstema == meldingOppgave.behandlingstema &&
            this.behandlingstype == meldingOppgave.behandlingstype &&
            this.tildeltEnhetsnr == meldingOppgave.tildeltEnhetsnr &&
            this.opprettetAvEnhetsnr == meldingOppgave.opprettetAvEnhetsnr &&
            (this.aktoerId?.let { it == meldingOppgave.aktoerId } ?: (this.bruker?.ident == meldingOppgave.personIdent)) &&
            this.orgnr == meldingOppgave.orgnr
    }

    fun findDuplicateOppgave(oppgaveList: Set<Oppgave>): Int {
        val duplicates =
            oppgaveList.groupBy { oppgave ->
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
}
