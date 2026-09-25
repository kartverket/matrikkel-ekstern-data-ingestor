import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.kotlinPluginSerialization)
}

dependencies {
    api(libs.bundles.kotlinxEcosystem)
    api(libs.bundles.ktorClientEcosystem)
    api(libs.okhttp)
    api(project(":tjenestespesifikasjoner:openapi-infrastructure"))
}

tasks.register<GenerateTask>("generateForFormueobjekt") {
    val specFile = file("openapi-formueobjekt.json")
    inputSpec = specFile
    outputDir =
        layout.buildDirectory
            .dir("generated/formueobjekt")
            .get()
            .asFile
    skipValidateSpec = true
    generateModelDocumentation = false
    generateModelTests = false
    generateApiDocumentation = false
    generateApiTests = false

    generatorName = "kotlin"
    library = "jvm-okhttp4"
    configOptions.put("dateLibrary", "java8")
    configOptions.put("serializationLibrary", "jackson")
    typeMappings.put("string+date-time", "LocalDateTime")
    apiPackage = "no.kartverket.tjenestespesifikasjoner.serg.formueobjekt.apis"
    modelPackage = "no.kartverket.tjenestespesifikasjoner.serg.formueobjekt.models"

    globalProperties.put("apis", "")
    globalProperties.put("models", "")
    globalProperties.put("supportingFiles", "false")
}

sourceSets {
    main {
        kotlin {
            srcDir(layout.buildDirectory.dir("generated/formueobjekt/src/main/kotlin"))
        }
    }
}

tasks.named("compileKotlin").configure {
    dependsOn(tasks.named("generateForFormueobjekt"))
}
