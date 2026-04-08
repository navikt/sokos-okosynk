package no.nav.sokos.okosynk

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

import no.nav.sokos.okosynk.config.ApplicationState
import no.nav.sokos.okosynk.config.PropertiesConfig.schedulerProperties
import no.nav.sokos.okosynk.config.applicationLifecycleConfig
import no.nav.sokos.okosynk.config.commonConfig
import no.nav.sokos.okosynk.config.routingConfig
import no.nav.sokos.okosynk.metrics.Metrics.initMetrics
import no.nav.sokos.okosynk.service.SchedulerService

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(true)
}

fun Application.module() {
    val applicationState = ApplicationState()

    commonConfig()
    applicationLifecycleConfig(applicationState)
    routingConfig(applicationState)
    initMetrics()

    if (schedulerProperties.enabled) {
        val scheduler = SchedulerService()
        scheduler.scheduleWithCronExpression(schedulerProperties.cronExpression)

        this.monitor.subscribe(ApplicationStopping) {
            scheduler.stop()
        }
    }
}
