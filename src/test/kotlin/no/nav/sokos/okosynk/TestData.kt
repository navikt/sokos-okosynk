package no.nav.sokos.okosynk

import java.math.BigDecimal
import java.time.LocalDate

import no.nav.oppgave.models.OpprettOppgaveRequest
import no.nav.oppgave.models.OpprettOppgaveRequest.Prioritet
import no.nav.oppgave.models.PatchOppgaveRequest
import no.nav.sokos.okosynk.domain.GjelderIdType
import no.nav.sokos.okosynk.domain.Melding
import no.nav.sokos.okosynk.domain.MeldingOppgave
import no.nav.sokos.okosynk.domain.OsMelding
import no.nav.sokos.okosynk.domain.UrMelding
import no.nav.sokos.okosynk.integration.TEMA_OKONOMI_KODE

object TestData {
    val opprettOppgaveRequest =
        OpprettOppgaveRequest(
            aktivDato = LocalDate.of(2024, 9, 21),
            behandlingstema = "ab0270",
            behandlingstype = "ae0215",
            fristFerdigstillelse = LocalDate.of(2024, 9, 28),
            oppgavetype = "OKO_OS",
            opprettetAvEnhetsnr = "9999",
            orgnr = null,
            personident = "1000068345109",
            prioritet = Prioritet.LAV,
            tema = TEMA_OKONOMI_KODE,
            tildeltEnhetsnr = "4151",
            beskrivelse = "AVAV",
        )

    val patchOppgaveRequest =
        PatchOppgaveRequest(
            endretAvEnhetsnr = "9999",
            versjon = 1,
            status = PatchOppgaveRequest.Status.FERDIGSTILT,
        )

    val osMelding: Melding =
        OsMelding(
            gjelderId = "12345",
            beregningsid = "123456789",
            beregningsdato = LocalDate.parse("2023-10-01"),
            datoForStatus = LocalDate.parse("2023-10-02"),
            venteStatus = "AVAV",
            brukerId = "K124096",
            fomPeriode = LocalDate.parse("2023-10-01"),
            tomPeriode = LocalDate.parse("2023-10-31"),
            totalNettoBelop = BigDecimal("1000.00").toDouble(),
            flaggFeilKonto = "feil",
            behandlendeEnhet = "8020",
            faggruppe = "PEN",
            utbetalesTilId = "123456789",
            etteroppgjor = "",
        )

    val urMelding: Melding =
        UrMelding(
            gjelderId = "80000662771",
            brukerId = "K124096",
            gjelderIdType = GjelderIdType.ORGANISASJON.name,
            datoForStatus = LocalDate.parse("2023-10-02"),
            venteStatus = "25",
            totalNettoBelop = BigDecimal("1000.00").toDouble(),
            behandlendeEnhet = "8020",
            oppdragsKode = "KREDREF",
            kilde = "UR230",
            datoPostert = LocalDate.parse("2023-10-01"),
            bilagsId = "793627889",
            arsakTekst = "KID ugyldig/mangler",
            mottakerId = "80000662771",
        )

    val meldingOppgave =
        MeldingOppgave(
            behandlingstema = "ab0270",
            behandlingstype = "ae0215",
            tildeltEnhetsnr = "4151",
            oppgavetype = "OKO_OS",
            opprettetAvEnhetsnr = "9999",
            beskrivelse = "AVAV",
            personIdent = "1000068345109",
            aktoerId = null,
            orgnr = null,
        )
}
