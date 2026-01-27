package no.nav.sokos.okosynk

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.matching
import com.github.tomakehurst.wiremock.client.WireMock.patch
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.common.ContentTypes
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

import no.nav.sokos.okosynk.listener.WireMockListener.wiremock
import no.nav.sokos.okosynk.util.Utils.readFromResource

const val OPPGAVE_URL = "/api/v1/oppgaver"

object WireMockTestData {
    fun sokOppgaveWireMock(
        response: String = "oppgave/sokOppgaveResponse.json".readFromResource(),
        offset: Int = 0,
    ) {
        wiremock.stubFor(
            get(urlPathEqualTo(OPPGAVE_URL))
                .withQueryParam("tema", matching("OKO"))
                .withQueryParam("oppgavetype", matching("OKO_OS"))
                .withQueryParam("statuskategori", matching("AAPEN"))
                .withQueryParam("limit", matching("1000"))
                .withQueryParam("offset", matching(offset.toString()))
                .withHeader(HttpHeaders.Authorization, matching("Bearer .*"))
                .withHeader(HttpHeaders.XCorrelationId, matching(".*"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, ContentTypes.APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(response),
                ),
        )
    }

    fun opprettOppgaveWireMock(response: String = "oppgave/opprettOppgaveResponse.json".readFromResource()) {
        wiremock.stubFor(
            post(urlEqualTo(OPPGAVE_URL))
                .withHeader(HttpHeaders.Authorization, matching("Bearer .*"))
                .withHeader(HttpHeaders.XCorrelationId, matching(".*"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, ContentTypes.APPLICATION_JSON)
                        .withStatus(HttpStatusCode.Created.value)
                        .withBody(response),
                ),
        )
    }

    fun oppdaterOppgaveWireMock(response: String = "oppgave/oppdaterOppgaveResponse.json".readFromResource()) {
        wiremock.stubFor(
            patch(urlPathMatching("$OPPGAVE_URL/\\d+"))
                .withHeader(HttpHeaders.Authorization, matching("Bearer .*"))
                .withHeader(HttpHeaders.XCorrelationId, matching(".*"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, ContentTypes.APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(response),
                ),
        )
    }

    fun hentPersonWireMock(response: String = "pdl/hentPersonResponse.json".readFromResource()) {
        wiremock.stubFor(
            post(urlEqualTo("/graphql"))
                .willReturn(
                    aResponse()
                        .withHeader(HttpHeaders.ContentType, ContentTypes.APPLICATION_JSON)
                        .withStatus(HttpStatusCode.OK.value)
                        .withBody(response),
                ),
        )
    }
}
