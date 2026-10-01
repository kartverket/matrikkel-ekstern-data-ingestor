pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()    // TODO: Fjern
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kartverket/matrikkel")
            credentials {
                username = System.getenv("GITHUB_ACTOR") ?: System.getenv("GITHUB_USER") ?: "token"
                password = System.getenv("PACKAGES_TOKEN") ?: System.getenv("KV_PACKAGES_PAT") ?: System.getenv("GH_PACKAGES_PAT")
            }
        }
        maven {
            name = "geotools"; url = uri("https://repo.osgeo.org/repository/release/")
            content {
                includeGroupByRegex ("org\\.geotools(\\..*)?") //includeGroupAndSubgroups
                includeGroupByRegex ("it\\.geosolutions(\\..*)?") //includeGroupAndSubgroups
                includeGroupByRegex ("org\\.eclipse\\.imagen(\\..*)?") //includeGroupAndSubgroups
                includeModule ("javax.media", "jai_core")
                includeModule ("javax.media", "jai_codec")
                includeModule ("javax.media", "jai_imageio") //org.geotools:gt-coverage
                includeModule ("org.apache.xml", "xml-commons-resolver") //org.geotools:gt-xml
                includeModule ("org.huldra.math", "bigint") //org.geotools:gt-coverage
                includeModule ("net.sf.json-lib", "json-lib") //geoserver
            }
        }
        maven {
            // repo for patch-versjoner releaset av TIBCO Software Inc.
            name = "jaspersoft"; url = uri("https://jaspersoft.jfrog.io/jaspersoft/jaspersoft-repo")
            content {
                includeGroup ("com.github.librepdf")
            }
        }

    }
    versionCatalogs {
        create("ktorLibs").from("io.ktor:ktor-version-catalog:3.6.0")
    }
}

include(":tjenestespesifikasjoner:openapi-infrastructure")
include(":tjenestespesifikasjoner:serg")

rootProject.name = "matrikkel-ekstern-data-ingestor"

