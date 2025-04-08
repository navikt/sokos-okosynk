package no.nav.sokos.okosynk.security

import com.auth0.jwt.JWT
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall

private const val JWT_CLAIM_NAVIDENT = "NAVident"

object AuthToken {
    fun getSaksbehandler(call: ApplicationCall): String {
        val token = call.request.headers[HttpHeaders.Authorization]?.removePrefix("Bearer ") ?: throw Error("Could not get token from request header")
        return JWT.decode(token).claims[JWT_CLAIM_NAVIDENT]?.asString() ?: throw RuntimeException("Missing NAVident in private claims")
    }
}
