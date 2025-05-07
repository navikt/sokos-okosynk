package no.nav.sokos.okosynk.integration

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.okosynk.config.SftpConfig
import no.nav.sokos.okosynk.listener.SftpListener

class FtpServiceTest : FunSpec({
    extensions(SftpListener)

    val ftpService: FtpService by lazy {
        FtpService(SftpConfig(SftpListener.sftpProperties))
    }

    test("opprett fil i INBOUND, endre filnavn i INBOUND") {
        val filename = "test.txt"
        val newFilename = "new_test.txt"

        SftpListener.createFile(filename, Directories.INBOUND, "content")
        ftpService.downloadFiles(filename).size shouldBe 1

        ftpService.renameFile("${Directories.INBOUND.value}/$filename", "${Directories.INBOUND.value}/$newFilename")
        ftpService.downloadFiles(filename).size shouldBe 0
        ftpService.downloadFiles(newFilename).size shouldBe 1
    }
})
