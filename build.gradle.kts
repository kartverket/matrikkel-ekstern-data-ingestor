plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
}

group = "no.kartverket.matrikkel"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "no.kartverket.matrikkel.MainKt"
}

kotlin {
    jvmToolchain(25)
}
dependencies {
    implementation(ktorLibs.server.cio)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.metrics.micrometer)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.callId)
    implementation(ktorLibs.server.statusPages)
    implementation(libs.slf4j)
    implementation(libs.logback.classic)
    implementation(libs.logstash)
    implementation(libs.ktorUtils)
    implementation(libs.kotlin.utils)
    implementation(libs.micrometerPrometheus)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
