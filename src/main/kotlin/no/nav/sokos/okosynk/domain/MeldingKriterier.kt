package no.nav.sokos.okosynk.domain

import no.nav.sokos.okosynk.config.RegelverkConfig

data class MeldingKriterier(
    val gjelderId: String,
    val gjelderIdType: GjelderIdType,
    val ansvarligEnhetId: String? = null,
    val faggruppeEllerOppdragskode: String,
) {
    constructor(melding: Melding) : this(
        gjelderId = melding.gjelderId,
        gjelderIdType = GjelderIdType.value(melding.gjelderId),
        ansvarligEnhetId =
            when (melding) {
                is OsMelding -> RegelverkConfig.regelverkOSMap[melding.ruleKey()]?.ansvarligEnhetId
                is UrMelding -> RegelverkConfig.regelverkURMap[melding.ruleKey()]?.ansvarligEnhetId
                else -> null
            },
        faggruppeEllerOppdragskode =
            when (melding) {
                is OsMelding -> melding.faggruppe
                is UrMelding -> melding.oppdragsKode
                else -> ""
            },
    )
}
