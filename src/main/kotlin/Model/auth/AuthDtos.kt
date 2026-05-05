package homeaq.dothattask.Model.auth

import homeaq.dothattask.Model.GroupSummary
import kotlinx.serialization.Serializable

/**
 * Login by email — preferred. Older clients that still send a username
 * are accepted via the legacy [LegacyLoginRequest] shape, see [AuthController].
 */
@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class LegacyLoginRequest(val username: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String)

/**
 * Registration shape:
 *  - [email] is required and unique.
 *  - [username] is optional; if blank the server derives it from the part
 *    of the email before `@`, with a numeric suffix on collision. Old
 *    clients that always send `username` continue to work — that string
 *    becomes the user's username verbatim.
 */
@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val username: String? = null,
)

@Serializable
data class ChangePasswordRequest(val oldPassword: String, val newPassword: String)

@Serializable
data class AuthenticatedUser(
    val username: String,
    val name: String,
    val email: String? = null,
    val emailVerified: Boolean = false,
    val groups: List<GroupSummary> = emptyList(),
)

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: AuthenticatedUser,
)
