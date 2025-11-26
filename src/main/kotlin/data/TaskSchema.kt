package homeaq.dothattask.data

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskCategory
import homeaq.dothattask.Model.TaskStatus
import java.sql.Connection


sealed class TasksSchema
{
    companion object {
        const val CREATE_TABLE_TASKS =
            "CREATE TABLE IF NOT EXISTS TASKS (ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "name TEXT NOT NULL UNIQUE, " +
                    "category INTEGER NOT NULL," +
                    "status INTEGER NOT NULL," +
                    "description TEXT);"
        const val SELECT_TASK_BY_ID = "SELECT * FROM tasks WHERE id = ?"
        const val SELECT_TASK_BY_NAME = "SELECT * FROM tasks WHERE name = ?"
        const val INSERT_TASK = "INSERT INTO tasks (name, category, status, description) VALUES (?, ?, ?, ?)"
        const val UPDATE_TASK = "UPDATE tasks SET name = ?, category = ?, status = ?, description = ? WHERE name = ?"
        const val DELETE_TASK = "DELETE FROM tasks WHERE name = ?"

    }
}


class TaskTableSeedH2(private val connection: Connection) : ITableSeed
{
    private val tasks = listOf(
        Task("Cleaning", "Clean the house", TaskCategory.Career, TaskStatus.TODO),
        Task("Gardening", "Mow the lawn", TaskCategory.Career,TaskStatus.TODO),
        Task("Shopping", "Buy the groceries", TaskCategory.Career,TaskStatus.TODO),
        Task("Painting", "Paint the fence", TaskCategory.Health,TaskStatus.COMPLETED),
        Task("Cooking", "Cook the dinner", TaskCategory.Social,TaskStatus.TODO),
        Task("Relaxing", "Take a walk", TaskCategory.Career,TaskStatus.TODO),
        Task("Exercising", "Go to the gym", TaskCategory.Social,TaskStatus.TODO),
        Task("Learning", "Read a book", TaskCategory.Career,TaskStatus.ACTIVE),
        Task("Snoozing", "Go for a nap", TaskCategory.Career,TaskStatus.TODO),
        Task("Socializing", "Go to a party", TaskCategory.Health,TaskStatus.TODO),
    )

    override fun seed()
    {
        val statement = connection.prepareStatement("INSERT INTO tasks (name, category, status, description) VALUES (?, ?, ?, ?)")

        tasks.forEach {
            statement.setString(1, it.name)
            statement.setInt(2, it.category.code)
            statement.setInt(3, it.status.code)
            statement.setString(4, it.description)
            statement.executeUpdate()
        }
    }
}

class TaskTableSeedPostgres(private val connection: Connection) : ITableSeed
{
    override fun seed(){}
}