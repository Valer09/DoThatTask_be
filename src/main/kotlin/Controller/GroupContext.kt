package homeaq.dothattask.Controller

import homeaq.dothattask.Model.UserPrincipal
import homeaq.dothattask.data.repository.UserGroupRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.principal
import io.ktor.server.response.respond

/**
 * Reads `X-Group-Id` from the request headers, parses it, and verifies the
 * authenticated user is a member of that group. On any failure responds with
 * the appropriate status and returns null (caller should `return@<route>`).
 */
suspend fun ApplicationCall.requireGroupId(userGroups: UserGroupRepository): Int? {
    val header = request.headers["X-Group-Id"]
    val groupId = header?.toIntOrNull() ?: run {
        respond(HttpStatusCode.BadRequest, "Missing or invalid X-Group-Id header")
        return null
    }
    val principal = principal<UserPrincipal>() ?: run {
        respond(HttpStatusCode.Unauthorized)
        return null
    }
    if (!userGroups.isMember(principal.getUserName(), groupId)) {
        respond(HttpStatusCode.Forbidden, "You are not a member of this group")
        return null
    }
    return groupId
}
