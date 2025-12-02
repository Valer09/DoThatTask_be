package homeaq.dothattask

import homeaq.dothattask.data.DBSchema.TaskTableFactoryH2
import homeaq.dothattask.data.DBSchema.TaskTableFactoryPostgres
import homeaq.dothattask.data.repository.TaskRepository
import homeaq.dothattask.data.service.TaskService
import homeaq.dothattask.data.DBSchema.TaskTableSeedH2
import homeaq.dothattask.data.DBSchema.TaskTableSeedPostgres
import homeaq.dothattask.data.DBSchema.UserTableFactoryH2
import homeaq.dothattask.data.DBSchema.UserTableFactoryPostgres
import homeaq.dothattask.data.repository.UserRepository
import homeaq.dothattask.data.DBSchema.UserTableSeedH2
import homeaq.dothattask.data.DBSchema.UserTableSeedPostgres
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import io.ktor.server.application.Application
import io.ktor.server.application.log
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.sql.Connection
import java.sql.DriverManager

val appModule = module {


    single<Application> {
        getKoin().getProperty("application") ?: throw IllegalStateException("Application property is missing")
    }

    single<Boolean>(qualifier = named("embedded")) {
        get<Application>().environment.config
            .propertyOrNull("ktor.database.embedded")
            ?.getString()
            ?.toBoolean()
            ?: false
    }

    //Bean
    single<Connection>(qualifier = named("connection")) {
        val app = get<Application>()
        val useEmbedded = get<Boolean>( named("embedded"))

        if (useEmbedded) {
            app.log.info("Using embedded H2 database")
            DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "root", "")

        }
        else {
            val url = app.environment.config.property("ktor.postgres.url").getString()
            val user = app.environment.config.property("ktor.postgres.user").getString()
            val password = app.environment.config.property("ktor.postgres.password").getString()
            app.log.info("Connecting to postgres database at $url")
            DriverManager.getConnection(url, user, password)
        }
    }

    single<ITableFactory>(qualifier = named("task_table_factory")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) TaskTableFactoryH2() else TaskTableFactoryPostgres()
    }

    single<ITableFactory>(qualifier = named("user_table_factory")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) UserTableFactoryH2() else UserTableFactoryPostgres()
    }

    single<ITableSeed>(qualifier = named("task_table_seed")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) TaskTableSeedH2() else TaskTableSeedPostgres()
    }

    single<ITableSeed>(qualifier = named("user_table_seed")) {
        val useEmbedded = get<Boolean>(named("embedded"))
        if (useEmbedded) UserTableSeedH2() else UserTableSeedPostgres()
    }

    single<TaskRepository> { TaskRepository(
            get(named("connection")),
        get(named("task_table_factory")),
        get(named("task_table_seed"))) }

    single<UserRepository> { UserRepository(
        get(named("connection")),
        get(named("user_table_factory")),
        get(named("user_table_seed"))) }

    single<TaskService> { TaskService(get()) }

}
