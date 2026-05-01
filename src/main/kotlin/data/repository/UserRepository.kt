package homeaq.dothattask.data.repository

import homeaq.dothattask.Model.User
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.sql.DataSource

class UserRepository(private val dataSource: DataSource, factory: ITableFactory, seeder: ITableSeed, isEmbedded: Boolean)
{
    private val _isEmb = isEmbedded
    private val SELECT_ALL = "SELECT name, username FROM users"
    private val SELECT_USER_BY_USERNAME = "SELECT * FROM users WHERE username = ?"
    private val GET_PASSWORD_HASH_BY_USERNAME = "SELECT password_hash FROM users WHERE username = ?"
    private val FIND_USERS_FOR_REMINDER =
        """SELECT * FROM users 
           WHERE reminder_enabled = TRUE 
           AND(
               reminder_last_sent IS NULL 
               OR (reminder_consecutive_unopened < ? AND reminder_last_sent < NOW() - (? * INTERVAL '1' HOUR))
           )""".trimIndent()

    private val FIND_USERS_FOR_REMINDER_H2 =
        """SELECT * FROM users 
            WHERE reminder_enabled = TRUE 
            AND (
                reminder_last_sent IS NULL
                OR (reminder_consecutive_unopened < ? AND reminder_last_sent < DATEADD('HOUR', -?, NOW()))
            )""".trimIndent()

    private val INSERT_USER =
        "INSERT INTO users (name, username, password_hash) VALUES (?, ?, ?)"
    private val DELETE_USER = "DELETE FROM users WHERE username = ?"
    private val UPDATE_REMINDER =
        "UPDATE users SET reminder_enabled = ?, reminder_interval_hours = ? WHERE username = ?"
    private val MARK_REMINDER_SENT =
        "UPDATE users SET reminder_last_sent = NOW(), reminder_consecutive_unopened = reminder_consecutive_unopened + 1 WHERE username = ?"
    private val RESET_REMINDER_STREAK =
        "UPDATE users SET reminder_consecutive_unopened = 0 WHERE username = ?"

    init {
        dataSource.connection.use { conn ->
            factory.createTable(conn)
            seeder.seed(conn)
        }
    }

    suspend fun allUsers(): List<User> = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val rs = conn.prepareStatement(SELECT_ALL).executeQuery()
            buildList { while (rs.next()) add(User(rs.getString("name"), rs.getString("username"), "")) }
        }
    }

    suspend fun userByUsername(username: String): User? = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement(SELECT_USER_BY_USERNAME)
            stmt.setString(1, username.lowercase())
            val rs = stmt.executeQuery()
            if (rs.next()) User(rs.getString("name"), rs.getString("username"), rs.getString("password_hash"))
            else null
        }
    }

    suspend fun getPasswordHashByUsername(username: String): String? = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement(GET_PASSWORD_HASH_BY_USERNAME)
            stmt.setString(1, username.lowercase())
            val rs = stmt.executeQuery()
            if (rs.next()) rs.getString("password_hash") else null
        }
    }

    suspend fun createUser(user: User): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement(INSERT_USER)
            stmt.setString(1, user.name)
            stmt.setString(2, user.username.lowercase())
            stmt.setString(3, user.password_hash)
            stmt.executeUpdate()
        }
    }

    suspend fun deleteUser(username: String): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement(DELETE_USER)
            stmt.setString(1, username.lowercase())
            stmt.executeUpdate()
        }
    }

    suspend fun updateReminder(username: String, enabled: Boolean, intervalHours: Int): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement(UPDATE_REMINDER)
            stmt.setBoolean(1, enabled)
            stmt.setInt(2, intervalHours)
            stmt.setString(3, username.lowercase())
            stmt.executeUpdate()
        }
    }

    suspend fun usersForReminder(maxConsecutive: Int, intervalHours: Int): List<User> = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val sql = if (_isEmb) FIND_USERS_FOR_REMINDER_H2 else FIND_USERS_FOR_REMINDER
            val stmt = conn.prepareStatement(sql)
            stmt.setInt(1, maxConsecutive)
            stmt.setInt(2, intervalHours)
            val rs = stmt.executeQuery()
            buildList {
                while (rs.next()) add(
                    User(rs.getString("name"), rs.getString("username"), rs.getString("password_hash"))
                )
            }
        }
    }

    suspend fun markReminderSent(username: String): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement(MARK_REMINDER_SENT)
            stmt.setString(1, username.lowercase())
            stmt.executeUpdate()
        }
    }

    suspend fun resetReminderStreak(username: String): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement(RESET_REMINDER_STREAK)
            stmt.setString(1, username.lowercase())
            stmt.executeUpdate()
        }
    }
}
