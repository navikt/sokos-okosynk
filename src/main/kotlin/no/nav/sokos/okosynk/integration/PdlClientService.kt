package no.nav.sokos.okosynk.integration

import com.expediagroup.graphql.client.types.GraphQLClientError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import mu.KotlinLogging
import org.slf4j.MDC

import no.nav.pdl.HentIdenter
import no.nav.pdl.hentidenter.IdentInformasjon
import no.nav.sokos.okosynk.config.PropertiesConfig
import no.nav.sokos.okosynk.config.httpClient
import no.nav.sokos.okosynk.exception.PdlException
import no.nav.sokos.okosynk.integration.model.GraphQLResponse
import no.nav.sokos.okosynk.security.AccessTokenClient

private val logger = KotlinLogging.logger {}

class PdlClientService(
    private val pdlUrl: String = PropertiesConfig.PdlProperties().pdlUrl,
    private val pdlScope: String = PropertiesConfig.PdlProperties().pdlScope,
    private val client: HttpClient = httpClient,
    private val accessTokenClient: AccessTokenClient = AccessTokenClient(azureAdScope = pdlScope),
) {
    suspend fun hentIdenter(ident: List<String>): Map<String, List<IdentInformasjon>> {
        val request = HentIdenter(HentIdenter.Variables(ident))

        logger.info { "Henter accesstoken for oppslag mot PDL" }
        val accessToken = accessTokenClient.getSystemToken()

        logger.info { "Henter ident fra PDL" }
        val response =
            client.post("$pdlUrl/graphql") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("behandlingsnummer", "B154")
                header("Nav-Call-Id", MDC.get("x-correlation-id"))
                contentType(ContentType.Application.Json)
                setBody(request)
            }

        return when {
            response.status.isSuccess() -> {
                val result = response.body<GraphQLResponse<HentIdenter.Result>>()
                if (result.errors?.isNotEmpty() == true) {
                    handleErrors(result.errors)
                }
                result.data
                    ?.hentIdenterBolk
                    ?.map { item -> item.ident to (item.identer ?: emptyList()) }
                    ?.groupBy({ it.first }, { it.second })
                    ?.mapValues { entry -> entry.value.flatten() }
                    .orEmpty()
            }

            else -> throw ClientRequestException(response, "Noe gikk galt ved oppslag mot PDL")
        }
    }

    private fun handleErrors(errors: List<GraphQLClientError>) {
        val errorMessage = errors.joinToString { it.message }
        val exceptionMessage = "Message: $errorMessage"
        throw PdlException(exceptionMessage)
    }
}
