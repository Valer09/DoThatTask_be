package homeaq.dothattask

import homeaq.dothattask.Model.UserPrincipal
import homeaq.dothattask.Model.auth.JwtConfig
import homeaq.dothattask.data.repository.GroupRepository
import homeaq.dothattask.data.repository.InviteRepository
import homeaq.dothattask.data.repository.RefreshTokenRepository
import homeaq.dothattask.data.repository.TaskRepository
import homeaq.dothattask.data.repository.UserGroupRepository
import homeaq.dothattask.data.repository.UserRepository
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.maxAgeDuration
import io.ktor.server.plugins.cors.routing.CORS
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

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
                allowCredentials = true
                allowSameOrigin = true

                allowHeader(HttpHeaders.ContentType)
                allowHeader(HttpHeaders.Authorization)
                allowMethod(HttpMethod.Options)
                allowMethod(HttpMethod.Get)
                allowMethod(HttpMethod.Post)
                allowMethod(HttpMethod.Put)
                allowMethod(HttpMethod.Delete)
                allowHost("serverless.synology.me:8443", schemes = listOf("https"))
                //allowHost("raspi.tail0458e4.ts.net", schemes = listOf("https"))
            }


            maxAgeDuration = kotlin.time.Duration.parse("24h")
        }
        install(Koin)
        {
            slf4jLogger()
            modules(appModule)
            properties(mapOf("application" to this@module))
        }

        // Force FK-safe table creation order via eager injection.
        val userRepository: UserRepository by inject()
        val groupRepository: GroupRepository by inject()
        val userGroupRepository: UserGroupRepository by inject()
        val taskRepository: TaskRepository by inject()
        val inviteRepository: InviteRepository by inject()
        val refreshTokenRepository: RefreshTokenRepository by inject()
        userRepository.hashCode()
        groupRepository.hashCode()
        userGroupRepository.hashCode()
        taskRepository.hashCode()
        inviteRepository.hashCode()
        refreshTokenRepository.hashCode()

        val jwtConfig: JwtConfig by inject()

        install(Authentication) {
            jwt("auth-jwt") {
                realm = jwtConfig.realm
                verifier(jwtConfig.verifier())
                validate { credential ->
                    val username = credential.payload.subject ?: return@validate null
                    val user = userRepository.userByUsername(username) ?: return@validate null
                    val gidClaim = credential.payload.getClaim("gid")
                    val groupId = if (gidClaim.isNull || gidClaim.isMissing) null else gidClaim.asInt()
                    UserPrincipal(user.username, user.name, groupId)
                }
            }
        }


        configureSerialization()
    configureRouting()
}
