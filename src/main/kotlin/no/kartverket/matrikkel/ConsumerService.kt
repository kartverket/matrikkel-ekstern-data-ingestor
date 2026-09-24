package no.kartverket.matrikkel

import io.ktor.server.application.*
import kotlinx.coroutines.*
import no.kartverket.matrikkel.kafkaclient.ConsumerRecord
import no.kartverket.matrikkel.kafkaclient.MessageConsumer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds


suspend fun <TKey, TValue> MessageConsumer<TKey, TValue>.consume(
    delayOnEmpty: Duration,
    onRecord: suspend (ConsumerRecord<TKey, TValue>) -> Unit,
) {
    this.use { consumer ->
        while (true) {
            currentCoroutineContext().ensureActive()

            val batch = consumer.poll()

            if (batch.records.isEmpty()) {
                delay(delayOnEmpty)
                continue
            }

            for (record in batch.records) {
                onRecord(record)
            }

            consumer.commitSync()
        }
    }
}

fun <TKey, TValue> Application.startConsumer(
    config: MessageConsumer.Config<TKey, TValue>,
    onRecord: suspend (ConsumerRecord<TKey, TValue>) -> Unit
) {

    val consumer: MessageConsumer<TKey, TValue> = MessageConsumer.Impl(config)
    val job = launch(Dispatchers.IO) {
        consumer.consume(10.seconds, onRecord)
    }

    monitor.subscribe(ApplicationStopping) {
        runBlocking { job.cancelAndJoin() }
    }
}
