package no.nav.sokos.okosynk.domain

import no.nav.sokos.okosynk.config.PropertiesConfig

enum class BatchType(
    private val rawFileName: String,
    val oppgaveType: String,
    val opprettetAv: String,
    val antallDagerFrist: Long,
) {
    OS("OS.INPUT", "OKO_OS", "okosynkos", 7),
    UR("UR.INPUT", "OKO_UR", "okosynkur", 3),
    UNKOWN("UNKNOWN", "UNKNOWN", "UNKNOWN", 0),
    ;

    val fileName: String
        get() =
            if (PropertiesConfig.Configuration().profile == PropertiesConfig.Profile.PROD) {
                rawFileName.lowercase()
            } else {
                rawFileName
            }
}

object BatchTypeContext {
    private val threadLocalBatchType = ThreadLocal<BatchType>()

    fun set(batchType: BatchType) = threadLocalBatchType.set(batchType)

    fun get(): BatchType = threadLocalBatchType.get() ?: BatchType.UNKOWN

    fun clear() = threadLocalBatchType.remove()
}
