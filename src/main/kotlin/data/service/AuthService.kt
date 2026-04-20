package homeaq.dothattask.data.service

import homeaq.dothattask.Model.PasswordHash
import homeaq.dothattask.Model.auth.AuthTokens
import homeaq.dothattask.Model.auth.AuthenticatedUser
import homeaq.dothattask.Model.auth.JwtConfig
import homeaq.dothattask.data.repository.RefreshTokenRepository
import homeaq.dothattask.data.repository.UserGroupRepository
import homeaq.dothattask.data.repository.UserRepository
import java.security.MessageDigest
import java.time.Instant
import java.util.HexFormat

class AuthService(
    private val jwt: JwtConfig,
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val userGroups: UserGroupRepository,
) {
    suspend fun login(username: String, password: String): AuthTokens? {
        val user = users.userByUsername(username) ?: return null
        if (!PasswordHash.verifyPassword(password, user.password_hash)) return null
        val groupId = userGroups.groupIdOfUser(user.username)
        return issueTokens(user.username, user.name, groupId)
    }

    /** Returns null if the username is already taken. */
    suspend fun register(name: String, username: String, password: String): AuthTokens? {
        val created = users.create(name, username, PasswordHash.hashPassword(password))
        if (!created) return null
        return issueTokens(username.lowercase(), name, null)
    }

    /** Returns true on success; false if the old password does not verify. */
    suspend fun changePassword(username: String, oldPassword: String, newPassword: String): Boolean {
        val user = users.userByUsername(username) ?: return false
        if (!PasswordHash.verifyPassword(oldPassword, user.password_hash)) return false
        users.updatePasswordHash(user.username, PasswordHash.hashPassword(newPassword))
        // Sign out every existing session so a stolen credential cannot outlive the rotation.
        refreshTokens.revokeAllForUser(user.username)
        return true
    }

    suspend fun refresh(plainRefreshToken: String): AuthTokens? {
        val tokenHash = sha256(plainRefreshToken)
        val existing = refreshTokens.findByHash(tokenHash) ?: return null
        if (existing.revokedAt != null || existing.expiresAt.isBefore(Instant.now())) return null
        refreshTokens.revoke(tokenHash)
        val user = users.userByUsername(existing.userUsername) ?: return null
        val groupId = userGroups.groupIdOfUser(user.username)
        return issueTokens(user.username, user.name, groupId)
    }

    suspend fun logout(plainRefreshToken: String) {
        refreshTokens.revoke(sha256(plainRefreshToken))
    }

    private suspend fun issueTokens(username: String, name: String, groupId: Int?): AuthTokens {
        val access = jwt.generateAccessToken(username, groupId)
        val refresh = jwt.generateRefreshToken()
        refreshTokens.create(username, sha256(refresh), jwt.refreshExpiry())
        return AuthTokens(
            accessToken = access,
            refreshToken = refresh,
            expiresIn = jwt.accessTtlMinutes * 60,
            user = AuthenticatedUser(username, name, groupId),
        )
    }

    private fun sha256(s: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        return HexFormat.of().formatHex(digest)
    }
}
