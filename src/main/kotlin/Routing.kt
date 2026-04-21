package homeaq.dothattask

import homeaq.dothattask.Controller.authRoutes
import homeaq.dothattask.Controller.groupRoutes
import homeaq.dothattask.Controller.inviteRoutes
import homeaq.dothattask.Controller.taskRoutes
import homeaq.dothattask.Controller.userRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureRouting() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respondText(text = "Internal server error", status = HttpStatusCode.InternalServerError)
        }
    }
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        // Static plugin. Try to access `/static/index.html`
        staticResources("/static", "static")

        taskRoutes()
        userRoutes()
        authRoutes()
        groupRoutes()
        inviteRoutes()
    }
}