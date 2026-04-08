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
    UNKNOWN("UNKNOWN", "UNKNOWN", "UNKNOWN", 0),
    ;

    fun getFileName(profile: PropertiesConfig.Profile): String =
        if (profile == PropertiesConfig.Profile.PROD) {
            rawFileName.lowercase()
        } else {
            rawFileName
        }
}
