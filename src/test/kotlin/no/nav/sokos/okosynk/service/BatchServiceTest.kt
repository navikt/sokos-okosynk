package no.nav.sokos.okosynk.service

import FileProcessService
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath
import com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.client.WireMock.verify
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

import no.nav.sokos.okosynk.OPPGAVE_URL
import no.nav.sokos.okosynk.WireMockTestData
import no.nav.sokos.okosynk.config.SftpConfig
import no.nav.sokos.okosynk.domain.BatchType
import no.nav.sokos.okosynk.integration.Directories
import no.nav.sokos.okosynk.integration.FtpService
import no.nav.sokos.okosynk.integration.OppgaveClientService
import no.nav.sokos.okosynk.integration.PdlClientService
import no.nav.sokos.okosynk.listener.SftpListener
import no.nav.sokos.okosynk.listener.WireMockListener
import no.nav.sokos.okosynk.listener.WireMockListener.wiremock
import no.nav.sokos.okosynk.process.BehandleMeldingProcessService
import no.nav.sokos.okosynk.process.BehandleOppgaveProcessService
import no.nav.sokos.okosynk.util.Utils.readFromResource

class BatchServiceTest : FunSpec({
    extensions(SftpListener, WireMockListener)

    val ftpService: FtpService by lazy {
        FtpService(SftpConfig(SftpListener.sftpProperties))
    }

    val pdlClientService: PdlClientService by lazy {
        PdlClientService(
            pdlUrl = wiremock.baseUrl(),
            accessTokenClient = WireMockListener.accessTokenClient,
        )
    }

    val oppgaveClientService: OppgaveClientService by lazy {
        OppgaveClientService(
            oppgaveUrl = wiremock.baseUrl(),
            accessTokenClient = WireMockListener.accessTokenClient,
        )
    }

    val batchService: BatchService by lazy {
        BatchService(
            ftpService = ftpService,
            fileProcessService = FileProcessService(),
            behandleMeldingProcessService = BehandleMeldingProcessService(pdlClientService),
            behandleOppgaveProcessService = BehandleOppgaveProcessService(oppgaveClientService),
        )
    }

    beforeTest {
        SftpListener.deleteFile(Directories.INBOUND.value + "/*")
        wiremock.resetAll()
    }

    test("BatchService run should process batches without error") {
        // Create test files in sftp server
        val osInputFile = "sftp/OS.INPUT".readFromResource()
        SftpListener.createFile(BatchType.OS.fileName, Directories.INBOUND, osInputFile)

        // Mock Pdl and Oppgave responses
        WireMockTestData.hentPersonWireMock()
        WireMockTestData.sokOppgaveWireMock(response = "oppgave/sokOppgaveResponse.json".readFromResource(), offset = 0)
        WireMockTestData.sokOppgaveWireMock(response = """{ "antallTreffTotalt": 4, "oppgaver": [] }""", offset = 1000)
        WireMockTestData.opprettOppgaveWireMock()
        WireMockTestData.oppdaterOppgaveWireMock()

        batchService.run()

        // Search PDL for 11 times
        verify(11, postRequestedFor(WireMock.urlEqualTo("/graphql")))
        // Search Oppgave for 2 times
        verify(2, getRequestedFor(urlPathMatching("$OPPGAVE_URL.*")))
        // Create Oppgave for 11 time
        verify(11, postRequestedFor(urlEqualTo(OPPGAVE_URL)))
        // Update Oppgave for 0 times
        verify(0, patchRequestedFor(urlPathMatching("$OPPGAVE_URL/\\d+")).withRequestBody(matchingJsonPath("$.status", WireMock.absent())))
        // FERDIGSTILT Oppgave for 4 times
        verify(4, patchRequestedFor(urlPathMatching("$OPPGAVE_URL/\\d+")).withRequestBody(matchingJsonPath("$.status", equalTo("FERDIGSTILT"))))

        SftpListener.searchFile(BatchType.OS.fileName) shouldBe true
    }

    test("BatchService run should not process when no meldingFile is found") {
        val batchTypeList = BatchType.entries.filter { it != BatchType.UNKOWN }
        batchTypeList.forEach {
            ftpService.downloadFiles(it.fileName).shouldBeEmpty()
        }

        WireMockTestData.hentPersonWireMock()
        WireMockTestData.sokOppgaveWireMock()
        WireMockTestData.opprettOppgaveWireMock()
        WireMockTestData.oppdaterOppgaveWireMock()

        batchService.run()
        verify(0, postRequestedFor(urlEqualTo("/graphql")))
        verify(0, getRequestedFor(urlPathMatching("$OPPGAVE_URL.*")))
        verify(0, postRequestedFor(urlEqualTo(OPPGAVE_URL)))
        verify(0, patchRequestedFor(urlPathMatching("$OPPGAVE_URL/\\d+")))
    }

    test("BatchService run should not process when meldingFile oversize $MAX_ANTALL_LINJER lines") {
        val osInputFile = "sftp/OS.INPUT".readFromResource()
        val lines = osInputFile.lines().filter { it.isNotBlank() }
        val oversizeInput = List(MAX_ANTALL_LINJER + 1) { lines[it % lines.size] }.joinToString("\n")

        SftpListener.createFile(BatchType.OS.fileName, Directories.INBOUND, oversizeInput)

        WireMockTestData.hentPersonWireMock()
        WireMockTestData.sokOppgaveWireMock()
        WireMockTestData.opprettOppgaveWireMock()
        WireMockTestData.oppdaterOppgaveWireMock()

        batchService.run()
        verify(0, postRequestedFor(urlEqualTo("/graphql")))
        verify(0, getRequestedFor(urlPathMatching("$OPPGAVE_URL.*")))
        verify(0, postRequestedFor(urlEqualTo(OPPGAVE_URL)))
        verify(0, patchRequestedFor(urlPathMatching("$OPPGAVE_URL/\\d+")))

        SftpListener.searchFile(BatchType.OS.fileName) shouldBe true
    }
})
