package no.nav.sokos.okosynk.domain

enum class BatchType(val fileName: String, val oppgaveType: String, val opprettetAv: String) {
    OS("OS.INPUT", "OKO_OS", "okosynkos"),
    UR("UR.INPUT", "OKO_UR", "okosynkur"),
    UNKOWN("UNKNOWN", "UNKNOWN", "UNKNOWN"),
}

object BatchTypeContext {
    private val threadLocalBatchType = ThreadLocal<BatchType>()

    fun set(batchType: BatchType) = threadLocalBatchType.set(batchType)

    fun get(): BatchType = threadLocalBatchType.get() ?: BatchType.UNKOWN

    fun clear() = threadLocalBatchType.remove()
}
