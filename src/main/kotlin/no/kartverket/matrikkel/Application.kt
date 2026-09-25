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
import no.kartverket.heimdall.common.featureflags.FeatureToggle
import no.kartverket.heimdall.common.ktor.plugins.Metrics
import no.kartverket.heimdall.common.ktor.plugins.selftest.Selftest
import no.kartverket.heimdall.common.ktor.utils.KtorServer
import no.kartverket.heimdall.common.tokenclient.TokenClientFactory
import no.kartverket.matrikkel.config.JsonSerde
import no.kartverket.matrikkel.kafkaclient.InitialOffsetPolicy
import no.kartverket.matrikkel.kafkaclient.LongSerde
import no.kartverket.matrikkel.kafkaclient.MessageConsumer
import no.kartverket.matrikkel.utils.asKafkaAuth
import no.kartverket.tjenestespesifikasjoner.serg.formueobjekt.models.FastEiendomSomFormuesobjekt
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

val logger = LoggerFactory.getLogger("matrikkel-ekstern-data-ingestor")
fun runApplication(disableSecurity: Boolean = false) {
    val config = Configuration()

    KtorServer.create(factory = CIO, port = 8050) {
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                logger.error("Uncaught exception", cause)
                call.respond(HttpStatusCode.InternalServerError, "Uncaught exception")
            }
        }

        install(CallId) {
            header(HttpHeaders.XRequestId)
            generate { Uuid.random().toString() }
        }

        install(CallLogging) {
            this.logger = no.kartverket.matrikkel.logger
            disableDefaultColors()
            filter { call -> call.request.path().contains("/internal/").not() }
            mdc("RequestId") { it.callId }
        }

        install(Metrics.Plugin)
        install(Selftest.Plugin) {
            this.appname = "matrikkel-ekstern-data-ingestor"
            this.version = config.version
        }


        val sergFormuesobjektConfig = MessageConsumer.Config(
            server = config.kafkaBrokerUrl,
            topic = Topics.SERG_FORMUESOBJEKT.value,
            authentication = TokenClientFactory.MachineToMachine.azureAd()
                .asKafkaAuth(config.kafkaBrokerScope),
            keySerializer = LongSerde,
            valueSerializer = JsonSerde<FastEiendomSomFormuesobjekt>(),
            correlationIdProvider = { UUID.randomUUID().toString() },
            maxRetries = 3,
            consumerGroup = "matrikkel-ekstern-data-ingestor",
            instanceId = UUID.randomUUID().toString(),
            timeout = 10.seconds,
            maxRecords = 100,
            initialOffsetPolicy = InitialOffsetPolicy.LATEST,
        )

        val ctxProvider = FeatureToggle.ContextProvider {
            mapOf(
                FeatureToggle.CtxKeys.ENVIRONMENT to config.environment,
            )
        }

        val posthogService = when (config.environment) {
            "local" -> FeatureToggle.MockImpl()
            else -> FeatureToggle.remoteEvaluation(globalContextProvider = ctxProvider)
        }

        if (posthogService.isActive(FeatureFlags.ON_REDEPLOY_SERG_FORMUESOBJEKT)) {
            startConsumer(sergFormuesobjektConfig) { record ->
                logger.info("Polled ${record.key} - ${record.value}")
            }
        }
        posthogService.close()

    }.start(wait = true)
}

