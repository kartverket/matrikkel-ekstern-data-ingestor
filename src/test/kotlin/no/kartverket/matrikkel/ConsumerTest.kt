package no.kartverket.matrikkel

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
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
    fun `start etter feilet db transaksjon`() = testApplication {

    }

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

        val job = backgroundScope.launch(Dispatchers.IO) {
            client.consume(1.milliseconds) { }
        }

//        withContext(Dispatchers.IO) {
//        }
        delay(50.milliseconds)

        job.cancelAndJoin()

//        withContext(Dispatchers.IO) {
//            withTimeout(5.seconds) { job.cancelAndJoin() }
//        }

        assertThat(pollCount.get()).isGreaterThan(1)
        coVerify(exactly = 0) { client.commitSync() }
        coVerify(exactly = 1) { client.close() }
    }

    @Test
    fun `cancelAndJoin() fra en annen coroutine deadlocker ikke - mirrors ApplicationStopping`() = runTest {
        val client = mockk<MessageConsumer<String, String>>(relaxed = true)
        val pollCount = AtomicInteger(0)

        coEvery { client.poll() } coAnswers {
            pollCount.incrementAndGet()
            delay(10.milliseconds)
            ConsumerRecords(topic = "my-topic", records = emptyList())
        }

        val job = backgroundScope.launch(Dispatchers.IO) {
            client.consume(10.seconds) { }
        }

        withContext(Dispatchers.IO) {
            delay(50.milliseconds)
        }

        withContext(Dispatchers.IO) {
            withTimeout(5.seconds) { job.cancelAndJoin() }
        }

        assertThat(pollCount.get()).isGreaterThan(0)
        coVerify(exactly = 1) { client.close() }
    }
}