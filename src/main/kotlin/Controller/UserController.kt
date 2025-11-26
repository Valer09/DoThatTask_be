package homeaq.dothattask.Controller

import homeaq.dothattask.Model.PasswordHash
import homeaq.dothattask.data.repository.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Application.userRoutes()
{
    val userRepository by inject<UserRepository>()

    routing {
        route("/user") {
            get("/byUsername/{username}") {
                val username = call.parameters["username"]
                call.respond(HttpStatusCode.OK, userRepository.userByUsername(username!!)!!)
                return@get
            }

           /* get("/passwordByUsername/{username}") {
                val username = call.parameters["username"]
                call.respond(HttpStatusCode.OK, userRepository.passwordHashByUsername(username!!))
                return@get
            }*/

            get("/login/") {

                val username = call.request.queryParameters["username"]
                val password = call.request.queryParameters["password"]

                if (username == null || password == null)
                {
                    call.respond(HttpStatusCode.BadRequest, "Missing username or password")
                    return@get
                }

                var storedHash = ""
                try
                {
                    storedHash = userRepository.passwordHashByUsername(username)
                }
                catch (ex: NotFoundException)
                {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid username or password")
                    return@get
                }
                catch (ex: Exception)
                {
                    call.respond(HttpStatusCode.InternalServerError, ex)
                    return@get
                }


                val success = PasswordHash.verifyPassword(password, storedHash)

                if (!success)
                {
                    call.respond(HttpStatusCode.Unauthorized, "Invalid username or password")
                    return@get
                }
                call.respond(HttpStatusCode.OK, "Login successful")
            }
        }
    }
}