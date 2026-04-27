package homeaq.dothattask.Controller

import homeaq.dothattask.Model.SendInviteRequest
import homeaq.dothattask.Model.UserPrincipal
import homeaq.dothattask.data.DataResult
import homeaq.dothattask.data.repository.UserGroupRepository
import homeaq.dothattask.data.service.InviteService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.inviteRoutes() {
    val inviteService by inject<InviteService>()
    val userGroups by inject<UserGroupRepository>()

    routing {
        authenticate("auth-jwt") {
            route("/api/invites") {

                post {
                    val principal = call.principal<UserPrincipal>()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val groupId = call.requireGroupId(userGroups) ?: return@post
                    try {
                        val body = call.receive<SendInviteRequest>()
                        val response = inviteService.send(principal.getUserName(), groupId, body.inviteeUsername)
                        when (response.result) {
                            DataResult.SUCCESS -> call.respond(HttpStatusCode.Created, response.data!!)
                            DataResult.NOT_FOUND -> call.respond(HttpStatusCode.NotFound, response.message)
                            DataResult.FORBIDDEN -> call.respond(HttpStatusCode.Forbidden, response.message)
                            DataResult.VALIDATION_ERROR -> call.respond(HttpStatusCode.Conflict, response.message)
                            else -> call.respond(HttpStatusCode.InternalServerError, response.message)
                        }
                    } catch (_: JsonConvertException) {
                        call.respond(HttpStatusCode.BadRequest)
                    } catch (_: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }

                get("/incoming") {
                    val principal = call.principal<UserPrincipal>()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val response = inviteService.incoming(principal.getUserName())
                    call.respond(HttpStatusCode.OK, response.data!!)
                }

                post("/{id}/accept") {
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val principal = call.principal<UserPrincipal>()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val response = inviteService.accept(id, principal.getUserName())
                    when (response.result) {
                        DataResult.SUCCESS -> call.respond(HttpStatusCode.OK, response.data!!)
                        DataResult.NOT_FOUND -> call.respond(HttpStatusCode.NotFound, response.message)
                        DataResult.FORBIDDEN -> call.respond(HttpStatusCode.Forbidden, response.message)
                        DataResult.VALIDATION_ERROR -> call.respond(HttpStatusCode.Conflict, response.message)
                        else -> call.respond(HttpStatusCode.InternalServerError, response.message)
                    }
                }

                post("/{id}/reject") {
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@post call.respond(HttpStatusCode.BadRequest)
                    val principal = call.principal<UserPrincipal>()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val response = inviteService.reject(id, principal.getUserName())
                    when (response.result) {
                        DataResult.SUCCESS -> call.respond(HttpStatusCode.OK, response.data!!)
                        DataResult.NOT_FOUND -> call.respond(HttpStatusCode.NotFound, response.message)
                        DataResult.FORBIDDEN -> call.respond(HttpStatusCode.Forbidden, response.message)
                        DataResult.VALIDATION_ERROR -> call.respond(HttpStatusCode.Conflict, response.message)
                        else -> call.respond(HttpStatusCode.InternalServerError, response.message)
                    }
                }

                delete("/{id}") {
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest)
                    val principal = call.principal<UserPrincipal>()
                        ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                    val response = inviteService.revoke(id, principal.getUserName())
                    when (response.result) {
                        DataResult.SUCCESS -> call.respond(HttpStatusCode.OK, response.data!!)
                        DataResult.NOT_FOUND -> call.respond(HttpStatusCode.NotFound, response.message)
                        DataResult.FORBIDDEN -> call.respond(HttpStatusCode.Forbidden, response.message)
                        DataResult.VALIDATION_ERROR -> call.respond(HttpStatusCode.Conflict, response.message)
                        else -> call.respond(HttpStatusCode.InternalServerError, response.message)
                    }
                }
            }
        }
    }
}
