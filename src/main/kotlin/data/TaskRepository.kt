package homeaq.dothattask.data
import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskCategory
import homeaq.dothattask.Model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.Statement

class TaskRepository(private val connection: Connection, private val seeder: ITaskTableSeed)
{
    companion object {
        private const val CREATE_TABLE_TASKS =
            "CREATE TABLE IF NOT EXISTS TASKS (ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY, " +
                    "name TEXT NOT NULL UNIQUE, " +
                    "category INTEGER NOT NULL," +
                    "status INTEGER NOT NULL," +
                    "description TEXT);"
        private const val SELECT_TASK_BY_ID = "SELECT * FROM tasks WHERE id = ?"
        private const val SELECT_TASK_BY_NAME = "SELECT * FROM tasks WHERE name = ?"
        private const val INSERT_TASK = "INSERT INTO tasks (name, category, status, description) VALUES (?, ?, ?, ?)"
        private const val UPDATE_TASK = "UPDATE tasks SET name = ?, category = ?, status = ?, description = ? WHERE name = ?"
        private const val DELETE_TASK = "DELETE FROM tasks WHERE name = ?"

    }

    init {
        val statement = connection.createStatement()
        statement.executeUpdate(CREATE_TABLE_TASKS)
        seeder.seed()
    }


    suspend fun allTasks(): List<Task> = withContext(Dispatchers.IO)
    {
        val statement = connection.prepareStatement("SELECT * FROM tasks")
        val resultSet = statement.executeQuery()
        val result = mutableListOf<Task>()
        while (resultSet.next()) {
            result.add(Task(
                name = resultSet.getString("name"),
                category = TaskCategory.fromCode(resultSet.getInt("category")),
                status = TaskStatus.fromCode(resultSet.getInt("status")),
                description = resultSet.getString("description")
            ))
        }

        return@withContext result
    }

    suspend fun taskById(id: Int): Task? = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(SELECT_TASK_BY_ID)
        statement.setInt(1, id)
        val resultSet = statement.executeQuery()

        if (resultSet.next())
        {
            return@withContext Task(
                resultSet.getString("name"),
                resultSet.getString("description"),
                TaskCategory.fromCode(resultSet.getInt("category")),
                TaskStatus.fromCode(resultSet.getInt("status")))
        }

        null
    }

    suspend fun taskByName(name: String): Task? = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(SELECT_TASK_BY_NAME)
        statement.setString(1, name)
        val resultSet = statement.executeQuery()

        if (resultSet.next())
        {
            return@withContext Task(
                resultSet.getString("name"),
                resultSet.getString("description"),
                TaskCategory.fromCode(resultSet.getInt("category")),
                TaskStatus.fromCode(resultSet.getInt("status")))
        }

        null
    }

    suspend fun update(updatedTask: Task, oldTaskName: String) = withContext(Dispatchers.IO)
    {
        val findStm = connection.prepareStatement("SELECT ID FROM tasks WHERE name = ?")
        findStm.setString(1, oldTaskName)
        val find = findStm.executeQuery()

        if(!find.next()) throw Exception("Unable to retrieve task")

        val statement = connection.prepareStatement(UPDATE_TASK)
        statement.setString(1, updatedTask.name)
        statement.setInt(2, updatedTask.category.code)
        statement.setInt(3, updatedTask.status.code)
        statement.setString(4, updatedTask.description)
        statement.setString(5, oldTaskName)
        statement.executeUpdate()
    }

    suspend fun create(task: Task): Int = withContext(Dispatchers.IO)
    {
        val statement = connection.prepareStatement(INSERT_TASK, Statement.RETURN_GENERATED_KEYS)
        statement.setString(1, task.name)
        statement.setInt(2, task.category.code)
        statement.setInt(3, task.status.code)
        statement.setString(4, task.description)
        statement.executeUpdate()

        val generatedKeys = statement.generatedKeys
        if (generatedKeys.next()) return@withContext generatedKeys.getInt(1)

        return@withContext -1
    }

    suspend fun delete(name: String): Unit = withContext(Dispatchers.IO)
    {
        val findStm = connection.prepareStatement("SELECT ID FROM tasks WHERE name = ?")
        findStm.setString(1, name)
        val find = findStm.executeQuery()

        if(!find.next()) throw Exception("Given task doesn't exist already")

        val statement = connection.prepareStatement(DELETE_TASK)
        statement.setString(1, name)
        statement.executeUpdate()
    }

}