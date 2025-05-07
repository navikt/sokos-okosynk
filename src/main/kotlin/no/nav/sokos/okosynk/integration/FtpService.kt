package no.nav.sokos.okosynk.integration

import java.io.ByteArrayOutputStream

import com.jcraft.jsch.SftpException
import mu.KotlinLogging

import no.nav.sokos.okosynk.config.SftpConfig
import no.nav.sokos.okosynk.exception.SFtpException

private val logger = KotlinLogging.logger {}

enum class Directories(
    var value: String,
) {
    INBOUND("/inbound"),
}

class FtpService(
    private val sftpConfig: SftpConfig = SftpConfig(),
) {
    fun renameFile(
        oldFilename: String,
        newFilename: String,
    ) {
        sftpConfig.channel { connector ->
            runCatching {
                connector.rename(oldFilename, newFilename)
                logger.debug { "Filen endre navn fra $oldFilename til $oldFilename" }
            }.onFailure { exception ->
                logger.error { "Feil til endre navn fra $oldFilename til $newFilename. Feil: ${exception.message}" }
                throw SFtpException("SFTP-feil: $exception")
            }
        }
    }

    fun downloadFiles(
        directory: Directories = Directories.INBOUND,
        fileName: String,
    ): List<String> {
        return sftpConfig.channel { connector ->
            try {
                val files =
                    connector
                        .ls("${directory.value}/*")
                        .filter { it.filename == fileName }

                if (files.isEmpty()) {
                    logger.info { "Ingen filer med navn $fileName funnet i mappen ${directory.value}" }
                    return@channel emptyList()
                }

                ByteArrayOutputStream().use { outputStream ->
                    logger.info { "Laster ned filen $fileName fra mappen ${directory.value}" }
                    connector.get("${directory.value}/$fileName", outputStream)
                    return@channel String(outputStream.toByteArray())
                        .lines()
                        .filter { it.isNotBlank() }
                }
            } catch (exception: SftpException) {
                logger.error { "$fileName ble ikke hentet. Feilmelding: ${exception.message}" }
                throw SFtpException("SFtp-feil: $exception", exception)
            }
        }
    }
}
