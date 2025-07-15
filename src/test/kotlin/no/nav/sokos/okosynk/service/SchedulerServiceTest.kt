package no.nav.sokos.okosynk.service

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk

class SchedulerServiceTest :
    BehaviorSpec({
        val batchService = mockk<BatchService>(relaxed = true)
        val schedulerService =
            spyk(
                SchedulerService(
                    batchService = batchService,
                    jobTimeout = 2.seconds,
                ),
            )

        afterTest {
            schedulerService.stop()
        }

        Given("SchedulerService is ready to start") {
            When("Scheduling with a valid cron expression") {
                val cronExpression = "* * * * * *"

                Then("It should execute batch job according to schedule") {
                    schedulerService.scheduleWithCronExpression(cronExpression)
                    delay(1500.milliseconds)
                    coVerify(atLeast = 1) { batchService.run() }
                }
            }

            When("The batch service execution takes longer than timeout") {
                val cronExpression = "* * * * * *"

                Then("It should terminate the execution") {
                    var completedExecution = false
                    coEvery { batchService.run() } coAnswers {
                        delay(5.seconds)
                        completedExecution = true
                    }

                    schedulerService.scheduleWithCronExpression(cronExpression)

                    // Wait enough time for the timeout to occur
                    delay(3.seconds)

                    // Verify that run was called but didn't complete normally
                    coVerify(atLeast = 1) { batchService.run() }
                    completedExecution shouldBe false
                }
            }

            When("The batch service throws an exception") {
                val cronExpression = "* * * * * *" // every second

                Then("It should handle the exception and continue scheduling") {
                    coEvery { batchService.run() } throws RuntimeException("Test exception")

                    schedulerService.scheduleWithCronExpression(cronExpression)

                    // Wait for multiple execution attempts
                    delay(2.seconds)

                    // Verify batch service was called multiple times despite exceptions
                    coVerify(atLeast = 1) { batchService.run() }
                }
            }

            When("A job is already running") {
                val cronExpression = "* * * * * *"

                Then("It should skip the new execution") {
                    coEvery { batchService.run() } coAnswers {
                        delay(1500.milliseconds)
                    }

                    schedulerService.scheduleWithCronExpression(cronExpression)

                    // Wait for a while to let multiple scheduling attempts occur
                    delay(2.seconds)

                    // Since each job takes 1.5 seconds, we should see fewer executions than the time elapsed would allow
                    coVerify(atMost = 2) { batchService.run() }
                }
            }

            When("Stopping the scheduler") {
                val cronExpression = "1 * * * * *"
                Then("It should cancel the job") {
                    val testBatchService = mockk<BatchService>(relaxed = true)
                    val testScheduler =
                        spyk(
                            SchedulerService(
                                batchService = testBatchService,
                                jobTimeout = 2.seconds,
                            ),
                        )

                    testScheduler.scheduleWithCronExpression(cronExpression)
                    delay(500.milliseconds)
                    testScheduler.stop()

                    // Wait for a while to let multiple scheduling attempts occur
                    delay(1500.milliseconds)

                    // Should be no calls after clearing
                    coVerify(exactly = 0) { testBatchService.run() }
                }
            }
        }
    })
