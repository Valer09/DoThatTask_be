package homeaq.dothattask.data.repository

import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource

data class EmailVerificationToken(
    val id: Int,
    val userUsername: String,
    val tokenHash: String,
    val expiresAt: Instant,
    val usedAt: Instant?,
)

/**
 * Stores hashed email-verification tokens. The plain token is only known to
 * the user (delivered by email); the DB only ever sees its SHA-256 digest,
 * matching how refresh tokens are persisted.
 */
class EmailVerificationTokenRepository(
    private val dataSource: DataSource,
    factory: ITableFactory,
    seeder: ITableSeed,
) {
    init {
        dataSource.connection.use { connection ->
            factory.createTable(connection)
            seeder.seed(connection)
        }
    }

    suspend fun create(username: String, tokenHash: String, expiresAt: Instant): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "INSERT INTO email_verification_tokens (user_username, token_hash, expires_at) VALUES (?, ?, ?)"
            )
            stmt.setString(1, username.lowercase())
            stmt.setString(2, tokenHash)
            stmt.setTimestamp(3, Timestamp.from(expiresAt))
            stmt.executeUpdate()
        }
    }

    suspend fun findByHash(tokenHash: String): EmailVerificationToken? = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT id, user_username, token_hash, expires_at, used_at " +
                        "FROM email_verification_tokens WHERE token_hash = ?"
            )
            stmt.setString(1, tokenHash)
            val rs = stmt.executeQuery()
            if (!rs.next()) return@withContext null
            EmailVerificationToken(
                id = rs.getInt("id"),
                userUsername = rs.getString("user_username"),
                tokenHash = rs.getString("token_hash"),
                expiresAt = rs.getTimestamp("expires_at").toInstant(),
                usedAt = rs.getTimestamp("used_at")?.toInstant(),
            )
        }
    }

    suspend fun markUsed(tokenHash: String): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE email_verification_tokens SET used_at = CURRENT_TIMESTAMP " +
                        "WHERE token_hash = ? AND used_at IS NULL"
            ).use { stmt ->
                stmt.setString(1, tokenHash)
                stmt.executeUpdate() > 0
            }
        }
    }

    suspend fun deleteAllForUser(username: String): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                "DELETE FROM email_verification_tokens WHERE user_username = ?"
            ).use { stmt ->
                stmt.setString(1, username.lowercase())
                stmt.executeUpdate()
            }
        }
    }
}
