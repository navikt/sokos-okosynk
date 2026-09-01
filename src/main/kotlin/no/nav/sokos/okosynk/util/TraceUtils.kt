package no.nav.sokos.okosynk.util

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.context.Context
import org.slf4j.MDC
// import io.opentelemetry.instrumentation.api.incubator.log.LoggingContextConstants

object TraceUtils {
    // Matcher io.opentelemetry.instrumentation.api.incubator.log.LoggingContextConstants,
    // som er et ustabilt alpha-API og derfor ikke brukes direkte som avhengighet.
    private const val TRACE_ID = "trace_id"
    private const val SPAN_ID = "span_id"

    private val openTelemetry = GlobalOpenTelemetry.get()

    suspend fun <T> withTracerId(
        tracer: Tracer = openTelemetry.getTracer(this::class.java.canonicalName),
        spanName: String = "withTracerId",
        block: suspend () -> T,
    ): T {
        val span = tracer.spanBuilder(spanName).startSpan()
        val context = span.spanContext

        // Make the span the current active span in the context
        return Context.current().with(span).makeCurrent().use { scope ->
            try {
                MDC.put(TRACE_ID, context.traceId)
                MDC.put(SPAN_ID, context.spanId)
//                MDC.put(LoggingContextConstants.TRACE_ID, context.traceId)
//                MDC.put(LoggingContextConstants.SPAN_ID, context.spanId)

                val result = block()
                span.setStatus(StatusCode.OK)
                result
            } catch (e: Exception) {
                span.setStatus(StatusCode.ERROR, e.message ?: "Unknown error")
                span.recordException(e)
                throw e
            } finally {
                MDC.remove(TRACE_ID)
                MDC.remove(SPAN_ID)
//                MDC.remove(LoggingContextConstants.TRACE_ID)
//                MDC.remove(LoggingContextConstants.SPAN_ID)
                span.end()
            }
        }
    }
}
