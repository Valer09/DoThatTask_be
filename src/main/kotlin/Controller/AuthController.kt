package homeaq.dothattask.Controller

import homeaq.dothattask.Model.UserPrincipal
import homeaq.dothattask.Model.auth.ChangePasswordRequest
import homeaq.dothattask.Model.auth.LegacyLoginRequest
import homeaq.dothattask.Model.auth.LoginRequest
import homeaq.dothattask.Model.auth.LogoutRequest
import homeaq.dothattask.Model.auth.RefreshRequest
import homeaq.dothattask.Model.auth.RegisterRequest
import homeaq.dothattask.data.repository.UserRepository
import homeaq.dothattask.data.service.AuthService
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.reflect.TypeInfo
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Application.authRoutes() {
    val authService by inject<AuthService>()
    val user by inject<UserRepository>()

    routing {
        route("/api/auth") {
            post("/login") {
                try {
                    // Accept both the new email-based shape and the legacy
                    // username-based shape so a freshly deployed backend never
                    // breaks an older client mid-rollout.
                    val rawBody = call.receiveText()
                    val identifier: String
                    val password: String
                    val parser = Json { ignoreUnknownKeys = true }
                    val newShape = runCatching { parser.decodeFromString(LoginRequest.serializer(), rawBody) }.getOrNull()
                    if (newShape != null)
                    {
                        identifier = newShape.email
                        password = newShape.password
                    }
                    else return@post call.respond(HttpStatusCode.BadRequest)

                    if (identifier.isBlank() || password.isBlank()) {
                        return@post call.respond(HttpStatusCode.BadRequest)
                    }
                        when(val loginResponse = authService.login(identifier, password))
                        {
                            /* is AuthService.LoginResult.EmailNotConfirmed
                                -> {call.respond(HttpStatusCode.OK, message = loginResponse.authTokens)}
                                Suppressed until full email service is available -> return@post call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Need email confirmation")
                             */

                            is AuthService.LoginResult.Unauthorized ->
                                {
                                    return@post call.respond(HttpStatusCode.Unauthorized)
                                }
                            is AuthService.LoginResult.Success ->
                                {
                                    runCatching { user.reactivateUserNotification(loginResponse.authTokens?.user?.username ?: return@post call.respond(HttpStatusCode.Unauthorized)) }
                                    call.respond(HttpStatusCode.OK, message = loginResponse.authTokens)
                                }
                            is AuthService.LoginResult.EmailNotConfirmed ->
                            {
                                runCatching { user.reactivateUserNotification(loginResponse.authTokens?.user?.username ?: return@post call.respond(HttpStatusCode.Unauthorized)) }
                                call.respond(HttpStatusCode.OK, message = loginResponse.authTokens)
                            }


                        }
                } catch (_: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (_: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            post("/register") {
                try {
                    val body = call.receive<RegisterRequest>()
                    if (body.name.isBlank() || body.password.isBlank() || body.email.isBlank()) {
                        return@post call.respond(HttpStatusCode.BadRequest)
                    }
                    when (val result = authService.register(body.name, body.email, body.password, body.username)) {
                        is AuthService.RegisterResult.Success ->
                            call.respond(HttpStatusCode.Created, result.tokens)
                        AuthService.RegisterResult.EmailTaken ->
                            call.respond(HttpStatusCode.Conflict, mapOf("error" to "email_taken"))
                        AuthService.RegisterResult.UsernameTaken ->
                            call.respond(HttpStatusCode.Conflict, mapOf("error" to "username_taken"))
                        AuthService.RegisterResult.InvalidEmail ->
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "invalid_email"))
                    }
                } catch (_: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (_: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }

            // GET endpoint to make verification links clickable straight from
            // the email body. Returns a small HTML page in both branches —
            // the user is unlikely to want a JSON response in their browser.
            get("/verify-email") {
                val token = call.request.queryParameters["token"].orEmpty()
                if (token.isBlank()) {
                    call.respondText(
                        verifyHtml("Missing verification token.", success = false),
                        ContentType.Text.Html,
                        HttpStatusCode.BadRequest,
                    )
                    return@get
                }
                val ok = authService.verifyEmail(token)
                call.respondText(
                    if (ok) verifyHtml("Email confirmed — you can return to the app and log in.", success = true)
                    else verifyHtml("This link is invalid or expired.", success = false),
                    ContentType.Text.Html,
                    if (ok) HttpStatusCode.OK else HttpStatusCode.BadRequest,
                )
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

                post("/resend-verification") {
                    val principal = call.principal<UserPrincipal>()
                        ?: return@post call.respond(HttpStatusCode.Unauthorized)
                    val ok = authService.resendVerificationEmail(principal.getUserName())
                    if (ok) call.respond(HttpStatusCode.NoContent)
                    else call.respond(HttpStatusCode.BadRequest)
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

private fun verifyHtml(message: String, success: Boolean): String {
    val color = if (success) "#16a34a" else "#dc2626"
    val title = if (success) "Email confirmed" else "Verification failed"
    return """
        <!doctype html>
        <html lang="en">
          <head>
            <meta charset="utf-8">
            <title>$title</title>
            <style>
              body{font-family:Arial,Helvetica,sans-serif;background:#1F0A35;color:#F0E6FF;
                   display:flex;align-items:center;justify-content:center;height:100vh;margin:0}
              .card{background:#2B2140;padding:32px 40px;border-radius:16px;max-width:420px;text-align:center}
              h1{color:$color;margin:0 0 16px}
              p{margin:0;line-height:1.5}
            </style>
          </head>
          <body>
            <div class="card">
              <h1>$title</h1>
              <p>$message</p>
            </div>
          </body>
        </html>
    """.trimIndent()
}
