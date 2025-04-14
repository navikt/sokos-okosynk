package no.nav.sokos.okosynk.integration

import java.util.UUID

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.parameters
import mu.KotlinLogging

import no.nav.oppgave.models.Oppgave
import no.nav.oppgave.models.OpprettOppgaveRequest
import no.nav.oppgave.models.PatchOppgaveRequest
import no.nav.oppgave.models.SokOppgaverResponse
import no.nav.sokos.okosynk.config.PropertiesConfig
import no.nav.sokos.okosynk.config.httpClient
import no.nav.sokos.okosynk.exception.OppgaveException
import no.nav.sokos.okosynk.security.AccessTokenClient

private val logger = KotlinLogging.logger {}

private const val TEMA_OKONOMI_KODE = "OKO"
private const val STATUSKATEGORI_AAPEN = "AAPEN"

class OppgaveClientService(
    private val oppgaveUrl: String = PropertiesConfig.OppgaveProperties().oppgaveUrl,
    private val oppgaveScope: String = PropertiesConfig.OppgaveProperties().oppgaveScope,
    private val client: HttpClient = httpClient,
    private val accessTokenClient: AccessTokenClient = AccessTokenClient(azureAdScope = oppgaveScope),
) {
    suspend fun sokOppgaver(
        limit: Int,
        offset: Int,
    ) {
        val response =
            runCatching {
                val accessToken = accessTokenClient.getSystemToken()
                logger.info { "Søk åpen oppgaver" }

                client.get("$oppgaveUrl/api/v1/oppgaver") {
                    header(HttpHeaders.Authorization, "Bearer $accessToken")
                    header(HttpHeaders.XCorrelationId, UUID.randomUUID())
                    contentType(ContentType.Application.Json)
                    parameters {
                        append("tema", TEMA_OKONOMI_KODE)
                        append("statuskategori", STATUSKATEGORI_AAPEN)
                        append("limit", limit.toString())
                        append("offset", offset.toString())
                    }
                }.body<SokOppgaverResponse>()
            }

        return response.fold(
            onSuccess = { it.also { logger.info { "Antall oppgaver hentet: ${it.oppgaver?.size}" } } },
            onFailure = { e -> throw OppgaveException("Feil ved sok oppgaver", e) },
        )
    }

    suspend fun opprettOppgave(request: OpprettOppgaveRequest): Oppgave {
        return runCatching {
            val accessToken = accessTokenClient.getSystemToken()
            logger.info { "Opprett en ny oppgave" }

            client.post("$oppgaveUrl/api/v1/oppgaver") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header(HttpHeaders.XCorrelationId, UUID.randomUUID())
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<Oppgave>()
        }.fold(
            onSuccess = { response -> response.also { logger.info { "Oppgave opprettet med id: ${response.id}" } } },
            onFailure = { e -> throw OppgaveException("Feil ved opprettelse av oppgave", e) },
        )
    }

    suspend fun oppdaterOppgave(
        id: Int,
        request: PatchOppgaveRequest,
    ): Oppgave {
        return runCatching {
            val accessToken = accessTokenClient.getSystemToken()
            logger.info { "Oppdater en eksisterende oppgave" }

            client.patch("$oppgaveUrl/api/v1/oppgaver/$id") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header(HttpHeaders.XCorrelationId, UUID.randomUUID())
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<Oppgave>()
        }.fold(
            onSuccess = { response -> response.also { logger.info { "Oppgave oppdater med id: $id" } } },
            onFailure = { e -> throw OppgaveException("Feil ved oppdatering av oppgave", e) },
        )
    }
}
