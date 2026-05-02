package homeaq.dothattask.data.repository

import homeaq.dothattask.Model.RefreshToken
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Timestamp
import java.time.Instant
import javax.sql.DataSource

class RefreshTokenRepository(
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

    suspend fun create(userUsername: String, tokenHash: String, expiresAt: Instant): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "INSERT INTO refresh_tokens (user_username, token_hash, expires_at) VALUES (?, ?, ?)"
            )
            stmt.setString(1, userUsername)
            stmt.setString(2, tokenHash)
            stmt.setTimestamp(3, Timestamp.from(expiresAt))
            stmt.executeUpdate()
        }
    }

    suspend fun findByHash(tokenHash: String): RefreshToken? = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT id, user_username, token_hash, issued_at, expires_at, revoked_at " +
                        "FROM refresh_tokens WHERE token_hash = ?"
            )
            stmt.setString(1, tokenHash)
            val rs = stmt.executeQuery()
            if (!rs.next()) return@withContext null
            RefreshToken(
                id = rs.getInt("id"),
                userUsername = rs.getString("user_username"),
                tokenHash = rs.getString("token_hash"),
                issuedAt = rs.getTimestamp("issued_at").toInstant(),
                expiresAt = rs.getTimestamp("expires_at").toInstant(),
                revokedAt = rs.getTimestamp("revoked_at")?.toInstant(),
            )
        }
    }

    suspend fun revoke(tokenHash: String): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "UPDATE refresh_tokens SET revoked_at = CURRENT_TIMESTAMP " +
                        "WHERE token_hash = ? AND revoked_at IS NULL"
            )
            stmt.setString(1, tokenHash)
            stmt.executeUpdate()
        }
    }

    suspend fun revokeAllForUser(userUsername: String): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "UPDATE refresh_tokens SET revoked_at = CURRENT_TIMESTAMP " +
                        "WHERE user_username = ? AND revoked_at IS NULL"
            )
            stmt.setString(1, userUsername)
            stmt.executeUpdate()
        }
    }
}
