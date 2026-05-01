package homeaq.dothattask.data.repository

import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.sql.DataSource

class FcmTokenRepository(
    private val dataSource: DataSource,
    factory: ITableFactory,
    seeder: ITableSeed,
) {
    init {
        dataSource.connection.use { conn ->
            factory.createTable(conn)
            seeder.seed(conn)
        }
    }

    suspend fun register(username: String, token: String, platform: String = "android"): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val del = conn.prepareStatement("DELETE FROM fcm_tokens WHERE token = ?")
            del.setString(1, token)
            del.executeUpdate()

            val ins = conn.prepareStatement(
                "INSERT INTO fcm_tokens (user_username, token, platform) VALUES (?, ?, ?)"
            )
            ins.setString(1, username.lowercase())
            ins.setString(2, token)
            ins.setString(3, platform)
            ins.executeUpdate()
        }
    }

    suspend fun unregister(token: String): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement("DELETE FROM fcm_tokens WHERE token = ?")
            stmt.setString(1, token)
            stmt.executeUpdate() > 0
        }
    }

    suspend fun tokensFor(username: String): List<String> = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement("SELECT token FROM fcm_tokens WHERE user_username = ?")
            stmt.setString(1, username.lowercase())
            val rs = stmt.executeQuery()
            buildList { while (rs.next()) add(rs.getString("token")) }
        }
    }

    suspend fun deleteTokens(tokens: Collection<String>): Unit = withContext(Dispatchers.IO) {
        if (tokens.isEmpty()) return@withContext
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement("DELETE FROM fcm_tokens WHERE token = ?")
            tokens.forEach {
                stmt.setString(1, it)
                stmt.addBatch()
            }
            stmt.executeBatch()
        }
    }
}
