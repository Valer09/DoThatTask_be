package homeaq.dothattask.data.repository

import homeaq.dothattask.Model.User
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

class UserRepository(private val connection: Connection, factory: ITableFactory, seeder: ITableSeed, isEmbedded: Boolean)
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
            AND(
            reminder_last_sent IS NULL 
            OR (reminder_consecutive_unopened < ? AND reminder_last_sent < DATEADD('HOUR', -?, NOW())))""".trimIndent()

    init {
        factory.createTable(connection)
        seeder.seed(connection)
    }

    suspend fun allInGroup(groupId: Int): List<User> = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "SELECT u.name, u.username FROM users u " +
                    "JOIN user_groups ug ON ug.user_username = u.username " +
                    "WHERE ug.group_id = ?"
        )
        stmt.setInt(1, groupId)
        val rs = stmt.executeQuery()
        val result = mutableListOf<User>()
        while (rs.next()) {
            result.add(User(rs.getString("name"), rs.getString("username"), "better_not"))
        }
        result
    }

/*    suspend fun all(): List<User> = withContext(Dispatchers.IO)
    {
        val statement = connection.prepareStatement(SELECT_ALL)

        val resultSet = statement.executeQuery()

        val result = mutableListOf<User>()
        while (resultSet.next()) {
            result.add(
                User(
                    resultSet.getString("name"),
                    resultSet.getString("username"),

                    //WARNING: DO YOU REALLY NEED?
                    //resultSet.getString("password_hash"),

                    "better_not"
                )
            )
        }

        return@withContext result
    }*/

    suspend fun userByUsername(username: String): User? = withContext(Dispatchers.IO)
    {
        val statement = connection.prepareStatement(SELECT_USER_BY_USERNAME)
        statement.setString(1, username.lowercase())
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            return@withContext User(
                resultSet.getString("name"),
                resultSet.getString("username"),
                resultSet.getString("password_hash"),
            )
        }
        null
    }

    suspend fun create(name: String, username: String, passwordHash: String): Boolean = withContext(Dispatchers.IO) {
        val exists = connection.prepareStatement(SELECT_USER_BY_USERNAME).apply {
            setString(1, username.lowercase())
        }.executeQuery()
        if (exists.next()) return@withContext false

        val insert = connection.prepareStatement(
            "INSERT INTO users (name, username, password_hash) VALUES (?, ?, ?)"
        )
        insert.setString(1, name)
        insert.setString(2, username.lowercase())
        insert.setString(3, passwordHash)
        insert.executeUpdate() == 1
    }

    suspend fun updatePasswordHash(username: String, newHash: String): Unit = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("UPDATE users SET password_hash = ? WHERE username = ?")
        stmt.setString(1, newHash)
        stmt.setString(2, username.lowercase())
        stmt.executeUpdate()
    }

    suspend fun passwordHashByUsername(username: String): String = withContext(Dispatchers.IO)
    {

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

    suspend fun getUsersEligibleForReminder() : List<User> = withContext(Dispatchers.IO){

        val stmt = when(_isEmb) {
            true -> connection.prepareStatement(FIND_USERS_FOR_REMINDER_H2)
            false -> connection.prepareStatement(FIND_USERS_FOR_REMINDER)
        }

        stmt.setInt(1, 7)
        stmt.setInt(2, 20)

        val resultSet = stmt.executeQuery()
        val usersToRemind = mutableListOf<User>()

        while (resultSet.next()) {
            usersToRemind.add(User(resultSet.getString("name"), resultSet.getString("username"), "better_not"))
        }

        usersToRemind
    }

    suspend fun resetUnopenedReminders(username: String) : Boolean = withContext(Dispatchers.IO){

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

    suspend fun incrementUnopenedReminders(username: String) : Boolean = withContext(Dispatchers.IO){
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

    suspend fun reactivateUserNotification(username: String): Boolean = withContext(Dispatchers.IO) {
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