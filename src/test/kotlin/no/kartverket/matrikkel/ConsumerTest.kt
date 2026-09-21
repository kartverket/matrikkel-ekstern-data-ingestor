package no.kartverket.matrikkel

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import no.kartverket.matrikkel.kafkaclient.ConsumerRecord
import no.kartverket.matrikkel.kafkaclient.ConsumerRecords
import no.kartverket.matrikkel.kafkaclient.MessageConsumer
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ConsumerTest {

    @Test
    fun `consume poller ett record og gjør commitSync`() = runTest {
        val client = mockk<MessageConsumer<String, String>>(relaxed = true)
        val handled = CompletableDeferred<ConsumerRecord<String, String>>()

        val record = ConsumerRecord(
            topic = "my-topic",
            sequence = 1L,
            key = "key",
            value = "value",
            publishedAt = Instant.fromEpochMilliseconds(0),
        )

        coEvery { client.poll() } returns ConsumerRecords(topic = "my-topic", records = listOf(record))

        val job = launch(Dispatchers.IO) {
            client.consume(10.seconds) { handled.complete(it) }
        }
        val received = handled.await()
        job.cancelAndJoin()

        assertThat(received).isEqualTo(record)
        coVerify(atLeast = 1) { client.poll() }
        coVerify(atLeast = 1) { client.commitSync() }
        coVerify(exactly = 1) { client.close() }
    }

    @Test
    fun `consume venter og prøver igjen ved tom batch, uten å committe`() = runTest {
        val client = mockk<MessageConsumer<String, String>>(relaxed = true)
        val pollCount = AtomicInteger(0)

        coEvery { client.poll() } coAnswers {
            pollCount.incrementAndGet()
            ConsumerRecords(topic = "my-topic", records = emptyList())
        }

        val job = launch {
            client.consume(1.milliseconds) { }
        }

        delay(50.milliseconds)

        job.cancelAndJoin()

        assertThat(pollCount.get()).isGreaterThan(1)
        coVerify(exactly = 0) { client.commitSync() }
        coVerify(exactly = 1) { client.close() }
    }

    @Test
    fun `kan bruke cancelAndJoin() fra en annen coroutine mens consume er i en delay`() = runTest {
        val client = mockk<MessageConsumer<String, String>>(relaxed = true)
        val pollCount = AtomicInteger(0)

        coEvery { client.poll() } coAnswers {
            pollCount.incrementAndGet()
            ConsumerRecords(topic = "my-topic", records = emptyList())
        }

        val job = launch(Dispatchers.IO) {
            client.consume(10.seconds) { }
        }
        withContext(Dispatchers.IO) {
            delay(50.milliseconds)
        }

        launch {
            withTimeout(5.seconds) { job.cancelAndJoin() }
        }
        delay(50.milliseconds)

        assertThat(pollCount.get()).isGreaterThan(0)
        coVerify(exactly = 1) { client.close() }
    }
}