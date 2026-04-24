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

fun Application.userRoutes()
{
    val userRepository by inject<UserRepository>()

    routing {
        route("/api/user") {

            authenticate("auth-jwt") {
                get("/me") {
                    val principal = call.principal<UserPrincipal>()
                    call.respond(
                        HttpStatusCode.OK,
                        mapOf(
                            "username" to principal?.getUserName(),
                            "name" to principal?.getName(),
                            "groupId" to principal?.groupId?.toString(),
                        ),
                    )
                }

                get("/usersLessMe") {
                    val principal = call.principal<UserPrincipal>()
                    val groupId = principal?.groupId
                    if (groupId == null) {
                        call.respond(HttpStatusCode.OK, emptyList<Any>())
                        return@get
                    }
                    call.respond(
                        HttpStatusCode.OK,
                        userRepository.allInGroup(groupId).filterNot { it.username == principal.getUserName() },
                    )
                }
            }
        }
    }
}
