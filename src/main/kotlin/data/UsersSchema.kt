package homeaq.dothattask.data
import homeaq.dothattask.Model.PasswordHash
import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.User
import homeaq.dothattask.Model.TaskUpdate
import java.sql.Connection


sealed class UsersSchema
{
    companion object
    {
        const val CREATE_TABLE_USERS =
            "CREATE TABLE IF NOT EXISTS USERS (ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "name TEXT NOT NULL, " +
                    "username TEXT NOT NULL UNIQUE," +
                    "password_hash TEXT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"

        const val SELECT_ALL = "SELECT name, username FROM users"
        const val SELECT_USER_BY_USERNAME = "SELECT * FROM users WHERE username = ?"
        const val GET_PASSWORD_HASH_BY_USERNAME = "SELECT password_hash FROM users WHERE username = ?"
    }
}

class UserTableSeedH2(private val connection: Connection) : ITableSeed
{
    override fun seed()
    {
        val users = listOf(
            User(name = "Valerio", username = "valerio99", password_hash = PasswordHash.hashPassword("password1")),
            User(name = "Jasmin", username = "jasmin99", password_hash = PasswordHash.hashPassword("password2")),
            User(name = "Nico", username = "nico99", password_hash = PasswordHash.hashPassword("password3")),
            User(name = "Fernanda", username = "fernanda99", password_hash = PasswordHash.hashPassword("password4")),

        )

        val statement = connection.prepareStatement("INSERT INTO users (name, username, password_hash) VALUES (?, ?, ?)")

        users.forEach {
            statement.setString(1, it.name)
            statement.setString(2, it.username)
            statement.setString(3, it.password_hash)
            statement.executeUpdate()
        }
    }
}

class UserTableSeedPostgres(private val connection: Connection) : ITableSeed
{
    override fun seed(){}
}

