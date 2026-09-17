package no.kartverket.matrikkel

import no.kartverket.heimdall.common.ktor.utils.EnvUtils.getRequiredConfig
import no.kartverket.heimdall.common.ktor.utils.EnvUtils.getConfig

data class Configuration(
    val version: String = getRequiredConfig("VERSION"),
    val kafkaUrl: String? = getConfig("KAFKA_URL"),  // Ikke required for nå
)