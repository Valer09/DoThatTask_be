package homeaq.dothattask.Model.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.config.ApplicationConfig
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.Date

class JwtConfig(config: ApplicationConfig) {
    val secret: String = config.property("ktor.jwt.secret").getString()
    val issuer: String = config.property("ktor.jwt.issuer").getString()
    val audience: String = config.property("ktor.jwt.audience").getString()
    val realm: String = config.property("ktor.jwt.realm").getString()
    val accessTtlMinutes: Long = config.property("ktor.jwt.accessTtlMinutes").getString().toLong()
    val refreshTtlDays: Long = config.property("ktor.jwt.refreshTtlDays").getString().toLong()

    private val algorithm: Algorithm = Algorithm.HMAC256(secret)
    private val random: SecureRandom = SecureRandom()

    fun generateAccessToken(username: String): String {
        val now = Instant.now()
        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withSubject(username)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(now.plusSeconds(accessTtlMinutes * 60)))
            .sign(algorithm)
    }

    fun generateRefreshToken(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    fun refreshExpiry(): Instant = Instant.now().plusSeconds(refreshTtlDays * 24 * 3600)

    fun verifier(): JWTVerifier =
        JWT.require(algorithm)
            .withIssuer(issuer)
            .withAudience(audience)
            .build()
}
