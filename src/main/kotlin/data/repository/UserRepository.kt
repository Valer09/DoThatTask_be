package homeaq.dothattask.data.repository

import homeaq.dothattask.Model.User
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import javax.sql.DataSource

class UserRepository(private val dataSource: DataSource, factory: ITableFactory, seeder: ITableSeed, isEmbedded: Boolean)
{
    private val _isEmb = isEmbedded
    private val SELECT_ALL = "SELECT name, username FROM users"
    private val SELECT_USER_BY_USERNAME = "SELECT * FROM users WHERE username = ?"
    private val SELECT_USER_BY_EMAIL = "SELECT * FROM users WHERE LOWER(email) = LOWER(?)"
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
            AND(
            reminder_last_sent IS NULL
            OR (reminder_consecutive_unopened < ? AND reminder_last_sent < DATEADD('HOUR', -?, NOW())))""".trimIndent()

    init {
        dataSource.connection.use { connection ->
            factory.createTable(connection)
            seeder.seed(connection)
        }
    }

    suspend fun allInGroup(groupId: Int): List<User> = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT u.name, u.username, u.email FROM users u " +
                        "JOIN user_groups ug ON ug.user_username = u.username " +
                        "WHERE ug.group_id = ?"
            )
            stmt.setInt(1, groupId)
            val rs = stmt.executeQuery()
            val result = mutableListOf<User>()
            while (rs.next()) {
                result.add(User(
                    name = rs.getString("name"),
                    username = rs.getString("username"),
                    password_hash = "better_not",
                    email = rs.getString("email"),
                ))
            }
            result
        }
    }

    suspend fun userByUsername(username: String): User? = withContext(Dispatchers.IO)
    {
        dataSource.connection.use { connection ->
            val statement = connection.prepareStatement(SELECT_USER_BY_USERNAME)
            statement.setString(1, username.lowercase())
            val resultSet = statement.executeQuery()

            if (resultSet.next()) {
                return@withContext rowToUser(resultSet)
            }
            null
        }
    }

    suspend fun userByEmail(email: String): User? = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(SELECT_USER_BY_EMAIL)
            stmt.setString(1, email.trim())
            val rs = stmt.executeQuery()
            if (rs.next()) rowToUser(rs) else null
        }
    }

    /** Lookup that accepts either an email (with `@`) or a legacy username. */
    suspend fun userByEmailOrUsername(identifier: String): User? {
        val trimmed = identifier.trim()
        return if (trimmed.contains('@')) userByEmail(trimmed) else userByUsername(trimmed)
    }

    private fun rowToUser(rs: java.sql.ResultSet): User = User(
        name = rs.getString("name"),
        username = rs.getString("username"),
        password_hash = rs.getString("password_hash"),
        email = runCatching { rs.getString("email") }.getOrNull(),
        emailVerified = runCatching { rs.getBoolean("email_verified") }.getOrDefault(false),
    )

    /**
     * Creates a user with optional [email]. The email is stored normalised
     * (lower-cased, trimmed); `null`/blank emails skip the column to avoid
     * collisions with the unique index. New users start as unverified — call
     * [markEmailVerified] after a successful confirmation token redeem.
     */
    suspend fun create(
        name: String,
        username: String,
        passwordHash: String,
        email: String? = null,
    ): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val normalisedUsername = username.lowercase()
            val normalisedEmail = email?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()

            val byUsername = connection.prepareStatement(SELECT_USER_BY_USERNAME).apply {
                setString(1, normalisedUsername)
            }.executeQuery()
            if (byUsername.next()) return@withContext false

            if (normalisedEmail != null) {
                val byEmail = connection.prepareStatement(SELECT_USER_BY_EMAIL).apply {
                    setString(1, normalisedEmail)
                }.executeQuery()
                if (byEmail.next()) return@withContext false
            }

            val insert = connection.prepareStatement(
                "INSERT INTO users (name, username, password_hash, email, email_verified) " +
                        "VALUES (?, ?, ?, ?, ?)"
            )
            insert.setString(1, name)
            insert.setString(2, normalisedUsername)
            insert.setString(3, passwordHash)
            if (normalisedEmail != null) insert.setString(4, normalisedEmail)
            else insert.setNull(4, java.sql.Types.VARCHAR)
            insert.setBoolean(5, false)
            insert.executeUpdate() == 1
        }
    }

    suspend fun markEmailVerified(username: String): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            connection.prepareStatement("UPDATE users SET email_verified = TRUE WHERE username = ?").use { stmt ->
                stmt.setString(1, username.lowercase())
                stmt.executeUpdate() > 0
            }
        }
    }

    suspend fun updatePasswordHash(username: String, newHash: String): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement("UPDATE users SET password_hash = ? WHERE username = ?")
            stmt.setString(1, newHash)
            stmt.setString(2, username.lowercase())
            stmt.executeUpdate()
        }
    }

    suspend fun passwordHashByUsername(username: String): String = withContext(Dispatchers.IO)
    {
        dataSource.connection.use { connection ->
            var statement = connection.prepareStatement(SELECT_USER_BY_USERNAME)
            statement.setString(1, username.lowercase())
            var resultSet = statement.executeQuery()
            if (!resultSet.next()) throw NotFoundException("User does not exists")

            statement = connection.prepareStatement(GET_PASSWORD_HASH_BY_USERNAME)
            statement.setString(1, username.lowercase())
            resultSet = statement.executeQuery()

            if (resultSet.next())
                return@withContext resultSet.getString("password_hash") else throw Exception("User does not have a password stored")
        }
    }

    suspend fun getUsersEligibleForReminder() : List<User> = withContext(Dispatchers.IO){
        dataSource.connection.use { connection ->
            val stmt = when(_isEmb) {
                true -> connection.prepareStatement(FIND_USERS_FOR_REMINDER_H2)
                false -> connection.prepareStatement(FIND_USERS_FOR_REMINDER)
            }

            stmt.setInt(1, 7)
            stmt.setInt(2, 20)

            val resultSet = stmt.executeQuery()
            val usersToRemind = mutableListOf<User>()

            while (resultSet.next()) {
                usersToRemind.add(rowToUser(resultSet))
            }

            usersToRemind
        }
    }

    suspend fun resetUnopenedReminders(username: String) : Boolean = withContext(Dispatchers.IO){
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                        UPDATE users
                        SET reminder_consecutive_unopened = 0,
                            reminder_last_opened = NOW(),
                            reminder_enabled = TRUE
                        WHERE username = ?
                        """
            ).use { stmt ->
                stmt.setString(1, username)
                stmt.executeUpdate() > 0
            }
        }
    }

    suspend fun incrementUnopenedReminders(username: String) : Boolean = withContext(Dispatchers.IO){
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                        UPDATE users
                        SET reminder_consecutive_unopened = reminder_consecutive_unopened + 1,
                            reminder_last_sent = NOW(),
                            reminder_enabled = CASE
                                WHEN reminder_consecutive_unopened + 1 >= 7 THEN FALSE
                                ELSE TRUE
                            END
                        WHERE username = ?
                        """
            ).use { stmt ->
                stmt.setString(1, username)
                stmt.executeUpdate() > 0
            }
        }
    }

    suspend fun reactivateUserNotification(username: String): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                        UPDATE users
                        SET reminder_consecutive_unopened = 0,
                            reminder_last_opened = NOW(),
                            reminder_enabled = TRUE
                        WHERE username = ?
                        """.trimIndent()
            ).use { stmt ->
                stmt.setString(1, username)
                stmt.executeUpdate() > 0
            }
        }
    }
}
