package no.nav.sokos.okosynk.config

import java.io.File

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

object PropertiesConfig {
    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "NAIS_APP_NAME" to "sokos-okosynk",
                "NAIS_NAMESPACE" to "okonomi",
                "SCHEDULER_ENABLED" to "true",
            ),
        )

    private val localDevProperties =
        ConfigurationMap(
            mapOf(
                "APPLICATION_PROFILE" to Profile.LOCAL.toString(),
                "SCHEDULER_CRON_EXPRESSION" to "0 * * * * *",
            ),
        )

    private val devProperties = ConfigurationMap(mapOf("APPLICATION_PROFILE" to Profile.DEV.toString()))
    private val prodProperties = ConfigurationMap(mapOf("APPLICATION_PROFILE" to Profile.PROD.toString()))

    private val config =
        when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
            "dev-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding devProperties overriding defaultProperties
            "prod-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding prodProperties overriding defaultProperties
            else ->
                ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding
                    ConfigurationProperties.fromOptionalFile(
                        File("defaults.properties"),
                    ) overriding localDevProperties overriding defaultProperties
        }

    operator fun get(key: String): String = config[Key(key, stringType)]

    fun getOrEmpty(key: String): String = config.getOrElse(Key(key, stringType), "")

    data class Configuration(
        val naisAppName: String = get("NAIS_APP_NAME"),
        val profile: Profile = Profile.valueOf(get("APPLICATION_PROFILE")),
        val azureAdProperties: AzureAdProperties = AzureAdProperties(),
    )

    class AzureAdProperties(
        val clientId: String = getOrEmpty("AZURE_APP_CLIENT_ID"),
        val wellKnownUrl: String = getOrEmpty("AZURE_APP_WELL_KNOWN_URL"),
        val tenantId: String = getOrEmpty("AZURE_APP_TENANT_ID"),
        val clientSecret: String = getOrEmpty("AZURE_APP_CLIENT_SECRET"),
    )

    data class SchedulerProperties(
        val enabled: Boolean = get("SCHEDULER_ENABLED").toBoolean(),
        val cronExpression: String = getOrEmpty("SCHEDULER_CRON_EXPRESSION"),
    )

    data class SftpProperties(
        val host: String = getOrEmpty("SFTP_SERVER"),
        val username: String = getOrEmpty("SFTP_USERNAME"),
        val privateKey: String = getOrEmpty("SFTP_PRIVATE_KEY"),
        val port: Int = getOrEmpty("SFTP_PORT").toInt(),
    )

    data class PdlProperties(
        val pdlUrl: String = getOrEmpty("PDL_URL"),
        val pdlScope: String = getOrEmpty("PDL_SCOPE"),
    )

    data class OppgaveProperties(
        val oppgaveUrl: String = getOrEmpty("OPPGAVE_URL"),
        val oppgaveScope: String = getOrEmpty("OPPGAVE_SCOPE"),
    )

    enum class Profile {
        LOCAL,
        DEV,
        PROD,
    }
}
