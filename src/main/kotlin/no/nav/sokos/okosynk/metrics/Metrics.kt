package no.nav.sokos.okosynk.metrics

import java.util.concurrent.ConcurrentHashMap

import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.core.metrics.Counter

private const val METRICS_NAMESPACE = "sokos_okosynk"

object Metrics {
    private val counters = ConcurrentHashMap<String, Counter>()
    private val timers = ConcurrentHashMap<String, Timer>()

    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val timer: (metricName: String) -> Timer = { metricName ->
        timers.computeIfAbsent(metricName) {
            Timer
                .builder("${METRICS_NAMESPACE}_$metricName")
                .description("Timer for $metricName")
                .register(prometheusMeterRegistry)
        }
    }

    val counter: (metricName: String) -> Counter = { metricName ->
        counters.computeIfAbsent(metricName) {
            Counter
                .builder()
                .name("${METRICS_NAMESPACE}_$metricName")
                .help("Counts the number of $metricName")
                .withoutExemplars()
                .register(prometheusMeterRegistry.prometheusRegistry)
        }
    }
}
