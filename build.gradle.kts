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
    implementation(project(":tjenestespesifikasjoner:serg"))
    implementation(ktorLibs.server.cio)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.metrics.micrometer)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.callId)
    implementation(ktorLibs.server.statusPages)
    implementation(libs.slf4j)
    implementation(libs.logbackClassic)
    implementation(libs.logstash)
    implementation(libs.common.ktorUtils)
    implementation(libs.common.kotlinUtils)
    implementation(libs.common.tokenClient)
    implementation(libs.common.featureFlags)
    implementation(libs.micrometerPrometheus)
    implementation(libs.kafkaLight.client)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.bundles.testEcosystem)

}

tasks.test {
    useJUnitPlatform()
}
