package no.nav.sokos.okosynk.config

import java.util.Properties

import no.nav.sokos.okosynk.domain.Regelverk
import no.nav.sokos.okosynk.util.Utils.readFromResource

private const val OS_PROPERTIES_PATH = "properties/os_mapping_regler.properties"
private const val UR_PROPERTIES_PATH = "properties/ur_mapping_regler.properties"

object RegelverkConfig {
    val regelverkOSMap: Map<String, Regelverk> by lazy {
        initRegelverkMap(OS_PROPERTIES_PATH)
    }

    val regelverkURMap: Map<String, Regelverk> by lazy {
        initRegelverkMap(UR_PROPERTIES_PATH)
    }

    private fun initRegelverkMap(resourceFile: String): Map<String, Regelverk> {
        val properties = Properties()
        val content = resourceFile.readFromResource()
        properties.load(content.reader())

        return properties.map { (key, value) ->

            val (behandlingstema, behandlingstype, ansvarligEnhetId) =
                value.toString()
                    .split(",")
                    .map { it.takeIf { it.isNotBlank() } }

            val regelverk =
                Regelverk(
                    behandlingstema = behandlingstema,
                    behandlingstype = behandlingstype,
                    ansvarligEnhetId = ansvarligEnhetId,
                )
            key.toString() to regelverk
        }.toMap()
    }
}
