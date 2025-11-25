package homeaq.dothattask

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.CORS
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger


//https://raspi.tail0458e4.ts.net


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

    fun Application.module() {
        install(ContentNegotiation) {
            json()
        }
        install(CORS) {
            allowHeader(HttpHeaders.ContentType)
            allowMethod(HttpMethod.Delete)
            // For ease of demonstration we allow any connections.
            // Don't do this in production.
            anyHost()
        }
        install(Koin)
        {
            slf4jLogger()
            modules(appModule)
            properties(mapOf("application" to this@module))
        }

    configureSerialization()
    configureRouting()
}
