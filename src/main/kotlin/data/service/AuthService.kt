package homeaq.dothattask.data.service

import homeaq.dothattask.Model.GroupSummary
import homeaq.dothattask.Model.PasswordHash
import homeaq.dothattask.Model.User
import homeaq.dothattask.Model.auth.AuthTokens
import homeaq.dothattask.Model.auth.AuthenticatedUser
import homeaq.dothattask.Model.auth.JwtConfig
import homeaq.dothattask.data.repository.EmailVerificationTokenRepository
import homeaq.dothattask.data.repository.RefreshTokenRepository
import homeaq.dothattask.data.repository.UserGroupRepository
import homeaq.dothattask.data.repository.UserRepository
import homeaq.dothattask.email.EmailConfig
import homeaq.dothattask.email.EmailService
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.HexFormat

class AuthService(
    private val jwt: JwtConfig,
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userGroupRepository: UserGroupRepository,
    private val emailVerificationTokens: EmailVerificationTokenRepository,
    private val emailService: EmailService,
    private val emailConfig: EmailConfig,
) {
    private val log = LoggerFactory.getLogger(AuthService::class.java)
    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /**
     * Email-first login. The [identifier] may be either an email (with `@`)
     * or — for retro-compatibility with seeded demo users / legacy clients —
     * a bare username.
     */
    suspend fun login(identifier: String, password: String): AuthTokens? {
        val user = userRepository.userByEmailOrUsername(identifier) ?: return null
        if (!PasswordHash.verifyPassword(password, user.password_hash)) return null
        return issueTokens(user)
    }

    sealed class RegisterResult {
        data class Success(val tokens: AuthTokens) : RegisterResult()
        data object EmailTaken : RegisterResult()
        data object UsernameTaken : RegisterResult()
        data object InvalidEmail : RegisterResult()
    }

    /**
     * Registers a new user. [email] is required and must be syntactically
     * valid + unique. [requestedUsername] is optional — when blank a unique
     * value is derived from the local-part of the email.
     *
     * On success the caller receives auth tokens immediately (account is
     * usable) AND a verification email is dispatched out-of-band so the
     * `email_verified` flag can be flipped later via [verifyEmail].
     */
    suspend fun register(
        name: String,
        email: String,
        password: String,
        requestedUsername: String?,
    ): RegisterResult {
        val normalisedEmail = email.trim().lowercase()
        if (!emailRegex.matches(normalisedEmail)) return RegisterResult.InvalidEmail

        if (userRepository.userByEmail(normalisedEmail) != null) {
            return RegisterResult.EmailTaken
        }

        val username = resolveUsername(requestedUsername, normalisedEmail)
            ?: return RegisterResult.UsernameTaken

        val created = userRepository.create(
            name = name,
            username = username,
            passwordHash = PasswordHash.hashPassword(password),
            email = normalisedEmail,
        )
        if (!created) {
            // Race on the unique index; if username collided we retried but
            // got beat — surface as username taken.
            return if (userRepository.userByEmail(normalisedEmail) != null) RegisterResult.EmailTaken
            else RegisterResult.UsernameTaken
        }

        val newUser = userRepository.userByUsername(username)
            ?: return RegisterResult.UsernameTaken

        runCatching { dispatchVerificationEmail(newUser) }
            .onFailure { log.warn("Verification email dispatch failed for {}: {}", username, it.message) }

        return RegisterResult.Success(issueTokens(newUser))
    }

    /**
     * Best-effort: produces a username that satisfies the unique index.
     * Honours an explicit [requested] choice when not blank; otherwise
     * generates from the email local-part with `_2`, `_3`, … suffixes
     * until we find an available slot. Returns null after 100 attempts to
     * avoid a runaway loop on pathological collisions.
     */
    private suspend fun resolveUsername(requested: String?, email: String): String? {
        val explicit = requested?.trim()?.takeIf { it.isNotEmpty() }
        if (explicit != null) {
            val sanitized = sanitizeUsername(explicit)
            if (sanitized.isEmpty()) return null
            return if (userRepository.userByUsername(sanitized) == null) sanitized else null
        }

        val base = sanitizeUsername(email.substringBefore('@'))
            .ifEmpty { "user" }

        if (userRepository.userByUsername(base) == null) return base
        for (i in 2..100) {
            val candidate = "${base}_$i"
            if (userRepository.userByUsername(candidate) == null) return candidate
        }
        return null
    }

    /** Mirrors the FE-side regex `^[a-zA-Z0-9_]+$` to keep usernames URL-safe. */
    private fun sanitizeUsername(raw: String): String =
        raw.lowercase().replace(Regex("[^a-z0-9_]"), "").take(50)

    /** Returns true if the token was valid and the user is now verified. */
    suspend fun verifyEmail(plainToken: String): Boolean {
        val tokenHash = sha256(plainToken)
        val record = emailVerificationTokens.findByHash(tokenHash) ?: return false
        if (record.usedAt != null) return false
        if (record.expiresAt.isBefore(Instant.now())) return false
        emailVerificationTokens.markUsed(tokenHash)
        return userRepository.markEmailVerified(record.userUsername)
    }

    /** Issues a fresh verification token and re-sends the email. */
    suspend fun resendVerificationEmail(username: String): Boolean {
        val user = userRepository.userByUsername(username) ?: return false
        if (user.email.isNullOrBlank() || user.emailVerified) return false
        return runCatching { dispatchVerificationEmail(user) }.isSuccess
    }

    private suspend fun dispatchVerificationEmail(user: User) {
        val email = user.email ?: return
        val plainToken = generateToken()
        val tokenHash = sha256(plainToken)
        val expiresAt = Instant.now().plus(emailConfig.verificationTtlHours, ChronoUnit.HOURS)
        emailVerificationTokens.create(user.username, tokenHash, expiresAt)
        val link = "${emailConfig.appBaseUrl}/api/auth/verify-email?token=$plainToken"
        emailService.sendVerificationEmail(email, user.name, link)
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
        return issueTokens(user)
    }

    suspend fun logout(plainRefreshToken: String) {
        refreshTokenRepository.revoke(sha256(plainRefreshToken))
    }

    private suspend fun issueTokens(user: User): AuthTokens {
        val access = jwt.generateAccessToken(user.username)
        val refresh = jwt.generateRefreshToken()
        refreshTokenRepository.create(user.username, sha256(refresh), jwt.refreshExpiry())
        val groups = userGroupRepository.groupsOfUser(user.username).map {
            GroupSummary(id = it.id, name = it.name, color = it.color)
        }
        return AuthTokens(
            accessToken = access,
            refreshToken = refresh,
            expiresIn = jwt.accessTtlMinutes * 60,
            user = AuthenticatedUser(
                username = user.username,
                name = user.name,
                email = user.email,
                emailVerified = user.emailVerified,
                groups = groups,
            ),
        )
    }

    private fun generateToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return HexFormat.of().formatHex(bytes)
    }

    private fun sha256(s: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
        return HexFormat.of().formatHex(digest)
    }
}
