package no.nav.sokos.okosynk.security

import kotlinx.coroutines.runBlocking

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.TestCaseOrder
import io.kotest.matchers.shouldBe

import no.nav.sokos.okosynk.config.PropertiesConfig
import no.nav.sokos.okosynk.config.httpClient
import no.nav.sokos.okosynk.listener.WiremockListener
import no.nav.sokos.okosynk.listener.WiremockListener.wiremock

class AccessTokenClientTest : FunSpec({
    extensions(WiremockListener)

    testOrder = TestCaseOrder.Sequential

    val mockAzureAdProperties =
        PropertiesConfig.AzureAdProperties(
            tenantId = "test-tenant",
            clientId = "test-client-id",
            clientSecret = "test-client-secret",
        )

    val accessTokenClient: AccessTokenClient by lazy {
        AccessTokenClient(
            azureAdProperties = mockAzureAdProperties,
            azureAdScope = "test-scope",
            client = httpClient,
            azureAdAccessTokenUrl = wiremock.baseUrl() + "/oauth2/v2.0/token",
        )
    }

    test("getSystemToken should throw an exception on failure") {
        wiremock.stubFor(
            post(urlEqualTo("/oauth2/v2.0/token"))
                .willReturn(
                    aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"error_description": "Invalid request"}"""),
                ),
        )

        val exception =
            runBlocking {
                kotlin.runCatching { accessTokenClient.getSystemToken() }.exceptionOrNull()
            }

        exception?.message shouldBe "GetAccessToken returnerte 400 Bad Request med feilmelding: Invalid request"
    }

    test("getSystemToken should return a valid token") {
        val mockResponse =
            """
            {
                "access_token": "mock-access-token",
                "expires_in": 3600
            }
            """.trimIndent()

        wiremock.stubFor(
            post(urlEqualTo("/oauth2/v2.0/token"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse),
                ),
        )

        val token = runBlocking { accessTokenClient.getSystemToken() }
        token shouldBe "mock-access-token"
    }
})
