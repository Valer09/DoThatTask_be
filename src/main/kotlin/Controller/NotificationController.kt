package homeaq.dothattask.Controller

import homeaq.dothattask.Model.UserPrincipal
import homeaq.dothattask.data.repository.FcmTokenRepository
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

@Serializable
data class FcmTokenRequest(val token: String, val platform: String = "android")

fun Application.notificationRoutes() {
    val fcmTokens by inject<FcmTokenRepository>()

    routing {
        authenticate("auth-jwt") {
            route("/api/notifications") {

                post("/register") {
                    val principal = call.principal<UserPrincipal>()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    try {
                        val body = call.receive<FcmTokenRequest>()
                        if (body.token.isBlank()) {
                            return@post call.respond(HttpStatusCode.BadRequest, "Empty token")
                        }
                        fcmTokens.register(principal.getUserName(), body.token, body.platform)
                        call.respond(HttpStatusCode.NoContent)
                    } catch (_: JsonConvertException) {
                        call.respond(HttpStatusCode.BadRequest)
                    } catch (_: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }

                delete("/register") {
                    try {
                        val body = call.receive<FcmTokenRequest>()
                        if (body.token.isBlank()) {
                            return@delete call.respond(HttpStatusCode.BadRequest, "Empty token")
                        }
                        val removed = fcmTokens.unregister(body.token)
                        call.respond(if (removed) HttpStatusCode.NoContent else HttpStatusCode.NotFound)
                    } catch (_: JsonConvertException) {
                        call.respond(HttpStatusCode.BadRequest)
                    } catch (_: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }
            }
        }
    }
}
