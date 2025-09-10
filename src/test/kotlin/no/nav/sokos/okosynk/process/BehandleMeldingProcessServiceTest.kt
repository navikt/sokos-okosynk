package no.nav.sokos.okosynk.process

import java.time.LocalDate

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

import no.nav.sokos.okosynk.TestData
import no.nav.sokos.okosynk.WireMockTestData.hentPersonWireMock
import no.nav.sokos.okosynk.domain.BatchType
import no.nav.sokos.okosynk.domain.BatchTypeContext
import no.nav.sokos.okosynk.domain.Melding
import no.nav.sokos.okosynk.domain.OsMelding
import no.nav.sokos.okosynk.domain.UrMelding
import no.nav.sokos.okosynk.integration.PdlClientService
import no.nav.sokos.okosynk.listener.WireMockListener
import no.nav.sokos.okosynk.listener.WireMockListener.wiremock
import no.nav.sokos.okosynk.util.Utils.readFromResource
import no.nav.sokos.okosynk.util.toDataClass

class BehandleMeldingProcessServiceTest :
    FunSpec({
        extensions(WireMockListener)

        val pdlClientService: PdlClientService by lazy {
            PdlClientService(
                pdlUrl = wiremock.baseUrl(),
                accessTokenClient = WireMockListener.accessTokenClient,
            )
        }

        val behandleMeldingProcessService: BehandleMeldingProcessService by lazy {
            BehandleMeldingProcessService(pdlClientService = pdlClientService)
        }

        beforeTest {
            BatchTypeContext.set(BatchType.OS)
        }

        test("behandleMeldingProcessService process should convert Melding to MeldingOppgave") {
            wiremock.resetAll()
            hentPersonWireMock()

            val osInput = "sftp/OS.INPUT".readFromResource()
            val osMeldingList = osInput.lines().map { it.toDataClass<OsMelding>() }
            osMeldingList.size shouldBe 20

            val meldingOppgaveSet = behandleMeldingProcessService.process(osMeldingList)

            meldingOppgaveSet.size shouldBe 11
            meldingOppgaveSet.toList()[0].behandlingstema shouldBe null
            meldingOppgaveSet.toList()[0].behandlingstype shouldBe "ae0216"
            meldingOppgaveSet.toList()[0].beskrivelse shouldBe "AVVE;;   1992kr;   beregningsdato/id:05.05.22/461386205;   periode:01.03.22-31.03.22;   feilkonto:" +
                ";   statusdato:05.05.22;   ;   UtbTil:03083734573;   H124085"
            meldingOppgaveSet.toList()[0].oppgavetype shouldBe "OKO_OS"
            meldingOppgaveSet.toList()[0].opprettetAvEnhetsnr shouldBe "9999"
            meldingOppgaveSet.toList()[0].orgnr shouldBe null
            meldingOppgaveSet.toList()[0].personIdent shouldBe null
            meldingOppgaveSet.toList()[0].aktoerId shouldBe "2305469522806"
            meldingOppgaveSet.toList()[0].tildeltEnhetsnr shouldBe "4819"

            WireMock.verify(11, WireMock.postRequestedFor(WireMock.urlEqualTo("/graphql")))
        }

        test("behandleMeldingProcessService process when hentIdenter returns exception ") {
            hentPersonWireMock(response = "pdl/hentPersonNotFoundResponse.json".readFromResource())

            val osInput = "sftp/OS.INPUT".readFromResource()
            val osMeldingList = osInput.lines().map { it.toDataClass<OsMelding>() }
            osMeldingList.size shouldBe 20

            val meldingOppgaveSet = behandleMeldingProcessService.process(osMeldingList)
            meldingOppgaveSet.shouldBeEmpty()
        }

        test("behandleMeldingProcessService process different gjelderIdType with UR.INPUT") {
            wiremock.resetAll()
            hentPersonWireMock()

            BatchTypeContext.set(BatchType.UR)

            val urInput = "sftp/UR.INPUT".readFromResource()
            val urMeldingList = urInput.lines().map { it.toDataClass<UrMelding>() }
            urMeldingList.size shouldBe 21

            val meldingOppgaveList = behandleMeldingProcessService.process(urMeldingList)
            meldingOppgaveList.size shouldBe 20
            WireMock.verify(19, WireMock.postRequestedFor(WireMock.urlEqualTo("/graphql")))
        }

        test("beskrivelse for a single OS Melding") {
            val melding: Melding = TestData.osMelding
            val result = melding.beskrivelse(listOf(melding))

            result shouldBe "AVAV;;   1000kr;   beregningsdato/id:01.10.23/123456789;   periode:01.10.23-31.10.23;   feilkonto:feil;   statusdato:02.10.23;   ;   UtbTil:123456789;   K124096"
        }

        test("beskrivelse for a list of OS Melding") {
            val melding1: Melding = TestData.osMelding
            val melding2: Melding =
                (TestData.osMelding as OsMelding).copy(
                    totalNettoBelop = 500.0,
                    fomPeriode = LocalDate.of(2022, 10, 1),
                    tomPeriode = LocalDate.of(2024, 10, 1),
                )

            val result = melding1.beskrivelse(listOf(melding1, melding2))

            result shouldBe "AVAV;;   1500kr;   beregningsdato/id:01.10.23/123456789;   periode:01.10.22-01.10.24;   feilkonto:feil;   statusdato:02.10.23;   ;   UtbTil:123456789;   K124096"
        }

        test("beskrivelse for a single UR Melding") {
            val melding: Melding = TestData.urMelding
            val result = melding.beskrivelse(listOf(melding))

            result shouldBe "25;;   KID ugyldig/mangler;   postert/bilagsnummer:01.10.23/793627889;   1000kr;   statusdato:02.10.23;   UtbTil:80000662771;   K124096"
        }

        test("beskrivelse for a list of ÚR Melding") {
            val melding1: Melding = TestData.urMelding
            val melding2: Melding = (TestData.urMelding as UrMelding).copy(totalNettoBelop = 300.0)
            val result = melding1.beskrivelse(listOf(melding1, melding2))

            result shouldBe "25;;   KID ugyldig/mangler;   postert/bilagsnummer:01.10.23/793627889;   1300kr;   statusdato:02.10.23;   UtbTil:80000662771;   K124096"
        }
    })
