package no.nav.sokos.okosynk.service

import no.nav.sokos.okosynk.integration.FtpService

private val logger = mu.KotlinLogging.logger {}
private val FILES = listOf("OS.INPUT", "UR.INOUT")

class BatchService(
    private val ftpService: FtpService = FtpService(),
) {
    fun start() {
        FILES.forEach { fileName ->
            logger.info { "Starter nedlasting av filnavn: $fileName" }
            val downloadFile = ftpService.downloadFiles(fileName = fileName)
            if (downloadFile.isEmpty()) {
                logger.info { "Start synk $fileName med Oppgave" }
            } else {
                logger.info { "Ingen fil med filnavn: $fileName fins til behandling" }
            }
        }
    }
}
