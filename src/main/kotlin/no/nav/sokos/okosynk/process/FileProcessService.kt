import mu.KotlinLogging

import no.nav.sokos.okosynk.domain.BatchType
import no.nav.sokos.okosynk.domain.Melding
import no.nav.sokos.okosynk.domain.OsMelding
import no.nav.sokos.okosynk.domain.UrMelding
import no.nav.sokos.okosynk.exception.OppgaveException
import no.nav.sokos.okosynk.metrics.Metrics
import no.nav.sokos.okosynk.process.Chain
import no.nav.sokos.okosynk.service.BatchTypeContext
import no.nav.sokos.okosynk.util.toDataClass

private val logger = KotlinLogging.logger {}

class FileProcessService() : Chain<List<String>, List<Melding>> {
    override fun process(meldingList: List<String>): List<Melding> {
        val batchType = BatchTypeContext.get()!!

        logger.info { "Start FileProcessService " }
        return when (batchType.fileName) {
            BatchType.OS.fileName -> meldingList.map { it.toDataClass<OsMelding>() }
            BatchType.UR.fileName -> meldingList.map { it.toDataClass<UrMelding>() }
            else -> {
                logger.error { "Ukjent filnavn: ${batchType.oppgaveType}" }
                throw OppgaveException("Ukjent filname: ${batchType.oppgaveType}")
            }
        }.also {
            logger.info { "Antall meldinger i ${batchType.fileName}: ${meldingList.size}" }
            Metrics.counter("les_melding_${batchType.opprettetAv}").inc(meldingList.size.toLong())
        }
    }
}
