package homeaq.dothattask.Controller

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskUpdate
import homeaq.dothattask.Model.UserPrincipal
import homeaq.dothattask.data.DataResponse
import homeaq.dothattask.data.DataResult
import homeaq.dothattask.data.service.TaskService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.taskRoutes()
{
    val taskService by inject<TaskService>()

    routing {
        authenticate("auth-jwt") {
            route("/api/tasks") {
                get {
                    val groupId = call.requireGroupId() ?: return@get
                    call.respond(HttpStatusCode.OK, taskService.all(groupId).data!!)
                }

                get("/tasksByUser/{username}") {
                    val username = call.parameters["username"]
                    if (username.isNullOrEmpty()) return@get call.respond(HttpStatusCode.BadRequest)
                    val groupId = call.requireGroupId() ?: return@get
                    call.respond(HttpStatusCode.OK, taskService.tasksByUser(username, groupId).data!!)
                }

                get("/completed") {
                    val principal = call.principal<UserPrincipal>()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val groupId = call.requireGroupId() ?: return@get

                    val response = taskService.getCompletedTasks(principal.getUserName(), groupId)
                    if (response.result == DataResult.NOT_FOUND) return@get call.respond(HttpStatusCode.InternalServerError)

                    call.respond(HttpStatusCode.OK, response.data!!)
                }

                get("/assignedTask") {
                    val principal = call.principal<UserPrincipal>()
                        ?: return@get call.respond(HttpStatusCode.Unauthorized)
                    val groupId = call.requireGroupId() ?: return@get

                    val task = taskService.assignedTask(principal.getUserName(), groupId)
                    if (task.result == DataResult.NOT_FOUND) return@get call.respond(HttpStatusCode.NotFound)
                    call.respond(HttpStatusCode.OK, task.data!!)
                }

                get("/byName/{name}") {
                    val name = call.parameters["name"]
                    if (name.isNullOrEmpty()) return@get call.respond(HttpStatusCode.BadRequest)
                    val groupId = call.requireGroupId() ?: return@get

                    val task = taskService.read(name, groupId)
                    if (task.result == DataResult.NOT_FOUND) return@get call.respond(HttpStatusCode.NotFound)
                    call.respond(HttpStatusCode.OK, task.data!!)
                }

                post {
                    try {
                        val principal = call.principal<UserPrincipal>()
                            ?: return@post call.respond(HttpStatusCode.Unauthorized)
                        val groupId = call.requireGroupId() ?: return@post

                        val task = call.receive<TaskUpdate>()
                        val response = taskService.addOrUpdate(task, principal.getUserName(), groupId)

                        if (response.isSuccessful()) call.respond(HttpStatusCode.OK, response.data!!)
                        else handleErrorResponse(response)
                    } catch (_: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest)
                    } catch (_: JsonConvertException) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }

                post("/pickTask") {
                    try {
                        val category = call.request.queryParameters["category"]
                        if (category.isNullOrEmpty()) return@post call.respond(HttpStatusCode.BadRequest)

                        val principal = call.principal<UserPrincipal>()
                            ?: return@post call.respond(HttpStatusCode.Unauthorized)
                        val groupId = call.requireGroupId() ?: return@post

                        val response = taskService.pickTask(principal.getUserName(), category, groupId)
                        if (response.result == DataResult.NOT_FOUND) return@post call.respond(
                            HttpStatusCode.NotFound,
                            message = "No tasks assigned to this user",
                        )

                        val assigned = taskService.assignedTask(principal.getUserName(), groupId)
                        if (assigned.result == DataResult.NOT_FOUND) return@post call.respond(HttpStatusCode.InternalServerError)
                        call.respond(HttpStatusCode.OK, assigned.data!!)
                    } catch (_: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest)
                    } catch (_: JsonConvertException) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }

                post("/unassign") {
                    try {
                        val taskName = call.request.queryParameters["task_name"]
                        if (taskName.isNullOrEmpty()) return@post call.respond(HttpStatusCode.BadRequest)
                        val groupId = call.requireGroupId() ?: return@post

                        val response = taskService.unassign(taskName, groupId)
                        if (response.result == DataResult.NOT_FOUND) return@post call.respond(
                            HttpStatusCode.NotFound,
                            message = "No tasks found with this name",
                        )
                        call.respond(HttpStatusCode.OK, response.data!!)
                    } catch (_: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest)
                    } catch (_: JsonConvertException) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }

                post("/completeTask") {
                    try {
                        val taskName = call.request.queryParameters["task_name"]
                        if (taskName.isNullOrEmpty()) return@post call.respond(HttpStatusCode.BadRequest)
                        val groupId = call.requireGroupId() ?: return@post

                        val response = taskService.complete(taskName, groupId)
                        if (response.isSuccessful()) call.respond(HttpStatusCode.OK, response.data!!)
                        else handleErrorResponse(response)
                    } catch (_: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest)
                    } catch (_: JsonConvertException) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }

                post("/completeActiveTask") {
                    try {
                        val principal = call.principal<UserPrincipal>()
                            ?: return@post call.respond(HttpStatusCode.Unauthorized)
                        val groupId = call.requireGroupId() ?: return@post

                        val response = taskService.completeActiveTask(principal.getUserName(), groupId)
                        if (response.isSuccessful()) call.respond(HttpStatusCode.OK, response.data!!)
                        else handleErrorResponse(response)
                    } catch (_: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest)
                    } catch (_: JsonConvertException) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }

                delete("/{name}") {
                    val name = call.parameters["name"]
                    if (name.isNullOrEmpty()) return@delete call.respond(HttpStatusCode.BadRequest)
                    val groupId = call.requireGroupId() ?: return@delete

                    try {
                        val response = taskService.delete(name, groupId)
                        if (response.isSuccessful()) call.respond(HttpStatusCode.OK)
                        else call.respond(HttpStatusCode.NotFound, response.message)
                    } catch (_: Exception) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }
}

private suspend fun ApplicationCall.requireGroupId(): Int? {
    val groupId = principal<UserPrincipal>()?.groupId
    if (groupId == null) {
        respond(HttpStatusCode.Forbidden, "User is not a member of any group")
        return null
    }
    return groupId
}

private suspend fun RoutingContext.handleErrorResponse(response: DataResponse<Task>)
{
    when (response.result) {
        DataResult.NOT_FOUND -> call.respond(HttpStatusCode.NotFound, response.message)
        DataResult.VALIDATION_ERROR -> call.respond(HttpStatusCode.BadRequest, response.message)
        DataResult.FORBIDDEN -> call.respond(HttpStatusCode.Forbidden, response.message)
        else -> call.respond(HttpStatusCode.InternalServerError, response.message)
    }
}
