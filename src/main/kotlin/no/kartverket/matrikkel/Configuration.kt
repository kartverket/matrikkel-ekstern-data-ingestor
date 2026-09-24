package no.kartverket.matrikkel

import io.ktor.http.Url
import no.kartverket.heimdall.common.featureflags.FeatureToggle
import no.kartverket.heimdall.common.kotlin.EnvUtils.getConfig
import no.kartverket.heimdall.common.tokenclient.client.DownstreamApi

data class Configuration(
    val version: String = getConfig("VERSION"),
    val kafkaBrokerUrl: Url = Url(getConfig("KAFKA_BROKER_URL")),
    val kafkaBrokerScope: DownstreamApi = DownstreamApi.parse(getConfig("KAFKA_BROKER_SCOPE")),
    val environment: String = getConfig("ENVIRONMENT"),
)

enum class FeatureFlags(override val value: String) : FeatureToggle.Flag {
    ON_REDEPLOY_SERG_FORMUESOBJEKT("on-redeploy-serg-formuesobjekt"),
}

enum class Topics(val value: String)  {
    SERG_FORMUESOBJEKT("SERG_FORMUESOBJEKT_FAST_EIENDOM")
}