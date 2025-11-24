package homeaq.dothattask

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*


//https://raspi.tail0458e4.ts.net


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

    fun Application.module() {
    configureSerialization()
    configureDatabases()
    configureRouting()

    install(ContentNegotiation)
    {
        json()
    }
}
