package no.nav.sokos.okosynk.domain

import java.time.LocalDate

const val FELTSEPARATOR = ";   "
const val FORSTE_FELTSEPARATOR = ";;   "

interface Melding {
    val gjelderId: String
    val datoForStatus: LocalDate?
    val venteStatus: String
    val brukerId: String
    val behandlendeEnhet: String
    val totalNettoBelop: Double

    fun sammenligningsDato(): LocalDate

    fun ruleKey(): String
}
