package no.nav.sokos.okosynk.integration

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import org.apache.http.entity.ContentType.APPLICATION_JSON

import no.nav.sokos.okosynk.exception.PdlException
import no.nav.sokos.okosynk.listener.WiremockListener
import no.nav.sokos.okosynk.listener.WiremockListener.wiremock
import no.nav.sokos.okosynk.util.Utils.readFromResource

private const val AKTORID = "70078749472"

class PdlClientServiceTest : FunSpec({
    extensions(WiremockListener)

    val pdlClientService: PdlClientService by lazy {
        PdlClientService(
            pdlUrl = wiremock.baseUrl(),
            accessTokenClient = WiremockListener.accessTokenClient,
        )
    }

    test("hentPerson should return person response with aktorId") {
        val hentPersonResponse = "pdl/hentPersonResponse.json".readFromResource()

        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON.mimeType)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentPersonResponse),
                ),
        )

        val response = pdlClientService.hentIdenter(AKTORID)
        response shouldBe "2305469522806"
    }

    test("hentPerson should throw PdlException with not found person") {
        val hentPersonResponse = "pdl/hentPersonNotFoundResponse.json".readFromResource()

        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON.mimeType)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentPersonResponse),
                ),
        )

        val exception =
            shouldThrow<PdlException> {
                pdlClientService.hentIdenter(AKTORID)
            }
        exception.message shouldBe "Fant ikke person"
    }

    test("hentPerson should throw PdlException with not authorized") {
        val hentPersonResponse = "pdl/hentPersonNotAuthorizedResponse.json".readFromResource()

        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON.mimeType)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(hentPersonResponse),
                ),
        )

        val exception =
            shouldThrow<PdlException> {
                pdlClientService.hentIdenter(AKTORID)
            }
        exception.message shouldBe "Ikke autentisert"
    }

    test("hentPerson should throw ClientExcpetion ") {
        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatusCode.InternalServerError.value)
                        .withHeader(HttpHeaders.ContentType, APPLICATION_JSON.mimeType)
                        .withBody("""{"message": "Internal Server Error"}"""),
                ),
        )

        val exception =
            shouldThrow<ClientRequestException> {
                pdlClientService.hentIdenter(AKTORID)
            }
        exception.message shouldContain "Noe gikk galt ved oppslag mot PDL"
    }
})
