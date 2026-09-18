package no.kartverket.matrikkel

import io.ktor.http.Url
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import io.ktor.utils.io.ReaderScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.kartverket.matrikkel.kafkaclient.ConsumerRecord
import no.kartverket.matrikkel.kafkaclient.InitialOffsetPolicy
import no.kartverket.matrikkel.kafkaclient.MessageConsumer
import no.kartverket.matrikkel.kafkaclient.StringSerde
import java.util.UUID
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds

private val kafkaLog = applog

class SafeConsumer<TKey, TValue>(
    private val scope: CoroutineScope,
    private val consumerFactory: () -> MessageConsumer<TKey, TValue>,
    private val maxRecords: Int = 100,
    private val recordHandler: suspend (ConsumerRecord<TKey, TValue>) -> Unit,
) {
    private val mutex = Mutex()
    private var job: Job? = null

    suspend fun start() = mutex.withLock {
        if (job?.isActive == true) return@withLock

        job = scope.launch(Dispatchers.IO) {
            val client = consumerFactory()

            client.use {
                while (isActive) {
                    runCatching {
                        val response = client.poll(maxRecords = maxRecords)
                        for (record in response.records) {
                            recordHandler(record)
                        }
                        client.commitSync()
                    }
                }
            }
        }
    }

    // Cancelling the job (rather than a manual flag) makes shutdown cooperative:
    // it interrupts the suspended poll() call directly, and cancelAndJoin() suspends
    // (instead of blocking a thread) until the finally block has closed the client.
    // Must be called from a different coroutine than the running job itself, or
    // cancelAndJoin() will hang waiting on its own completion.
    suspend fun stop() = mutex.withLock {
        job?.cancelAndJoin()
        job = null
    }

    private fun DataSource.withTransaction(block: () -> Unit) {
        this.connection.autoCommit = false
        runCatching { block() }
            .onSuccess { this.connection.commit() }
            .onFailure { this.connection.rollback() }
            .getOrThrow()
    }
}

fun <TKey, TValue> CoroutineScope.launchSafeConsumer(
    consumerFactory: () -> MessageConsumer<TKey, TValue>,
    maxRecords: Int = 100,
    recordHandler: suspend (ConsumerRecord<TKey, TValue>) -> Unit,
): SafeConsumer<TKey, TValue> =
    SafeConsumer(
        scope = this,
        consumerFactory = consumerFactory,
        maxRecords = maxRecords,
        recordHandler = recordHandler,
    ).also {
        runBlocking { it.start() }
    }

context(scope: CoroutineScope)
fun Application.configureConsumer(config: Configuration) {
    val kafkaConfig = MessageConsumer.Config(
        server = Url(config.kafkaUrl ?: ""),
        topic = "my-topic",
        keySerializer = StringSerde,
        valueSerializer = StringSerde,
        correlationIdProvider = { UUID.randomUUID().toString() },
        maxRetries = 3,
        consumerGroup = "my-consumer-group",
        instanceId = "my-instance-1",
        timeout = 10.seconds,
        maxRecords = 100,
        initialOffsetPolicy = InitialOffsetPolicy.LATEST,
    )

    val consumer = scope.launchSafeConsumer(
        consumerFactory = { MessageConsumer.Impl(kafkaConfig) },
        maxRecords = kafkaConfig.maxRecords,
    ) { record ->
        kafkaLog.info("Polled ${record.key} - ${record.value}")
    }

    monitor.subscribe(ApplicationStopping) {
        runBlocking { consumer.stop() }
    }
}
