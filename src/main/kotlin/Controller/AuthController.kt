package homeaq.dothattask.Controller

import homeaq.dothattask.Model.UserPrincipal
import homeaq.dothattask.Model.auth.ChangePasswordRequest
import homeaq.dothattask.Model.auth.LoginRequest
import homeaq.dothattask.Model.auth.LogoutRequest
import homeaq.dothattask.Model.auth.RefreshRequest
import homeaq.dothattask.Model.auth.RegisterRequest
import homeaq.dothattask.data.service.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject

fun Application.authRoutes() {
    val authService by inject<AuthService>()

    routing {
        route("/api/auth") {
            post("/login") {
                try {
                    val body = call.receive<LoginRequest>()
                    val tokens = authService.login(body.username, body.password)
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    call.respond(HttpStatusCode.OK, tokens)
                } catch (_: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (_: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post("/register") {
                try {
                    val body = call.receive<RegisterRequest>()
                    if (body.username.isBlank() || body.password.isBlank() || body.name.isBlank()) {
                        return@post call.respond(HttpStatusCode.BadRequest)
                    }
                    val tokens = authService.register(body.name, body.username, body.password)
                        ?: return@post call.respond(HttpStatusCode.Conflict)
                    call.respond(HttpStatusCode.Created, tokens)
                } catch (_: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (_: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post("/refresh") {
                try {
                    val body = call.receive<RefreshRequest>()
                    val tokens = authService.refresh(body.refreshToken)
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    call.respond(HttpStatusCode.OK, tokens)
                } catch (_: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (_: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            authenticate("auth-jwt") {
                post("/logout") {
                    try {
                        val body = call.receive<LogoutRequest>()
                        authService.logout(body.refreshToken)
                        call.respond(HttpStatusCode.NoContent)
                    } catch (_: JsonConvertException) {
                        call.respond(HttpStatusCode.BadRequest)
                    } catch (_: IllegalStateException) {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                }

                post("/change-password") {
                    val principal = call.principal<UserPrincipal>()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    try {
                        val body = call.receive<ChangePasswordRequest>()
                        if (body.newPassword.isBlank()) return@post call.respond(HttpStatusCode.BadRequest)
                        val ok = authService.changePassword(principal.getUserName(), body.oldPassword, body.newPassword)
                        if (ok) call.respond(HttpStatusCode.NoContent)
                        else call.respond(HttpStatusCode.Forbidden)
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
