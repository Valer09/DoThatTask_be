package homeaq.dothattask.data.repository

import homeaq.dothattask.Model.User
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import homeaq.dothattask.data.DBSchema.UsersSchema
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

class UserRepository(private val connection: Connection, factory: ITableFactory, seeder: ITableSeed)
{
    private val SELECT_ALL = "SELECT name, username FROM users"
    private val SELECT_USER_BY_USERNAME = "SELECT * FROM users WHERE username = ?"
    private val GET_PASSWORD_HASH_BY_USERNAME = "SELECT password_hash FROM users WHERE username = ?"

    init {
        factory.createTable(connection)
        seeder.seed(connection)
    }

    suspend fun all(): List<User> = withContext(Dispatchers.IO)
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
    }

    suspend fun userByUsername(username: String): User? = withContext(Dispatchers.IO)
    {
        val statement = connection.prepareStatement(SELECT_USER_BY_USERNAME)
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

        var statement = connection.prepareStatement(SELECT_USER_BY_USERNAME)
        statement.setString(1, username)
        var resultSet = statement.executeQuery()
        if (!resultSet.next()) throw NotFoundException("User does not exists")

        statement = connection.prepareStatement(GET_PASSWORD_HASH_BY_USERNAME)
        statement.setString(1, username)
        resultSet = statement.executeQuery()

        if (resultSet.next())
            return@withContext resultSet.getString("password_hash") else throw Exception("User does not have a password stored")
    }
}