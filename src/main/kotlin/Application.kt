package homeaq.dothattask

import homeaq.dothattask.Model.PasswordHash
import homeaq.dothattask.Model.User
import homeaq.dothattask.Model.UserPrincipal
import homeaq.dothattask.data.repository.UserRepository
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.basic
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.CORS
import org.koin.ktor.ext.inject
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

        val userRepository: UserRepository by inject()

        install(Authentication) {
            basic("auth-basic") {
                realm = "Ktor Server"
                validate { credentials ->
                    val user = userRepository.userByUsername(credentials.name)
                    if (user != null && PasswordHash.verifyPassword(credentials.password, user.password_hash))
                        UserPrincipal(user.username, user.name) else null
                }
            }
        }


        configureSerialization()
    configureRouting()
}
