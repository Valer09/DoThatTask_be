package homeaq.dothattask.Controller

import homeaq.dothattask.Model.CreateGroupRequest
import homeaq.dothattask.Model.UserPrincipal
import homeaq.dothattask.data.DataResult
import homeaq.dothattask.data.service.GroupService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.groupRoutes() {
    val groupService by inject<GroupService>()

    routing {
        authenticate("auth-jwt") {
            route("/api/groups") {

                post {
                    val principal = call.principal<UserPrincipal>()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    try {
                        val body = call.receive<CreateGroupRequest>()
                        val response = groupService.create(body.name, principal.getUserName())
                        when (response.result) {
                            DataResult.SUCCESS -> call.respond(HttpStatusCode.Created, response.data!!)
                            DataResult.FORBIDDEN -> call.respond(HttpStatusCode.Conflict, response.message)
                            DataResult.VALIDATION_ERROR -> call.respond(HttpStatusCode.Conflict, response.message)
                            else -> call.respond(HttpStatusCode.InternalServerError, response.message)
                        }
                    } catch (_: JsonConvertException) {
                        call.respond(HttpStatusCode.BadRequest)
                    } catch (_: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }

                get("/me") {
                    val principal = call.principal<UserPrincipal>()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val response = groupService.myGroup(principal.getUserName())
                    when (response.result) {
                        DataResult.SUCCESS -> call.respond(HttpStatusCode.OK, response.data!!)
                        DataResult.NOT_FOUND -> call.respond(HttpStatusCode.NoContent)
                        else -> call.respond(HttpStatusCode.InternalServerError, response.message)
                    }
                }

                post("/leave") {
                    val principal = call.principal<UserPrincipal>()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val response = groupService.leave(principal.getUserName())
                    when (response.result) {
                        DataResult.SUCCESS -> call.respond(HttpStatusCode.OK, response.message)
                        DataResult.NOT_FOUND -> call.respond(HttpStatusCode.NotFound, response.message)
                        DataResult.FORBIDDEN -> call.respond(HttpStatusCode.Conflict, response.message)
                        else -> call.respond(HttpStatusCode.InternalServerError, response.message)
                    }
                }
            }
        }
    }
}
