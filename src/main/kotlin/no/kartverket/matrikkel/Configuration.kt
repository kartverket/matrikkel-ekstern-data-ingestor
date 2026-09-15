package no.kartverket.matrikkel

import no.kartverket.heimdall.common.ktor.utils.EnvUtils.getRequiredConfig

data class Configuration(
    val version: String = getRequiredConfig("VERSION"),
)