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
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.server.cio)
    implementation(ktorLibs.server.core)
    implementation(libs.logback.classic)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
}
