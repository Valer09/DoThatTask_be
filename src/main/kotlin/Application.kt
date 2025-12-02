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
import io.ktor.server.plugins.cors.maxAgeDuration
import io.ktor.server.plugins.cors.routing.CORS
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.koin.mp.KoinPlatform.getKoin


//https://raspi.tail0458e4.ts.net


fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

    fun Application.module() {
        install(ContentNegotiation) {
            json()
        }
        install(CORS) {



            val isDev = this@module.environment.config.property("ktor.deployment.environment").getString() == "dev"

            if(isDev)
            {

                allowNonSimpleContentTypes = true
                allowCredentials = true
                allowSameOrigin = true

                allowMethod(HttpMethod.Options)
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Post)
                allowMethod(HttpMethod.Put)
                allowMethod(HttpMethod.Delete)

                allowHeader(HttpHeaders.ContentType)
                allowHeader(HttpHeaders.Authorization)
                allowCredentials = true
                anyHost()
            }
            else
            {
                allowHost("raspi.tail0458e4.ts.net", schemes = listOf("https"))
                allowCredentials = true
                allowHeader(HttpHeaders.ContentType)
                allowHeader(HttpHeaders.Authorization)
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Post)
                allowMethod(HttpMethod.Put)
                allowMethod(HttpMethod.Delete)
                allowSameOrigin = true
                anyHost()
            }


            maxAgeDuration = kotlin.time.Duration.parse("24h")
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
