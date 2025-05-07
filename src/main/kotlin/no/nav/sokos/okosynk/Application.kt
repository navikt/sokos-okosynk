package no.nav.sokos.okosynk

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

import no.nav.sokos.okosynk.config.ApplicationState
import no.nav.sokos.okosynk.config.PropertiesConfig
import no.nav.sokos.okosynk.config.applicationLifecycleConfig
import no.nav.sokos.okosynk.config.commonConfig
import no.nav.sokos.okosynk.config.routingConfig
import no.nav.sokos.okosynk.service.SchedulerService

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(true)
}

fun Application.module() {
    val applicationState = ApplicationState()

    commonConfig()
    applicationLifecycleConfig(applicationState)
    routingConfig(applicationState)

    if (PropertiesConfig.SchedulerProperties().enabled) {
        val scheduler = SchedulerService()
        scheduler.scheduleWithCronExpression(PropertiesConfig.SchedulerProperties().cronExpression)

        // Make sure to stop the scheduler when the application shuts down
        this.monitor.subscribe(ApplicationStopping) {
            scheduler.stop()
        }
    }
}
