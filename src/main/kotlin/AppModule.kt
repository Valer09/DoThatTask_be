package homeaq.dothattask

import homeaq.dothattask.data.TaskRepository
import homeaq.dothattask.data.TaskService
import homeaq.dothattask.data.TaskTableSeedH2
import homeaq.dothattask.data.TaskTableSeedPostgres
import io.ktor.server.application.Application
import io.ktor.server.application.log
import org.koin.dsl.module
import java.sql.Connection
import java.sql.DriverManager

val appModule = module {


    single<Application> {
        getKoin().getProperty<Application>("application") ?: throw IllegalStateException("Application property is missing")
    }

    single<Boolean> {
        get<Application>().environment.config
            .propertyOrNull("ktor.database.embedded")
            ?.getString()
            ?.toBoolean()
            ?: false
    }

    //Bean
    single<Connection> {
        val app = get<Application>()
        val useEmbedded = get<Boolean>()

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

    single<TaskRepository> { TaskRepository(get(), if (get<Boolean>()) TaskTableSeedH2(get()) else TaskTableSeedPostgres(get())) }
    single<TaskService> { TaskService(get()) }
}