package no.nav.sokos.okosynk.integration

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.common.ContentTypes
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

import no.nav.oppgave.models.Oppgave
import no.nav.sokos.okosynk.TestData
import no.nav.sokos.okosynk.WireMockTestData.oppdaterOppgaveWireMock
import no.nav.sokos.okosynk.WireMockTestData.opprettOppgaveWireMock
import no.nav.sokos.okosynk.WireMockTestData.sokOppgaveWireMock
import no.nav.sokos.okosynk.domain.BatchType
import no.nav.sokos.okosynk.exception.OppgaveException
import no.nav.sokos.okosynk.listener.WireMockListener
import no.nav.sokos.okosynk.listener.WireMockListener.wiremock

class OppgaveClientServiceTest :
    FunSpec({
        extensions(WireMockListener)

        val oppgaveClientService: OppgaveClientService by lazy {
            OppgaveClientService(
                oppgaveUrl = wiremock.baseUrl(),
                accessTokenClient = WireMockListener.accessTokenClient,
            )
        }

        test("sokOppgaver should return SokOppgaverResponse") {
            sokOppgaveWireMock()

            val response =
                oppgaveClientService.sokOppgaver(
                    oppgavetype = BatchType.OS.oppgaveType,
                    limit = 1000,
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
                            .withHeader(HttpHeaders.ContentType, ContentTypes.APPLICATION_JSON)
                            .withBody("""{"message": "Internal Server Error"}"""),
                    ),
            )

            val exception =
                shouldThrow<OppgaveException> {
                    oppgaveClientService.sokOppgaver(
                        oppgavetype = BatchType.OS.oppgaveType,
                        limit = 1000,
                        offset = 0,
                    )
                }
            exception.message shouldContain "Feil ved søk av oppgaver. Status: 500 Server Error, XCorrelationId: "
        }

        test("opprettOppgave should return oppgave as response") {
            opprettOppgaveWireMock()

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
                            .withHeader(HttpHeaders.ContentType, ContentTypes.APPLICATION_JSON)
                            .withBody("""{"message": "Bad Request"}"""),
                    ),
            )

            val request = TestData.opprettOppgaveRequest
            val exception =
                shouldThrow<OppgaveException> {
                    oppgaveClientService.opprettOppgave(request)
                }
            exception.message shouldContain "Feil ved opprettelse av oppgave. Status: 400 Bad Request, XCorrelationId: "
        }

        test("oppdaterOppgave should return update oppgave as response") {
            oppdaterOppgaveWireMock()

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
                            .withHeader(HttpHeaders.ContentType, ContentTypes.APPLICATION_JSON)
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
            exception.message shouldContain "Feil ved oppdater av oppgave. Status: 404 Not Found, XCorrelationId: "
        }
    })
