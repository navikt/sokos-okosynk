package no.nav.sokos.okosynk.util

/**
 * MDC keys used when injecting the current trace/span id into log entries.
 *
 * This mirrors the constants from the alpha `io.opentelemetry.instrumentation.api.incubator.log.LoggingContextConstants`
 * class. It is duplicated locally so this project can depend on stable OpenTelemetry artifacts
 * (`opentelemetry-api`/`opentelemetry-context`) instead of the alpha `opentelemetry-instrumentation-api-incubator`
 * module, which is only pulled in transitively by the (also alpha) `opentelemetry-ktor-3.0` instrumentation library.
 */
object LoggingContextConstants {
    /** Key under which the current trace id will be injected into the context data. */
    const val TRACE_ID = "trace_id"

    /** Key under which the current span id will be injected into the context data. */
    const val SPAN_ID = "span_id"
}
