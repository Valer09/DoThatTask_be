package homeaq.dothattask.Controller

import homeaq.dothattask.Model.Task
import homeaq.dothattask.data.TaskService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.request.receive
import io.ktor.server.response.respond
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
        route("/tasks") {
            get {
                val tasks = taskService.all()
                call.respond(tasks)
            }
            get("/byId/{id}") {
                val id = call.parameters["id"]
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }
                val task = taskService.read(Integer.parseInt(id))
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respond(task)
            }

            post {
                try {
                    val task = call.receive<Task>()
                    taskService.addOrUpdate(task)
                    call.respond(HttpStatusCode.NoContent)
                } catch (ex: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
            delete("/{id}") {
                val id = call.parameters["id"]
                if (id == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@delete
                }
                try
                {
                    taskService.delete(Integer.parseInt(id))
                    call.respond(HttpStatusCode.OK)
                }
                catch (ex: Exception)
                {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
        }
    }
}