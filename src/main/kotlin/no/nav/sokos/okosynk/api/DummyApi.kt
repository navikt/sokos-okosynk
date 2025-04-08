package no.nav.sokos.okosynk.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

import no.nav.sokos.okosynk.service.DummyService

fun Route.dummyApi(dummyService: DummyService = DummyService()) {
    route("/api/v1/") {
        get("hello") {
            val response = dummyService.sayHello()
            call.respond(HttpStatusCode.OK, response)
        }
    }
}
