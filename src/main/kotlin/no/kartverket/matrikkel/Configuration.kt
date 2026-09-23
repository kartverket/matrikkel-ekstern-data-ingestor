package no.kartverket.matrikkel

import io.ktor.http.Url
import no.kartverket.heimdall.common.kotlin.EnvUtils.getConfig
import no.kartverket.heimdall.common.tokenclient.client.DownstreamApi

data class Configuration(
    val version: String = getConfig("VERSION"),
    val kafkaBrokerUrl: Url = Url(getConfig("KAFKA_BROKER_URL")),
    val kafkaBrokerScope: DownstreamApi = DownstreamApi.parse(getConfig("KAFKA_BROKER_SCOPE")),
)