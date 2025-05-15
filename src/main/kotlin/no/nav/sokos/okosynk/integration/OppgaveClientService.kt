package no.nav.sokos.okosynk.integration

import java.util.UUID

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import mu.KLogger
import mu.KotlinLogging

import no.nav.oppgave.models.Oppgave
import no.nav.oppgave.models.OpprettOppgaveRequest
import no.nav.oppgave.models.PatchOppgaveRequest
import no.nav.oppgave.models.SokOppgaverResponse
import no.nav.sokos.okosynk.config.PropertiesConfig
import no.nav.sokos.okosynk.config.httpClient
import no.nav.sokos.okosynk.exception.OppgaveException
import no.nav.sokos.okosynk.security.AccessTokenClient

const val TEMA_OKONOMI_KODE = "OKO"
const val ENHET_ID_FOR_ANDRE_EKSTERNE = "9999"
private const val STATUSKATEGORI_AAPEN = "AAPEN"
private val logger: KLogger = KotlinLogging.logger {}

class OppgaveClientService(
    private val oppgaveUrl: String = PropertiesConfig.OppgaveProperties().oppgaveUrl,
    private val oppgaveScope: String = PropertiesConfig.OppgaveProperties().oppgaveScope,
    private val client: HttpClient = httpClient,
    private val accessTokenClient: AccessTokenClient = AccessTokenClient(azureAdScope = oppgaveScope),
) {
    suspend fun sokOppgaver(
        opprettetAv: String,
        limit: Int,
        offset: Int,
    ): SokOppgaverResponse {
        val result =
            runCatching {
                val accessToken = accessTokenClient.getSystemToken()
                logger.debug { "Søk etter oppgaver" }

                val response =
                    client.get("$oppgaveUrl/api/v1/oppgaver") {
                        header(HttpHeaders.Authorization, "Bearer $accessToken")
                        header(HttpHeaders.XCorrelationId, UUID.randomUUID())
                        contentType(ContentType.Application.Json)
                        parameter("tema", TEMA_OKONOMI_KODE)
                        parameter("opprettetAv", opprettetAv)
                        parameter("statuskategori", STATUSKATEGORI_AAPEN)
                        parameter("limit", limit.toString())
                        parameter("offset", offset.toString())
                    }

                when {
                    response.status.isSuccess() -> response.body<SokOppgaverResponse>()
                    else -> throw OppgaveException("Feil ved søk av oppgaver. Status: ${response.status}")
                }
            }

        return result.fold(
            onSuccess = { response -> response },
            onFailure = { exception -> throw OppgaveException("Feil ved sok oppgaver", exception) },
        )
    }

    suspend fun opprettOppgave(request: OpprettOppgaveRequest): Oppgave {
        return runCatching {
            val accessToken = accessTokenClient.getSystemToken()
            logger.debug { "Opprett en ny oppgave" }

            val response =
                client.post("$oppgaveUrl/api/v1/oppgaver") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    header(HttpHeaders.XCorrelationId, UUID.randomUUID())
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

            response.status.isSuccess() || throw OppgaveException("Feil ved opprettelse av oppgave. Status: ${response.status}")
            response.body<Oppgave>()
        }.fold(
            onSuccess = { response -> response.also { logger.debug { "Oppgave opprettet med id: ${response.id}" } } },
            onFailure = { exception -> throw exception },
        )
    }

    suspend fun oppdaterOppgave(
        id: Long,
        request: PatchOppgaveRequest,
    ): Oppgave {
        return runCatching {
            val accessToken = accessTokenClient.getSystemToken()
            logger.debug { "Oppdater en eksisterende oppgave" }

            val response =
                client.patch("$oppgaveUrl/api/v1/oppgaver/$id") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    header(HttpHeaders.XCorrelationId, UUID.randomUUID())
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

            response.status.isSuccess() || throw OppgaveException("Feil ved oppdater av oppgave. Status: ${response.status}")
            response.body<Oppgave>()
        }.fold(
            onSuccess = { response -> response.also { logger.debug { "Oppgave oppdater med id: $id" } } },
            onFailure = { exception -> throw exception },
        )
    }
}
