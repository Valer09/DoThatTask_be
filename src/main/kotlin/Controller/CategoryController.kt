package homeaq.dothattask.Controller

import homeaq.dothattask.data.DataResult
import homeaq.dothattask.data.repository.UserGroupRepository
import homeaq.dothattask.data.service.CategoryService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class CreateCategoryRequest(val name: String, val color: String? = null)

fun Application.categoryRoutes() {
    val categoryService by inject<CategoryService>()
    val userGroups by inject<UserGroupRepository>()

    routing {
        authenticate("auth-jwt") {
            route("/api/categories") {

                /** List categories visible to the active group. */
                get {
                    val groupId = call.requireGroupId(userGroups) ?: return@get
                    call.respond(HttpStatusCode.OK, categoryService.listForGroup(groupId).data!!)
                }

                /**
                 * Create-or-link. Body: { name, color? }. If a category with
                 * the same case-insensitive name exists, only the group link
                 * is added. Otherwise a new global category is created.
                 */
                post {
                    val groupId = call.requireGroupId(userGroups) ?: return@post
                    try {
                        val body = call.receive<CreateCategoryRequest>()
                        val response = categoryService.addToGroup(groupId, body.name, body.color)
                        when (response.result) {
                            DataResult.SUCCESS -> call.respond(HttpStatusCode.Created, response.data!!)
                            DataResult.VALIDATION_ERROR -> call.respond(HttpStatusCode.BadRequest, response.message)
                            else -> call.respond(HttpStatusCode.InternalServerError, response.message)
                        }
                    } catch (_: JsonConvertException) {
                        call.respond(HttpStatusCode.BadRequest)
                    } catch (_: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }

                /** Unlink — only removes the group↔category row, not the category itself. */
                delete("/{id}") {
                    val groupId = call.requireGroupId(userGroups) ?: return@delete
                    val id = call.parameters["id"]?.toIntOrNull()
                        ?: return@delete call.respond(HttpStatusCode.BadRequest, "Invalid category id")
                    val response = categoryService.removeFromGroup(groupId, id)
                    when (response.result) {
                        DataResult.SUCCESS -> call.respond(HttpStatusCode.NoContent)
                        DataResult.NOT_FOUND -> call.respond(HttpStatusCode.NotFound, response.message)
                        DataResult.FORBIDDEN -> call.respond(HttpStatusCode.Conflict, response.message)
                        else -> call.respond(HttpStatusCode.InternalServerError, response.message)
                    }
                }
            }
        }
    }
}
