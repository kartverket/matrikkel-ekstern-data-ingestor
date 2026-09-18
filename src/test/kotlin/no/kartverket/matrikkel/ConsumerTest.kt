package no.kartverket.matrikkel

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
    fun `consumeSafely poller ett record og gjør commitSync`() {
        val client = mockk<MessageConsumer<String, String>>(relaxed = true)
        val handled = CompletableDeferred<ConsumerRecord<String, String>>()

        val record = ConsumerRecord(
            topic = "my-topic",
            sequence = 1L,
            key = "key",
            value = "value",
            publishedAt = Instant.fromEpochMilliseconds(0),
        )

        coEvery { client.poll(any(), any()) } returns ConsumerRecords(topic = "my-topic", records = listOf(record))

//        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val result = runBlocking {
            val consumer = SafeConsumer(
                scope = this,
                consumerFactory = { client },
                maxRecords = 1,
            ) { handled.complete(it) }

            consumer.start()

            withTimeout(5.seconds) { handled.await() }
            this.cancel()
        }

        // stop() is called from this external test coroutine, never from inside the
        // mock's answers block, so there's no risk of the reentrancy deadlock we hit
        // before - cancelAndJoin() suspends cooperatively until the job's finally block
        // (which calls client.close()) has actually run.
//        consumer.stop()
//        withTimeout(5.seconds) { consumer.stop() }

        assertThat(result).isEqualTo(record)
        coVerify(atLeast = 1) { client.poll(1, any()) }
        coVerify(atLeast = 1) { client.commitSync() }
        coVerify(exactly = 1) { client.close() }
    }

    @Test
    fun `stop() from a different coroutine does not deadlock - mirrors ApplicationStopping`() {
        // This exercises the actual production call path: the consumer loop running on
        // its own scope/dispatcher, and stop() invoked independently - exactly like
        // `monitor.subscribe(ApplicationStopping) { consumer.stop() }` does.
        val client = mockk<MessageConsumer<String, String>>(relaxed = true)
        val pollCount = AtomicInteger(0)

        coEvery { client.poll(any(), any()) } coAnswers {
            pollCount.incrementAndGet()
            delay(10.milliseconds) // simulate real I/O latency, like the actual Kafka client
            ConsumerRecords(topic = "my-topic", records = emptyList())
        }

        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val consumer = SafeConsumer(
            scope = scope,
            consumerFactory = { client },
            maxRecords = 1,
        ) { }

        runBlocking {
            consumer.start()

            // Give the loop a moment to actually start polling.
            delay(50.milliseconds)

            // stop() is called from this separate coroutine - if it ever deadlocked
            // again, this withTimeout would fail fast instead of hanging the suite.
            withTimeout(5.seconds) { consumer.stop() }
        }

        assertThat(pollCount.get()).isGreaterThan(0)
        coVerify(exactly = 1) { client.close() }
    }
}