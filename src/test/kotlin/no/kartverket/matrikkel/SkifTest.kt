package no.kartverket.matrikkel

import com.google.inject.ConfigurationException
import com.google.inject.Injector
import no.statkart.matrikkel.config.MatrikkelServerModule
import no.statkart.matrikkel.config.MatrikkelenProperties
import no.statkart.skif.module.ModuleBuilder
import no.statkart.skif.persistence.ResourceManager
import no.statkart.skif.service.scope.ServiceRequestScope
import no.statkart.skif.store.Store
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SkifTest {

    val moduleBuilder: ModuleBuilder = ModuleBuilder()
    val configuration = MatrikkelenProperties.getInstance().configuration
    lateinit var injector: Injector

    @BeforeAll
    fun setUp() {
        moduleBuilder.setSingleVm(true)
        moduleBuilder.setUseSharedServer(true)
        moduleBuilder.setConfiguration(configuration)
        moduleBuilder.setModuleClass(MatrikkelServerModule::class.java)
        moduleBuilder.setSingleVmServerConfiguration(configuration)
        moduleBuilder.setSingleVmServerModuleClass(MatrikkelServerModule::class.java)
        injector = moduleBuilder.buildInjector()
    }

    @Test
    fun `Test hent store fra injector`() {
        val scope = injector.getInstance(ServiceRequestScope::class.java)

        scope.enter()
        try {
            val resourceManager = injector.getInstance(ResourceManager::class.java) // name is a guess
            resourceManager.start()
            try {
                val store: Store = injector.getInstance(Store::class.java)
            } catch (e: ConfigurationException) {
                fail("Klarte ikke konfigurere store", e)
            } finally {
                resourceManager.shutdown()
            }
        } finally {
            scope.exit()
        }

    }

    @Test
    fun `Test get med Skif`() {

    }
}