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
    implementation(libs.bundles.matrikkel)

    implementation("no.statkart.matrikkel:feature-flags:local-SNAPSHOT")
    implementation("no.statkart.matrikkel:configuration:local-SNAPSHOT")
    implementation("no.statkart.matrikkel:sk-querydsl-codegen:local-SNAPSHOT")
    implementation("no.statkart.matrikkel:mat-project-apt-annotations:local-SNAPSHOT")
    implementation("no.statkart.matrikkel:oidc-token-client:local-SNAPSHOT")
    implementation("no.statkart.matrikkel:util:local-SNAPSHOT")
    implementation("no.statkart.matrikkel:matrikkel-build-info:local-SNAPSHOT") // gjøre den her annerledes
    implementation("no.statkart.matrikkel:dokument-generering:local-SNAPSHOT") // gjøre den her annerledes
    implementation("no.statkart.matrikkel:openapi-infra:local-SNAPSHOT") // gjøre den her annerledes
    implementation("no.statkart.matrikkel:metrics:local-SNAPSHOT") // gjøre den her annerledes
    implementation("no.statkart.matrikkel:jdbc-query:local-SNAPSHOT") // gjøre den her annerledes
    implementation("no.statkart.matrikkel:egenregistrert-bygningsdata:local-SNAPSHOT") // gjøre den her annerledes
    implementation("no.statkart.matrikkel:okhttp-utils:local-SNAPSHOT") // gjøre den her annerledes
    implementation("no.statkart.matrikkel:rapporter:local-SNAPSHOT") // gjøre den her annerledes

    implementation (platform(libs.tomeePlusBom))
    compileOnly (platform("org.apache.tomee:jakartaee-api"))
    implementation ("jakarta.ejb:jakarta.ejb-api")
    implementation ("jakarta.interceptor:jakarta.interceptor-api")


    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.bundles.testEcosystem)

}
configurations.all {
    resolutionStrategy {
        force("no.statkart.matrikkel:feature-flags:local-SNAPSHOT")
        force("no.statkart.matrikkel:configuration:local-SNAPSHOT")
        force("no.statkart.matrikkel:sk-querydsl-codegen:local-SNAPSHOT")
        force("no.statkart.matrikkel:mat-project-apt-annotations:local-SNAPSHOT")
        force("no.statkart.matrikkel:oidc-token-client:local-SNAPSHOT")
        force("no.statkart.matrikkel:util:local-SNAPSHOT")
        force("no.statkart.matrikkel:matrikkel-build-info:local-SNAPSHOT")
        force("no.statkart.matrikkel:dokument-generering:local-SNAPSHOT")
        force("no.statkart.matrikkel:openapi-infra:local-SNAPSHOT")
        force("no.statkart.matrikkel:metrics:local-SNAPSHOT")
        force("no.statkart.matrikkel:jdbc-query:local-SNAPSHOT")
        force("no.statkart.matrikkel:egenregistrert-bygningsdata:local-SNAPSHOT")
        force("no.statkart.matrikkel:okhttp-utils:local-SNAPSHOT")
        force("no.statkart.matrikkel:rapporter:local-SNAPSHOT")
    }
}

tasks.test {
    useJUnitPlatform()
}
