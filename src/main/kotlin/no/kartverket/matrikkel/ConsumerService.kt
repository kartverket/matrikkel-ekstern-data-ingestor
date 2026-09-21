package no.kartverket.matrikkel

import io.ktor.http.*
import io.ktor.server.application.*
import kotlinx.coroutines.*
import no.kartverket.matrikkel.kafkaclient.ConsumerRecord
import no.kartverket.matrikkel.kafkaclient.InitialOffsetPolicy
import no.kartverket.matrikkel.kafkaclient.MessageConsumer
import no.kartverket.matrikkel.kafkaclient.StringSerde
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


suspend fun <TKey, TValue> MessageConsumer<TKey, TValue>.consume(
    delayDuration: Duration,
    onRecord: suspend (ConsumerRecord<TKey, TValue>) -> Unit,
) {
    this.use { consumer ->
        while (true) {
            currentCoroutineContext().ensureActive()

            val batch = consumer.poll()

            if (batch.records.isEmpty()) {
                delay(delayDuration)
                continue
            }

            for (record in batch.records) {
                onRecord(record)
            }

            consumer.commitSync()
        }
    }
}

fun Application.startConsumer(config: Configuration) {
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

    val consumer: MessageConsumer<String, String> = MessageConsumer.Impl(kafkaConfig)
    val job = launch(Dispatchers.IO) {
        consumer.consume(10.seconds) { record ->
            applog.info("Polled ${record.key} - ${record.value}")
        }
    }

    monitor.subscribe(ApplicationStopping) {
        runBlocking { job.cancelAndJoin() }
    }
}
