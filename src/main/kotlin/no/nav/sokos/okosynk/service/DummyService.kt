package no.nav.sokos.okosynk.service

import no.nav.sokos.okosynk.domain.DummyDomain
import no.nav.sokos.okosynk.metrics.Metrics

class DummyService {
    fun sayHello(): DummyDomain {
        Metrics.exampleCounter.inc()
        return DummyDomain("This is a template for Team Motta og Beregne")
    }
}
