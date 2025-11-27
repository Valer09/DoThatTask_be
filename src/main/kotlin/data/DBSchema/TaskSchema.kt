package homeaq.dothattask.data.DBSchema

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskCategory
import homeaq.dothattask.Model.TaskStatus
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import java.sql.Connection


sealed class TasksSchema
{
    companion object {
        const val CREATE_TABLE_TASKS =
            "CREATE TABLE IF NOT EXISTS TASKS (ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "name VARCHAR(150) NOT NULL UNIQUE," +
                    "category INTEGER NOT NULL," +
                    "status INTEGER NOT NULL," +
                    "description VARCHAR(255)," +
                    "user_username VARCHAR(150) NOT NULL REFERENCES users(username) ON DELETE CASCADE)"

    }
}

class TaskTableFactoryH2 : ITableFactory
{

    override fun createTable(connection: Connection)
    {
        connection.createStatement().executeUpdate(UsersSchema.CREATE_TABLE_USERS)
        connection.createStatement().executeUpdate(TasksSchema.CREATE_TABLE_TASKS)
    }
}


class TaskTableFactoryPostgres : ITableFactory
{
    override fun createTable(connection: Connection)
    {
        connection.createStatement().executeUpdate(TasksSchema.CREATE_TABLE_TASKS)
    }
}


class TaskTableSeedH2() : ITableSeed
{
    override fun seed(connection: Connection)
    {
        UserTableSeedH2().seed(connection)
        val demoUsers = UserTableSeedH2.Companion.demoUsers()

        val tasks = listOf(
            Task("Cleaning", "Clean the house", TaskCategory.Career, TaskStatus.TODO, demoUsers[0].username),
            Task("Gardening", "Mow the lawn", TaskCategory.Career,TaskStatus.TODO, demoUsers[1 % demoUsers.size].username),
            Task("Shopping", "Buy the groceries", TaskCategory.Career,TaskStatus.TODO, demoUsers[2 % demoUsers.size].username),
            Task("Painting", "Paint the fence", TaskCategory.Health,TaskStatus.COMPLETED, demoUsers[3 % demoUsers.size].username),
            Task("Cooking", "Cook the dinner", TaskCategory.Social,TaskStatus.TODO, demoUsers[4 % demoUsers.size].username),
            Task("Relaxing", "Take a walk", TaskCategory.Career,TaskStatus.TODO, demoUsers[5 % demoUsers.size].username),
            Task("Exercising", "Go to the gym", TaskCategory.Social,TaskStatus.TODO, demoUsers[6 % demoUsers.size].username),
            Task("Learning", "Read a book", TaskCategory.Career,TaskStatus.ACTIVE, demoUsers[7 % demoUsers.size].username),
            Task("Snoozing", "Go for a nap", TaskCategory.Career,TaskStatus.TODO, demoUsers[8 % demoUsers.size].username),
            Task("Socializing", "Go to a party", TaskCategory.Health,TaskStatus.TODO, demoUsers[9 % demoUsers.size].username),
        )

        val statement = connection.prepareStatement("MERGE INTO tasks (name, category, status, description, user_username) KEY(name) VALUES (?, ?, ?, ?, ?)")

        var count = 0
        tasks.forEach {

            statement.setString(1, it.name)
            statement.setInt(2, it.category.code)
            statement.setInt(3, it.status.code)
            statement.setString(4, it.description)
            statement.setString(5, it.ownership_username)
            statement.executeUpdate()
            count ++
            count %= demoUsers.size
        }
    }
}

class TaskTableSeedPostgres() : ITableSeed
{
    override fun seed(connection: Connection){}
}