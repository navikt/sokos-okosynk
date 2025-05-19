package no.nav.sokos.okosynk.process

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

import no.nav.sokos.okosynk.OPPGAVE_URL
import no.nav.sokos.okosynk.TestData.meldingOppgave
import no.nav.sokos.okosynk.WireMockTestData.oppdaterOppgaveWireMock
import no.nav.sokos.okosynk.WireMockTestData.opprettOppgaveWireMock
import no.nav.sokos.okosynk.WireMockTestData.sokOppgaveWireMock
import no.nav.sokos.okosynk.domain.BatchType
import no.nav.sokos.okosynk.domain.BatchTypeContext
import no.nav.sokos.okosynk.domain.MeldingOppgave
import no.nav.sokos.okosynk.integration.OppgaveClientService
import no.nav.sokos.okosynk.listener.WireMockListener
import no.nav.sokos.okosynk.listener.WireMockListener.wiremock
import no.nav.sokos.okosynk.util.Utils.readFromResource

class BehandleOppgaveProcessServiceTest : FunSpec({
    extensions(WireMockListener)

    val oppgaveClientService: OppgaveClientService by lazy {
        OppgaveClientService(
            oppgaveUrl = wiremock.baseUrl(),
            accessTokenClient = WireMockListener.accessTokenClient,
        )
    }

    val behandleOppgaveProcessService: BehandleOppgaveProcessService by lazy {
        BehandleOppgaveProcessService(oppgaveClientService)
    }

    beforeTest {
        BatchTypeContext.set(BatchType.OS)
        wiremock.resetAll()
    }

    test("behandleOppgaveProcessService process should handle oppgaver correctly") {
        sokOppgaveWireMock(response = "oppgave/sokOppgaveResponse.json".readFromResource(), offset = 0)
        sokOppgaveWireMock(response = """{ "antallTreffTotalt": 4, "oppgaver": [] }""", offset = 1000)
        opprettOppgaveWireMock()
        oppdaterOppgaveWireMock()

        val meldingOppgaveSet = setOf(meldingOppgave)
        behandleOppgaveProcessService.process(meldingOppgaveSet)

        verify(2, getRequestedFor(urlPathMatching("$OPPGAVE_URL.*")))
        verify(1, postRequestedFor(urlEqualTo(OPPGAVE_URL)))
        verify(4, patchRequestedFor(urlPathMatching("$OPPGAVE_URL/\\d+")))
    }

    test(name = "behandleOppgaveProcessService process should only created new oppgave") {
        sokOppgaveWireMock(response = """{ "antallTreffTotalt": 0, "oppgaver": [] }""", offset = 0)
        opprettOppgaveWireMock()
        oppdaterOppgaveWireMock()

        val meldingOppgaveSet = setOf(meldingOppgave)
        behandleOppgaveProcessService.process(meldingOppgaveSet)

        verify(1, getRequestedFor(urlPathMatching("$OPPGAVE_URL.*")))
        verify(1, postRequestedFor(urlEqualTo(OPPGAVE_URL)))
        verify(0, patchRequestedFor(urlPathMatching("$OPPGAVE_URL/\\d+")))
    }

    test("behandleOppgaveProcessService should handle patch requests with FERDIGSTILT and null status") {
        sokOppgaveWireMock(response = "oppgave/sokOppgaveResponse.json".readFromResource(), offset = 0)
        sokOppgaveWireMock(response = """{ "antallTreffTotalt": 4, "oppgaver": [] }""", offset = 1000)
        opprettOppgaveWireMock()
        oppdaterOppgaveWireMock()

        val meldingOppgaveSet =
            setOf(
                MeldingOppgave(
                    behandlingstype = "ae0216",
                    tildeltEnhetsnr = "4819",
                    opprettetAvEnhetsnr = "9999",
                    aktoerId = "1000091768276",
                    personIdent = "42126902896",
                    oppgavetype = BatchTypeContext.get().oppgaveType,
                ),
                MeldingOppgave(
                    behandlingstype = "ae0216",
                    tildeltEnhetsnr = "4819",
                    opprettetAvEnhetsnr = "9999",
                    aktoerId = "1000010121748",
                    personIdent = "13015519732",
                    oppgavetype = BatchTypeContext.get().oppgaveType,
                ),
                MeldingOppgave(
                    behandlingstype = "ae0216",
                    tildeltEnhetsnr = "4819",
                    opprettetAvEnhetsnr = "9999",
                    aktoerId = "1000045346097",
                    personIdent = "16123635756",
                    oppgavetype = BatchTypeContext.get().oppgaveType,
                ),
            )
        behandleOppgaveProcessService.process(meldingOppgaveSet)

        verify(0, postRequestedFor(urlEqualTo(OPPGAVE_URL)))
        verify(
            3,
            patchRequestedFor(urlPathMatching("$OPPGAVE_URL/\\d+"))
                .withRequestBody(matchingJsonPath("$.status", WireMock.absent())),
        )

        // Verify PATCH request with status FERDIGSTILT
        verify(
            1,
            patchRequestedFor(urlPathMatching("$OPPGAVE_URL/\\d+"))
                .withRequestBody(matchingJsonPath("$.status", equalTo("FERDIGSTILT"))),
        )
    }
})
