package no.kartverket.matrikkel

import no.kartverket.heimdall.common.kotlin.EnvUtils.getConfig

data class Configuration(
    val version: String = getConfig("VERSION"),
    val kafkaUrl: String = getConfig("KAFKA_URL"),
    val environment: String = getConfig("ENVIRONMENT"),
)