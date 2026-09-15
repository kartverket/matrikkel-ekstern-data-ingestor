package no.kartverket.matrikkel


fun main() {
    Env.load("docker/local.env")
    runApplication(disableSecurity = true)
}