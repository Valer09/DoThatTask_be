package homeaq.dothattask.Controller

import homeaq.dothattask.Model.UserPrincipal
import homeaq.dothattask.data.repository.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Application.userRoutes()
{
    val userRepository by inject<UserRepository>()

    routing {
        route("/api/user") {

            authenticate("auth-basic") {
                // Login check: the client calls this endpoint with Basic Auth credentials.
                // A 200 response confirms the credentials are valid; 401 means invalid.
                get("/me") {
                    val principal = call.principal<UserPrincipal>()
                    call.respond(HttpStatusCode.OK, mapOf("username" to principal?.getUserName(), "name" to principal?.getName()))
                    return@get
                }

                get("/usersLessMe") {
                    val principal = call.principal<UserPrincipal>()
                    call.respond(HttpStatusCode.OK, userRepository.all().filterNot { it.username == principal?.getUserName() })
                    return@get
                }
            }
        }
    }
}