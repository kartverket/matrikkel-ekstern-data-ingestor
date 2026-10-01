package no.kartverket.matrikkel

import com.google.inject.Injector
import no.statkart.matrikkel.config.MatrikkelServerModule
import no.statkart.matrikkel.config.MatrikkelenProperties
import no.statkart.skif.module.ModuleBuilder
import no.statkart.skif.persistence.ResourceManager
import no.statkart.skif.service.scope.ServiceRequestScope
import no.statkart.skif.store.Store
import org.junit.jupiter.api.Test

class SkifTest {

    val moduleBuilder: ModuleBuilder = ModuleBuilder()
    val configuration = MatrikkelenProperties.getInstance().getConfiguration()

    @Test
    fun `Test module sett opp module builder`() {
        Env.load("docker/local.env")


        moduleBuilder.setSingleVm(true)
        moduleBuilder.setUseSharedServer(true)
        moduleBuilder.setConfiguration(configuration)
        moduleBuilder.setModuleClass(MatrikkelServerModule::class.java)
        moduleBuilder.setSingleVmServerConfiguration(configuration)
        moduleBuilder.setSingleVmServerModuleClass(MatrikkelServerModule::class.java)
        val clientInjector: Injector = moduleBuilder.buildInjector()

        val scope = clientInjector.getInstance(ServiceRequestScope::class.java)

        scope.enter()
        try {
            val resourceManager = clientInjector.getInstance(ResourceManager::class.java) // name is a guess
            resourceManager.start()
            try {
                val store: Store = clientInjector.getInstance(Store::class.java)
                println("Store: $store")
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