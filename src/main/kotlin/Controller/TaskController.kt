package homeaq.dothattask.Controller

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskUpdate
import homeaq.dothattask.data.DataResponse
import homeaq.dothattask.data.DataResult
import homeaq.dothattask.data.service.TaskService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.get
import org.koin.ktor.ext.inject

fun Application.taskRoutes()
{
    val taskService by inject<TaskService>()

    routing {
        route("/tasks") {
            get {
                call.respond(HttpStatusCode.OK, taskService.all().data!!)
                return@get
                }

            get("/tasksByUser/{username}") {
                val username = call.parameters["username"]
                if(username.isNullOrEmpty()) return@get call.respond(HttpStatusCode.BadRequest)

                call.respond(HttpStatusCode.OK, taskService.tasksByUser(username).data!!)
                return@get
            }

            get("/assignedTask") {
                val task = taskService.assignedTask()

                if (task.result == DataResult.NOT_FOUND) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }

                call.respond(HttpStatusCode.OK, task.data!!)
                return@get
            }

            get("/byName/{name}") {
                val name = call.parameters["name"]
                if (name.isNullOrEmpty()) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                val task = taskService.read(name)
                if (task.result == DataResult.NOT_FOUND) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respond(HttpStatusCode.OK, task.data!!)
            }

            post {
                try {
                    val task = call.receive<TaskUpdate>()
                    val response = taskService.addOrUpdate(task)

                    if(response.isSuccessful()) call.respond(HttpStatusCode.OK, response.data!!)
                    else handleErrorResponse(response)
                }
                catch (ex: IllegalStateException)
                {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException)
                {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post("/pickTask"){
                try {

                    val response = taskService.pickTask()
                    if(response.result == DataResult.NOT_FOUND)return@post call.respond(HttpStatusCode.NotFound, message = "No tasks assigned to this user")

                    val assTask = taskService.assignedTask()

                    if (assTask.result == DataResult.NOT_FOUND) return@post call.respond(HttpStatusCode.InternalServerError)
                    return@post call.respond(HttpStatusCode.OK, assTask.data!!)
                }
                catch (ex: IllegalStateException)
                {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException)
                {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post("/completeTask"){
                try {
                    val taskName = call.request.queryParameters["task_name"]

                    if(taskName.isNullOrEmpty()) return@post call.respond(HttpStatusCode.BadRequest)

                    val response = taskService.complete(taskName)
                    if(response.isSuccessful()) return@post call.respond(HttpStatusCode.OK, response.data!!)

                    else handleErrorResponse(response)
                }
                catch (ex: IllegalStateException)
                {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException)
                {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post("/completeActiveTask"){
                try {

                    val response = taskService.completeActiveTask()
                    if(response.isSuccessful()) return@post call.respond(HttpStatusCode.OK, response.data!!)

                    else handleErrorResponse(response)
                }
                catch (ex: IllegalStateException)
                {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException)
                {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            delete("/{name}") {
                val name = call.parameters["name"]
                if (name.isNullOrEmpty()) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@delete
                }
                try
                {
                    val response = taskService.delete(name)
                    if(response.isSuccessful()) call.respond(HttpStatusCode.OK)
                    else call.respond(HttpStatusCode.NotFound, response.message)
                }
                catch (ex: Exception)
                {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }
}

private suspend fun RoutingContext.handleErrorResponse(response: DataResponse<Task>)
{
    when(response.result)
    {
        DataResult.NOT_FOUND -> call.respond(HttpStatusCode.NotFound, response.message)
        DataResult.VALIDATION_ERROR -> call.respond(HttpStatusCode.BadRequest, response.message)
        else -> call.respond(HttpStatusCode.InternalServerError, response.message)
    }
}