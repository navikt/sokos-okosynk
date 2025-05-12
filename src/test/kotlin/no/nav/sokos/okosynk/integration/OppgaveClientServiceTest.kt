package no.nav.sokos.okosynk.integration

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.apache.http.entity.ContentType.APPLICATION_JSON

import no.nav.oppgave.models.Oppgave
import no.nav.sokos.okosynk.TestData
import no.nav.sokos.okosynk.domain.BatchType
import no.nav.sokos.okosynk.exception.OppgaveException
import no.nav.sokos.okosynk.listener.WiremockListener
import no.nav.sokos.okosynk.listener.WiremockListener.wiremock
import no.nav.sokos.okosynk.util.Utils.readFromResource

class OppgaveClientServiceTest : FunSpec({
    extensions(WiremockListener)

    val oppgaveClientService: OppgaveClientService by lazy {
        OppgaveClientService(
            oppgaveUrl = wiremock.baseUrl(),
            accessTokenClient = WiremockListener.accessTokenClient,
        )
    }

    test("sokOppgaver should return SokOppgaverResponse") {
        val sokOppgaveResponse = "oppgave/sokOppgaveResponse.json".readFromResource()

        wiremock.stubFor(
            get(urlPathEqualTo("/api/v1/oppgaver"))
                .withQueryParam("tema", matching("OKO"))
                .withQueryParam("opprettetAv", matching("okosynkos"))
                .withQueryParam("statuskategori", matching("AAPEN"))
                .withQueryParam("limit", matching("10"))
                .withQueryParam("offset", matching("0"))
                .withHeader(HttpHeaders.Authorization, matching("Bearer .*"))
                .withHeader(HttpHeaders.XCorrelationId, matching(".*"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON.mimeType)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(sokOppgaveResponse),
                ),
        )

        val response =
            oppgaveClientService.sokOppgaver(
                opprettetAv = BatchType.OS.opprettetAv,
                limit = 10,
                offset = 0,
            )
        response.oppgaver.shouldNotBeEmpty()
        response.antallTreffTotalt shouldBe 4
        response.oppgaver!!.size shouldBe 4
    }

    test("sokOppgaver should throw OppgaveException on failure") {
        wiremock.stubFor(
            get(urlPathEqualTo("/api/v1/oppgaver"))
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatusCode.InternalServerError.value)
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON.mimeType)
                        .withBody("""{"message": "Internal Server Error"}"""),
                ),
        )

        val exception =
            shouldThrow<OppgaveException> {
                oppgaveClientService.sokOppgaver(
                    opprettetAv = BatchType.OS.opprettetAv,
                    limit = 10,
                    offset = 0,
                )
            }
        exception.message shouldBe "Feil ved sok oppgaver"
    }

    test("opprettOppgave should return oppgave as response") {
        val opprettOppgaveResponse = "oppgave/opprettOppgaveResponse.json".readFromResource()

        wiremock.stubFor(
            post(urlEqualTo("/api/v1/oppgaver"))
                .withHeader(HttpHeaders.Authorization, matching("Bearer .*"))
                .withHeader(HttpHeaders.XCorrelationId, matching(".*"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON.mimeType)
                        .withStatus(HttpStatusCode.Created.value)
                        .withBody(opprettOppgaveResponse),
                ),
        )

        val request = TestData.opprettOppgaveRequest

        val response = oppgaveClientService.opprettOppgave(request)
        response.id shouldBe 375144350L
        response.status shouldBe Oppgave.Status.OPPRETTET
        response.aktivDato shouldBe request.aktivDato
        response.behandlingstema shouldBe request.behandlingstema
        response.behandlingstype shouldBe request.behandlingstype
        response.fristFerdigstillelse shouldBe request.fristFerdigstillelse
        response.oppgavetype shouldBe request.oppgavetype
        response.opprettetAvEnhetsnr shouldBe request.opprettetAvEnhetsnr
        response.orgnr shouldBe request.orgnr
        response.bruker?.ident shouldBe request.personident
        response.prioritet.name shouldBe request.prioritet.name
        response.tema shouldBe request.tema
        response.tildeltEnhetsnr shouldBe request.tildeltEnhetsnr
        response.beskrivelse shouldBe request.beskrivelse
        response.versjon shouldBe 1
    }

    test("opprettOppgave should throw OppgaveException on failure") {
        wiremock.stubFor(
            post(urlEqualTo("/api/v1/oppgaver"))
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatusCode.BadRequest.value)
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON.mimeType)
                        .withBody("""{"message": "Bad Request"}"""),
                ),
        )

        val request = TestData.opprettOppgaveRequest
        val exception =
            shouldThrow<OppgaveException> {
                oppgaveClientService.opprettOppgave(request)
            }
        exception.message shouldBe "Feil ved opprettelse av oppgave. Status: 400 Bad Request"
    }

    test("oppdaterOppgave should return update oppgave as response") {
        val oppdaterOppgaveResponse = "oppgave/oppdaterOppgaveResponse.json".readFromResource()

        wiremock.stubFor(
            patch(urlEqualTo("/api/v1/oppgaver/12345"))
                .withHeader(HttpHeaders.Authorization, matching("Bearer .*"))
                .withHeader(HttpHeaders.XCorrelationId, matching(".*"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON.mimeType)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(oppdaterOppgaveResponse),
                ),
        )

        val request = TestData.patchOppgaveRequest
        val response =
            oppgaveClientService.oppdaterOppgave(
                id = 12345,
                request = request,
            )

        response.id shouldBe 375144350L
        response.status shouldBe Oppgave.Status.FERDIGSTILT
        response.endretAvEnhetsnr shouldBe request.endretAvEnhetsnr
        response.versjon shouldBe 2
    }

    test("oppdaterOppgave should throw OppgaveException on failure") {
        wiremock.stubFor(
            patch(urlEqualTo("/api/v1/oppgaver/12345"))
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatusCode.NotFound.value)
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON.mimeType)
                        .withBody("""{"message": "Not Found"}"""),
                ),
        )

        val request = TestData.patchOppgaveRequest
        val exception =
            shouldThrow<OppgaveException> {
                oppgaveClientService.oppdaterOppgave(
                    id = 12345,
                    request = request,
                )
            }
        exception.message shouldBe "Feil ved oppdater av oppgave. Status: 404 Not Found"
    }
})
