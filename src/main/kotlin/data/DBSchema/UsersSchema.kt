package homeaq.dothattask.data.DBSchema
import homeaq.dothattask.Model.PasswordHash
import homeaq.dothattask.Model.User
import homeaq.dothattask.data.DBSchema.UsersSchema.Companion.CREATE_TABLE_USERS
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import java.sql.Connection
import java.sql.SQLException


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

        const val ALTER_USERS_ADD_REMINDER_COLUMNS =
            "ALTER TABLE users " +
                    "ADD COLUMN IF NOT EXISTS reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE," +
                    "ADD COLUMN IF NOT EXISTS reminder_consecutive_unopened INTEGER NOT NULL DEFAULT 0," +
                    "ADD COLUMN IF NOT EXISTS reminder_last_sent TIMESTAMP," +
                    "ADD COLUMN IF NOT EXISTS reminder_last_opened TIMESTAMP"

        const val CREATE_REMINDER_INDEX =
            "CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_reminder_efficiency " +
                    "ON users (reminder_last_sent) " +
                    "WHERE reminder_enabled = TRUE"

    }
}

class UserTableFactoryH2 : ITableFactory
{
    override fun createTable(connection: Connection)
    {
        val ALTER_USERS_ADD_REMINDER_ENABLED = "ALTER TABLE users ADD COLUMN IF NOT EXISTS reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE"
        val ALTER_USERS_ADD_REMINDER_CONSECUTIVE_UNOPENED = "ALTER TABLE users ADD COLUMN IF NOT EXISTS reminder_consecutive_unopened INTEGER NOT NULL DEFAULT 0"
        val ALTER_USERS_ADD_REMINDER_LAST_SENT = "ALTER TABLE users ADD COLUMN IF NOT EXISTS reminder_last_sent TIMESTAMP"
        val ALTER_USERS_ADD_REMINDER_LAST_OPENED = "ALTER TABLE users ADD COLUMN IF NOT EXISTS reminder_last_opened TIMESTAMP"

        try {

            val wasAutoCommit = connection.autoCommit
            connection.autoCommit = true

            listOf(
                CREATE_TABLE_USERS,
                ALTER_USERS_ADD_REMINDER_ENABLED,
                ALTER_USERS_ADD_REMINDER_CONSECUTIVE_UNOPENED,
                ALTER_USERS_ADD_REMINDER_LAST_SENT,
                ALTER_USERS_ADD_REMINDER_LAST_OPENED
            ).forEach { sql ->
                connection.createStatement().execute(sql)
            }

            connection.autoCommit = wasAutoCommit
            println("DB Schema successfully verified.")

        }
        catch (e: SQLException)
        {
            println("Error happened during database initialization: ${e.message}")
        }
    }
}

class UserTableFactoryPostgres : ITableFactory
{
    override fun createTable(connection: Connection)
    {
        try {

            val wasAutoCommit = connection.autoCommit
            connection.autoCommit = true

            connection.createStatement().use { statement ->
                //TODO: VERIFY
                statement.executeUpdate(UsersSchema.CREATE_TABLE_USERS_PG)
                statement.executeUpdate(UsersSchema.ALTER_USERS_ADD_REMINDER_COLUMNS)
                statement.executeUpdate(UsersSchema.CREATE_REMINDER_INDEX)
            }

            connection.autoCommit = wasAutoCommit
            println("DB Schema successfully verified.")

        }
        catch (e: SQLException)
        {
            println("Error happened during database initialization: ${e.message}")
        }
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

        demoUsersAlt().forEach {
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

        fun demoUsersAlt(): List<User> {
            val users = listOf(
                User(name = "Francesca", username = "francesca99", password_hash = PasswordHash.hashPassword("password5")),
                User(name = "Paolino", username = "paolino99", password_hash = PasswordHash.hashPassword("password6")),
            )
            return users
        }

    }
}

class UserTableSeedPostgres() : ITableSeed
{
    override fun seed(connection: Connection){}
}

