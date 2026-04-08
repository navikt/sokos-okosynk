package no.nav.sokos.okosynk.process

import mu.KotlinLogging

import no.nav.sokos.okosynk.config.PropertiesConfig.configuration
import no.nav.sokos.okosynk.domain.BatchType
import no.nav.sokos.okosynk.domain.Melding
import no.nav.sokos.okosynk.domain.OsMelding
import no.nav.sokos.okosynk.domain.UrMelding
import no.nav.sokos.okosynk.exception.OppgaveException
import no.nav.sokos.okosynk.metrics.Metrics
import no.nav.sokos.okosynk.util.toDataClass

private val logger = KotlinLogging.logger {}

class FileProcessService : Chain<List<String>, List<Melding>> {
    override suspend fun process(
        batchType: BatchType,
        meldingList: List<String>,
    ): List<Melding> {
        val fileName = batchType.getFileName(configuration.profile)
        logger.info { "Start FileProcessService for filnavn: $fileName" }
        return when (batchType) {
            BatchType.OS -> {
                meldingList.map { it.toDataClass<OsMelding>() }
            }

            BatchType.UR -> {
                meldingList.map { it.toDataClass<UrMelding>() }
            }

            else -> {
                logger.error { "Ukjent filnavn: ${batchType.oppgaveType}" }
                throw OppgaveException("Ukjent filnavn: ${batchType.oppgaveType}")
            }
        }.also {
            logger.info { "Antall meldinger i $fileName: ${meldingList.size}" }
            Metrics.counter("les_melding_${batchType.opprettetAv}").inc(meldingList.size.toLong())
        }
    }
}
