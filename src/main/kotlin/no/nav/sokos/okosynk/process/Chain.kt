package no.nav.sokos.okosynk.process

import no.nav.sokos.okosynk.domain.BatchType

interface Chain<I, O> {
    suspend fun process(
        batchType: BatchType,
        data: I,
    ): O
}
