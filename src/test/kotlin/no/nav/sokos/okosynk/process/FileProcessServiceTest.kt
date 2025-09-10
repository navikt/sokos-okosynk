package no.nav.sokos.okosynk.process

import FileProcessService
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.okosynk.domain.BatchType
import no.nav.sokos.okosynk.domain.BatchTypeContext
import no.nav.sokos.okosynk.domain.OsMelding
import no.nav.sokos.okosynk.domain.UrMelding
import no.nav.sokos.okosynk.exception.OppgaveException
import no.nav.sokos.okosynk.util.Utils.readFromResource

class FileProcessServiceTest :
    FunSpec({
        val fileProcessService = FileProcessService()

        test("process should map meldingList to OsMelding when BatchType is OS") {
            BatchTypeContext.set(BatchType.OS)
            val meldingList = "sftp/OS.INPUT".readFromResource().lines()

            val result = fileProcessService.process(meldingList)
            result.size shouldBe 20
            result.all { it is OsMelding } shouldBe true
        }

        test("process should map meldingList to UrMelding when BatchType is UR") {
            BatchTypeContext.set(BatchType.UR)
            val meldingList = "sftp/UR.INPUT".readFromResource().lines()

            val result = fileProcessService.process(meldingList)
            result.size shouldBe 21
            result.all { it is UrMelding } shouldBe true
        }

        test("process should throw OppgaveException for unknown BatchType") {
            BatchTypeContext.set(BatchType.UNKOWN)
            val meldingList = "sftp/OS.INPUT".readFromResource().lines()

            val exception =
                shouldThrow<OppgaveException> {
                    fileProcessService.process(meldingList)
                }

            exception.message shouldBe "Ukjent filname: UNKNOWN"
        }
    })
