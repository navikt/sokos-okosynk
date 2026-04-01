package no.nav.sokos.okosynk.service

import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

import mu.KotlinLogging

import no.nav.sokos.okosynk.domain.BatchType
import no.nav.sokos.okosynk.integration.Directories
import no.nav.sokos.okosynk.integration.FtpService
import no.nav.sokos.okosynk.metrics.Metrics
import no.nav.sokos.okosynk.process.BehandleMeldingProcessService
import no.nav.sokos.okosynk.process.BehandleOppgaveProcessService
import no.nav.sokos.okosynk.process.FileProcessService
import no.nav.sokos.okosynk.util.TraceUtils
import no.nav.sokos.okosynk.util.Utils.toISO

private val logger = KotlinLogging.logger {}
const val MAX_ANTALL_LINJER = 25000

class BatchService(
    private val ftpService: FtpService = FtpService(),
    private val fileProcessService: FileProcessService = FileProcessService(),
    private val behandleMeldingProcessService: BehandleMeldingProcessService = BehandleMeldingProcessService(),
    private val behandleOppgaveProcessService: BehandleOppgaveProcessService = BehandleOppgaveProcessService(),
) {
    suspend fun run() {
        val batchTypeList = BatchType.entries.filter { it != BatchType.UNKOWN }

        runCatching {
            batchTypeList.forEach { batchType ->
                TraceUtils.withTracerId {
                    val startNanos = System.nanoTime()
                    processBatch(batchType)
                    Metrics
                        .timer("batch_${batchType.opprettetAv}")
                        .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS)
                }
            }
        }.onFailure { exception ->
            logger.error(exception) { "Feil ved behandling fil fra OS/UR" }
        }
    }

    private suspend fun processBatch(batchType: BatchType) {
        logger.info { "Starter nedlasting av filnavn: ${batchType.fileName}" }
        val meldingFile = ftpService.downloadFiles(fileName = batchType.fileName)

        when {
            meldingFile.isEmpty() -> logger.info { "Ingen fil med filnavn: ${batchType.fileName} fins til behandling, synking avsluttes" }
            meldingFile.size > MAX_ANTALL_LINJER -> logger.error { "Fil ${batchType.fileName} overskrider maks antall linjer: ($MAX_ANTALL_LINJER)" }
            else -> {
                logger.info { "Start synk ${batchType.fileName} med Oppgave" }

                meldingFile
                    .run { fileProcessService.process(batchType, this) }
                    .run { behandleMeldingProcessService.process(batchType, this) }
                    .run { behandleOppgaveProcessService.process(batchType, this) }

                ftpService.renameFile(
                    oldFilename = "${Directories.INBOUND.value}/${batchType.fileName}",
                    newFilename = "${Directories.INBOUND.value}/${batchType.fileName}.${LocalDateTime.now().toISO()}",
                )

                logger.info { "Ferdig synk ${batchType.fileName} med Oppgave" }
            }
        }
    }
}
