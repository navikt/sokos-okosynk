import mu.KotlinLogging

import no.nav.sokos.okosynk.process.Chain

private val logger = KotlinLogging.logger {}

class FileProcessService() : Chain<String, String> {
    override fun process(data: String): String {
        return ""
    }
}
