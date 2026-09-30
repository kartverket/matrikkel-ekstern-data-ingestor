package no.kartverket.matrikkel

import com.google.inject.Injector
import no.kartverket.heimdall.common.kotlin.EnvUtils.getConfig
import no.statkart.matrikkel.config.MatrikkelStoreConfigurationModule
import no.statkart.matrikkel.service.MatrikkelServiceContext
import no.statkart.skif.SkifModule
import no.statkart.skif.config.MapConfiguration
import no.statkart.skif.exception.ImplementationException
import no.statkart.skif.module.ModuleBuilder
import no.statkart.skif.module.ModuleConfiguration
import no.statkart.skif.service.module.server.RunOnServerServiceModule
import no.statkart.skif.service.module.server.ServerModule
import no.statkart.skif.store.Store
import no.statkart.skif.store.service.StoreService
import no.statkart.skif.store.service.StoreServiceImpl
import org.junit.jupiter.api.Test

class SkifTest {

    val moduleBuilder : ModuleBuilder = ModuleBuilder()
    val configuration  = MapConfiguration()
//    val store : Store = no.statkart.matrikkel.store.MatrikkelStoreServer()

    class IngestorServerModule (moduleConfiguration: ModuleConfiguration ) : SkifModule(moduleConfiguration) {

        override fun configure() {
            install(ServerModule(moduleConfiguration).setServiceContextClass(MatrikkelServiceContext::class.java))
            install(RunOnServerServiceModule(moduleConfiguration))
            install(object:  MatrikkelStoreConfigurationModule(moduleConfiguration, false) {
                override fun getJtaDataSourceName(): String {
                    return getConfig("DATASOURCE_JTA")
                }

                override fun getOldJtaDataSourceName(): String {
                    throw ImplementationException("Should not be in use")
                }


                override fun getJdbcUrl(): String {
                    return getConfig("JDBC_URL")
                }

                override fun getDatabaseUsername(): String {
                    return getConfig("DB_USERNAME")
                }

                override fun getDatabasePassword(): String {
                    return getConfig("DB_PASSWORD")
                }

                override fun getHistorikkJtaDataSourceName(): String {
                    throw ImplementationException("Should not be in use")
                }

                override fun getHistoriskDatabasePassword(): String {
                    throw ImplementationException("Should not be in use")
                }

                override fun getHistoriskDatabaseSchema(): String {
                    throw ImplementationException("Should not be in use")
                }

                override fun getHistorikkJdbcUrl(): String {
                    throw ImplementationException("Should not be in use")
                }
            })

            bind<StoreService?>(StoreService::class.java).to(StoreServiceImpl::class.java)

        }
    }

    @Test
    fun `Test module sett opp module builder`() {
        Env.load("docker/local.env")

//        configuration.addPropertyDirect()

        moduleBuilder.setSingleVm(true)
        moduleBuilder.setUseSharedServer(true)
        moduleBuilder.setConfiguration(configuration)
        moduleBuilder.setModuleClass(IngestorServerModule::class.java)
        moduleBuilder.setSingleVmServerConfiguration(configuration)
        moduleBuilder.setSingleVmServerModuleClass(IngestorServerModule::class.java)
        val clientInjector : Injector = moduleBuilder.buildInjector()
        clientInjector.getInstance(Store::class.java)

    }

    @Test
    fun `Test get med Skif`() {

    }
}