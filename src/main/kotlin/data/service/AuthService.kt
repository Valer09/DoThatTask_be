package homeaq.dothattask.data.service

import homeaq.dothattask.Model.GroupSummary
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
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userGroupRepository: UserGroupRepository,
) {
    suspend fun login(username: String, password: String): AuthTokens? {
        val user = userRepository.userByUsername(username) ?: return null
        if (!PasswordHash.verifyPassword(password, user.password_hash)) return null
        return issueTokens(user.username, user.name)
    }

    /** Returns null if the username is already taken. */
    suspend fun register(name: String, username: String, password: String): AuthTokens? {
        val created = userRepository.create(name, username, PasswordHash.hashPassword(password))
        if (!created) return null
        return issueTokens(username.lowercase(), name)
    }

    /** Returns true on success; false if the old password does not verify. */
    suspend fun changePassword(username: String, oldPassword: String, newPassword: String): Boolean {
        val user = userRepository.userByUsername(username) ?: return false
        if (!PasswordHash.verifyPassword(oldPassword, user.password_hash)) return false
        userRepository.updatePasswordHash(user.username, PasswordHash.hashPassword(newPassword))
        // Sign out every existing session so a stolen credential cannot outlive the rotation.
        refreshTokenRepository.revokeAllForUser(user.username)
        return true
    }

    suspend fun refresh(plainRefreshToken: String): AuthTokens? {
        val tokenHash = sha256(plainRefreshToken)
        val existing = refreshTokenRepository.findByHash(tokenHash) ?: return null
        if (existing.revokedAt != null || existing.expiresAt.isBefore(Instant.now())) return null
        refreshTokenRepository.revoke(tokenHash)
        val user = userRepository.userByUsername(existing.userUsername) ?: return null
        return issueTokens(user.username, user.name)
    }

    suspend fun logout(plainRefreshToken: String) {
        refreshTokenRepository.revoke(sha256(plainRefreshToken))
    }

    private suspend fun issueTokens(username: String, name: String): AuthTokens {
        val access = jwt.generateAccessToken(username)
        val refresh = jwt.generateRefreshToken()
        refreshTokenRepository.create(username, sha256(refresh), jwt.refreshExpiry())
        val groups = userGroupRepository.groupsOfUser(username).map {
            GroupSummary(id = it.id, name = it.name, color = it.color)
        }
        return AuthTokens(
            accessToken = access,
            refreshToken = refresh,
            expiresIn = jwt.accessTtlMinutes * 60,
            user = AuthenticatedUser(username, name, groups),
        )
    }

    private fun sha256(s: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        return HexFormat.of().formatHex(digest)
    }
}
