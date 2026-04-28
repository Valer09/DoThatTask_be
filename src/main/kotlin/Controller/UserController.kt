package homeaq.dothattask.Controller

import homeaq.dothattask.Model.GroupSummary
import homeaq.dothattask.Model.UserPrincipal
import homeaq.dothattask.Model.auth.AuthenticatedUser
import homeaq.dothattask.data.repository.UserGroupRepository
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
    val userGroups by inject<UserGroupRepository>()

    routing {
        route("/api/user") {

            authenticate("auth-jwt") {
                get("/me") {
                    val principal = call.principal<UserPrincipal>()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val groups = userGroups.groupsOfUser(principal.getUserName())
                        .map { GroupSummary(id = it.id, name = it.name, color = it.color) }
                    call.respond(
                        HttpStatusCode.OK,
                        AuthenticatedUser(
                            username = principal.getUserName(),
                            name = principal.getName(),
                            groups = groups,
                        )
                    )
                }

                get("/usersLessMe") {
                    val principal = call.principal<UserPrincipal>()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val groupId = call.requireGroupId(userGroups) ?: return@get
                    call.respond(
                        HttpStatusCode.OK,
                        userRepository.allInGroup(groupId).filterNot { it.username == principal.getUserName() },
                    )
                }

                get("/groupMembers/{groupId}") {
                    val principal = call.principal<UserPrincipal>()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val gid = call.parameters["groupId"]?.toIntOrNull()
                        ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid groupId")
                    if (!userGroups.isMember(principal.getUserName(), gid)) {
                        return@get call.respond(HttpStatusCode.Forbidden, "You are not a member of this group")
                    }
                    call.respond(
                        HttpStatusCode.OK,
                        userRepository.allInGroup(gid).filterNot { it.username == principal.getUserName() },
                    )
                }
            }
        }
    }
}
