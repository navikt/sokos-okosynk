package no.nav.sokos.okosynk.metrics

import java.util.concurrent.ConcurrentHashMap

import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.core.metrics.Counter

import no.nav.sokos.okosynk.domain.BatchType

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

    fun initMetrics() {
        BatchType.entries
            .filter { it != BatchType.UNKOWN }
            .forEach { batchType ->
                timer("batch_${batchType.opprettetAv}")
                counter("les_melding_${batchType.opprettetAv}")
                counter("ferdigstilt_oppgave_${batchType.opprettetAv}")
                counter("konvertert_oppgave_${batchType.opprettetAv}")
                counter("opprett_oppgave_${batchType.opprettetAv}")
                counter("ferdigstilt_oppgave_${batchType.opprettetAv}")
            }
    }
}
