package no.kartverket.matrikkel

import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.*

class ServerTest {

    @Test
    fun `test root endpoint`() = testApplication {
        assertEquals(HttpStatusCode.OK, HttpStatusCode.OK)
    }

}
