package homeaq.dothattask.data.DBSchema
import homeaq.dothattask.Model.PasswordHash
import homeaq.dothattask.Model.User
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import java.sql.Connection


sealed class UsersSchema
{
    companion object
    {
        const val CREATE_TABLE_USERS =
            "CREATE TABLE IF NOT EXISTS USERS (ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "name VARCHAR(150) NOT NULL, " +
                    "username VARCHAR(150) NOT NULL UNIQUE," +
                    "password_hash VARCHAR(255) NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"

        const val CREATE_TABLE_USERS_PG =
            "CREATE TABLE IF NOT EXISTS USERS (ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "name VARCHAR(150) NOT NULL, " +
                    "username CITEXT NOT NULL UNIQUE CHECK (length(username) <= 150)," +
                    "password_hash VARCHAR(255) NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
    }
}

class UserTableFactoryH2 : ITableFactory
{
    override fun createTable(connection: Connection)
    {
        connection.createStatement().executeUpdate(UsersSchema.CREATE_TABLE_USERS)
    }
}

class UserTableFactoryPostgres : ITableFactory
{
    override fun createTable(connection: Connection)
    {
        //TODO: VERIFY
        connection.createStatement().executeUpdate(UsersSchema.CREATE_TABLE_USERS_PG)
    }
}

class UserTableSeedH2() : ITableSeed
{
    override fun seed(connection: Connection)
    {
        val statement = connection.prepareStatement("MERGE INTO users (name, username, password_hash) KEY(username) VALUES (?, ?, ?)")

        demoUsers().forEach {
            statement.setString(1, it.name)
            statement.setString(2, it.username)
            statement.setString(3, it.password_hash)
            statement.executeUpdate()
        }
    }
    companion object
    {
        // WARNING: Test-only seed for embedded H2 database.
        // These users are seeded ONLY when DB_EMB=true (local development/testing).
        // Do NOT use in production — passwords are intentionally weak placeholders.
        fun demoUsers(): List<User> {
            val users = listOf(
                User(name = "Valerio", username = "valerio99", password_hash = PasswordHash.hashPassword("password1")),
                User(name = "Jasmin", username = "jasmin99", password_hash = PasswordHash.hashPassword("password2")),
                User(name = "Nico", username = "nico99", password_hash = PasswordHash.hashPassword("password3")),
                User(name = "Fernanda", username = "fernanda99", password_hash = PasswordHash.hashPassword("password4")),
            )
            return users
        }
    }
}

class UserTableSeedPostgres() : ITableSeed
{
    override fun seed(connection: Connection){}
}

