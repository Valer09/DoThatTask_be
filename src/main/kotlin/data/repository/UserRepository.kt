package homeaq.dothattask.data.repository

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskCategory
import homeaq.dothattask.Model.TaskStatus
import homeaq.dothattask.Model.User
import homeaq.dothattask.data.ITableSeed
import homeaq.dothattask.data.UsersSchema
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

class UserRepository(private val connection: Connection, private val seeder: ITableSeed)
{


    init {
        val statement = connection.createStatement()
        statement.executeUpdate(UsersSchema.Companion.CREATE_TABLE_USERS)
        seeder.seed()
    }

    suspend fun all(): List<User> = withContext(Dispatchers.IO)
    {
        val statement = connection.prepareStatement(UsersSchema.SELECT_ALL)

        val resultSet = statement.executeQuery()

        val result = mutableListOf<User>()
        while (resultSet.next()) {
            result.add(
                User(
                    resultSet.getString("name"),
                    resultSet.getString("username"),

                    // DO YOU REALLY NEED?
                    //resultSet.getString("password_hash"),

                    "better_not"
                )
            )
        }

        return@withContext result
    }

    suspend fun userByUsername(username: String): User? = withContext(Dispatchers.IO)
    {
        val statement = connection.prepareStatement(UsersSchema.Companion.SELECT_USER_BY_USERNAME)
        statement.setString(1, username)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            return@withContext User(
                resultSet.getString("name"),
                resultSet.getString("username"),

                // DO YOU REALLY NEED?
                //resultSet.getString("password_hash"),

                "better_not"
            )
        }
        null
    }

    suspend fun passwordHashByUsername(username: String): String = withContext(Dispatchers.IO)
    {

        var statement = connection.prepareStatement(UsersSchema.SELECT_USER_BY_USERNAME)
        statement.setString(1, username)
        var resultSet = statement.executeQuery()
        if (!resultSet.next()) throw NotFoundException("User does not exists")

        statement = connection.prepareStatement(UsersSchema.GET_PASSWORD_HASH_BY_USERNAME)
        statement.setString(1, username)
        resultSet = statement.executeQuery()

        if (resultSet.next())
            return@withContext resultSet.getString("password_hash") else throw Exception("User does not have a password stored")
    }
}