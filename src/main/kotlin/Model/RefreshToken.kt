package homeaq.dothattask.Model

import java.time.Instant

data class RefreshToken(
    val id: Int,
    val userUsername: String,
    val tokenHash: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val revokedAt: Instant?,
)
