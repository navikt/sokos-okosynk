package no.nav.sokos.okosynk.domain

import no.nav.sokos.okosynk.util.AktoerUtil.isBnr

private const val TSS_PREFIX_1 = '8'
private const val TSS_PREFIX_2 = '9'
private const val ORGANISASJONSNUMMER_LENGDE = 9

enum class GjelderIdType {
    ORGANISASJON,
    BNR,
    AKTORID,
    SAMHANDLER,
    ;

    companion object {
        fun value(gjelderId: String): GjelderIdType {
            return when {
                gjelderId.length == ORGANISASJONSNUMMER_LENGDE -> ORGANISASJON
                gjelderId.firstOrNull() in listOf(TSS_PREFIX_1, TSS_PREFIX_2) -> SAMHANDLER
                gjelderId.isBnr() -> BNR
                else -> AKTORID
            }
        }
    }
}
