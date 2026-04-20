package homeaq.dothattask.Model.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String)

@Serializable
data class RegisterRequest(val name: String, val username: String, val password: String)

@Serializable
data class ChangePasswordRequest(val oldPassword: String, val newPassword: String)

@Serializable
data class AuthenticatedUser(val username: String, val name: String, val groupId: Int?)

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: AuthenticatedUser,
)
