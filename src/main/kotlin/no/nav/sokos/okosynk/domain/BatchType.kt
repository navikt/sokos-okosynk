package no.nav.sokos.okosynk.domain

enum class BatchType(val fileName: String, val oppgaveType: String, val opprettetAv: String) {
    OS("OS.INPUT", "OKO_OS", "okosynkos"),
    UR("UR.INPUT", "OKO_UR", "okosynkur"),
    UNKOWN("UNKNOWN", "UNKNOWN", "UNKNOWN"),
}
