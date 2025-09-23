package no.nav.sokos.okosynk.service

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class SchedulerService(
    private val batchService: BatchService = BatchService(),
    private val jobTimeout: Duration = 30.minutes,
) {
    private lateinit var schedulerJob: Job

    private val scope = CoroutineScope(Dispatchers.Default)
    private val parser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING))
    private val isJobRunning = AtomicBoolean(false)

    fun scheduleWithCronExpression(cronExpression: String) {
        val cronDefinition = parser.parse(cronExpression)
        val executionTime = ExecutionTime.forCron(cronDefinition)

        schedulerJob =
            scope.launch {
                while (isActive) {
                    try {
                        val now = ZonedDateTime.now()
                        val nextExecution = executionTime.nextExecution(now).orElse(now)
                        val delayMillis = ChronoUnit.MILLIS.between(now, nextExecution)

                        logger.info {
                            val hours = delayMillis / 3_600_000
                            val minutes = (delayMillis / 60_000) % 60
                            val seconds = (delayMillis / 1000) % 60
                            "Next execution scheduled at: $nextExecution (in ${hours}h ${minutes}m ${seconds}s)"
                        }
                        delay(delayMillis.coerceAtLeast(0))

                        if (isJobRunning.compareAndSet(false, true)) {
                            logger.debug { "Starting batch job execution" }
                            withTimeoutOrNull(jobTimeout.absoluteValue) {
                                batchService.run()
                            } ?: throw TimeoutException("Job execution exceeded timeout of $jobTimeout")
                        } else {
                            logger.warn { "Skipping scheduled execution because previous job is still running" }
                        }
                    } catch (e: CancellationException) {
                        logger.error(e) { "Batch job cancelled" }
                        break
                    } catch (e: TimeoutException) {
                        logger.error(e) { "Batch job timed out after $jobTimeout" }
                    } catch (e: Exception) {
                        logger.error(e) { "Error executing batch job" }
                        delay(1000)
                    }
                }
            }
        logger.info { "Batch job scheduler started with cron expression: $cronExpression" }
    }

    fun stop() {
        schedulerJob.cancel()
        logger.info { "Batch job scheduler stopped" }
    }
}
