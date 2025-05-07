package no.nav.sokos.okosynk.domain

data class MeldingOppgave(
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val beskrivelse: String? = null,
    val oppgavetype: String,
    val opprettetAvEnhetsnr: String,
    val orgnr: String? = null,
    val personIdent: String? = null,
    val aktoerId: String? = null,
    val tildeltEnhetsnr: String? = null,
)
