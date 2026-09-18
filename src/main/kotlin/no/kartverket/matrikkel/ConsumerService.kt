package no.kartverket.matrikkel

import io.ktor.http.Url
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopping
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import no.kartverket.matrikkel.kafkaclient.ConsumerRecord
import no.kartverket.matrikkel.kafkaclient.InitialOffsetPolicy
import no.kartverket.matrikkel.kafkaclient.MessageConsumer
import no.kartverket.matrikkel.kafkaclient.StringSerde
import java.util.UUID
import javax.sql.DataSource
import kotlin.time.Duration.Companion.seconds

private val kafkaLog = applog

class SafeConsumer<TKey, TValue>(
    private val client: MessageConsumer<TKey, TValue>
) : AutoCloseable {
    private var running: Boolean = true
    private val closingCompleted = CompletableDeferred<Unit>()

    context(scope: CoroutineScope)
    fun consumeSafely(
        maxRecords: Int = 100,
        recordHandler: (ConsumerRecord<TKey, TValue>) -> Unit
    ) {
        scope.launch {
            while (running) {
                runCatching {
                    val response = client.poll(maxRecords = maxRecords)
                    response.records.forEach(recordHandler)
                    client.commitSync()
                }
            }
            client.close()
            closingCompleted.complete(Unit)
        }
    }

    fun stop() {
        running = false
    }

    suspend fun awaitClosed() {
        closingCompleted.await()
    }

    override fun close() {
        stop()
        runBlocking {
            awaitClosed()
        }
    }

    private fun DataSource.withTransaction(block: () -> Unit) {
        this.connection.autoCommit = false
        runCatching { block() }
            .onSuccess { this.connection.commit() }
            .onFailure { this.connection.rollback() }
            .getOrThrow()
    }
}


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

    val consumer: SafeConsumer<String, String> = SafeConsumer(MessageConsumer.Impl(kafkaConfig))

    monitor.subscribe(ApplicationStopping) { consumer.close() }

    launch(Dispatchers.IO) {
        consumer.consumeSafely(maxRecords = kafkaConfig.maxRecords) { record ->
            kafkaLog.info("Polled ${record.key} - ${record.value}")
        }
    }
}
