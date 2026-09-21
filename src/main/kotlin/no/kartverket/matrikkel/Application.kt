package no.kartverket.matrikkel

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callId
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.path
import io.ktor.server.response.respond
import no.kartverket.heimdall.common.ktor.plugins.Metrics
import no.kartverket.heimdall.common.ktor.plugins.selftest.Selftest
import no.kartverket.heimdall.common.ktor.utils.KtorServer
import org.slf4j.LoggerFactory
import kotlin.uuid.Uuid

val applog = LoggerFactory.getLogger("matrikkel-ekstern-data-ingestor")
fun runApplication(disableSecurity: Boolean = false) {
    val config = Configuration()

    KtorServer.create(factory = CIO, port = 8050) {
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                applog.error("Uncaught exception", cause)
                call.respond(HttpStatusCode.InternalServerError, "Uncaught exception")
            }
        }

        install(CallId) {
            header(HttpHeaders.XRequestId)
            generate { Uuid.random().toString() }
        }

        install(CallLogging) {
            logger = applog
            disableDefaultColors()
            filter { call -> call.request.path().contains("/internal/").not() }
            mdc("RequestId") { it.callId }
        }

        install(Metrics.Plugin)
        install(Selftest.Plugin) {
            this.appname = "matrikkel-ekstern-data-ingestor"
            this.version = config.version
        }
        configureRouting()
        startConsumer(config)

    }.start(wait = true)
}

