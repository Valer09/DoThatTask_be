package homeaq.dothattask.data.repository

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskCategory
import homeaq.dothattask.Model.TaskStatus
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import homeaq.dothattask.data.DBSchema.TasksSchema
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.Statement

class TaskRepository(private val connection: Connection, private val factory: ITableFactory, seeder: ITableSeed)
{
    private val SELECT_TASK_BY_ID = "SELECT * FROM tasks WHERE id = ?"
    private val SELECT_TASK_BY_NAME = "SELECT * FROM tasks WHERE name = ?"
    private val INSERT_TASK = "INSERT INTO tasks (name, category, status, description, user_username) VALUES (?, ?, ?, ?, ?)"
    private val UPDATE_TASK = "UPDATE tasks SET name = ?, category = ?, status = ?, description = ?, user_username = ? WHERE name = ?"
    private val DELETE_TASK = "DELETE FROM tasks WHERE name = ?"
    private val SELECT_TASK_BY_USER = "SELECT * FROM tasks WHERE user_username = ?"
    private val SELECT_COMPLETED_TASK_BY_USER = "SELECT * FROM tasks WHERE user_username = ? AND status = ?"

    init {
        factory.createTable(connection)
        seeder.seed(connection)
    }

    suspend fun allTasks(): List<Task> = withContext(Dispatchers.IO)
    {
        val statement = connection.prepareStatement("SELECT * FROM tasks")
        val resultSet = statement.executeQuery()
        val result = mutableListOf<Task>()
        while (resultSet.next()) {
            result.add(
                Task(
                    name = resultSet.getString("name"),
                    category = TaskCategory.fromCode(resultSet.getInt("category")),
                    status = TaskStatus.fromCode(resultSet.getInt("status")),
                    description = resultSet.getString("description"),
                    ownership_username = resultSet.getString("user_username")
                )
            )
        }

        return@withContext result
    }

    suspend fun taskById(id: Int): Task? = withContext(Dispatchers.IO)
    {
        val statement = connection.prepareStatement(SELECT_TASK_BY_ID)
        statement.setInt(1, id)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            return@withContext Task(
                resultSet.getString("name"),
                resultSet.getString("description"),
                TaskCategory.fromCode(resultSet.getInt("category")),
                TaskStatus.fromCode(resultSet.getInt("status")),
                resultSet.getString("user_username")
            )
        }

        null
    }

    suspend fun taskByName(name: String): Task? = withContext(Dispatchers.IO)
    {
        val statement = connection.prepareStatement(SELECT_TASK_BY_NAME)
        statement.setString(1, name)
        val resultSet = statement.executeQuery()

        if (resultSet.next()) {
            return@withContext Task(
                resultSet.getString("name"),
                resultSet.getString("description"),
                TaskCategory.Companion.fromCode(resultSet.getInt("category")),
                TaskStatus.Companion.fromCode(resultSet.getInt("status")),
                resultSet.getString("user_username")
            )
        }

        null
    }

    suspend fun tasksByUser(username: String): List<Task> = withContext(Dispatchers.IO)
    {
        val statement = connection.prepareStatement(SELECT_TASK_BY_USER)
        statement.setString(1, username)
        val resultSet = statement.executeQuery()
        val result = mutableListOf<Task>()

        while (resultSet.next()) {
            result.add(
                Task(
                    name = resultSet.getString("name"),
                    category = TaskCategory.fromCode(resultSet.getInt("category")),
                    status = TaskStatus.fromCode(resultSet.getInt("status")),
                    description = resultSet.getString("description"),
                    ownership_username = resultSet.getString("user_username")
                )
            )
        }
        return@withContext result
    }

    suspend fun completedTasks(username: String) :  List<Task> = withContext(Dispatchers.IO)
    {
        val userStatement = connection.prepareStatement("SELECT user FROM users WHERE username = ?")
        userStatement.setString(1, username)
        val userResultSet = userStatement.executeQuery()

        if (!userResultSet.next()) throw NotFoundException("User not found")

        val statement = connection.prepareStatement(SELECT_COMPLETED_TASK_BY_USER)
        statement.setString(1, username)
        statement.setInt(2, TaskStatus.COMPLETED.code)
        val resultSet = statement.executeQuery()
        val result = mutableListOf<Task>()

        while (resultSet.next()) {
            result.add(
                Task(
                    name = resultSet.getString("name"),
                    category = TaskCategory.fromCode(resultSet.getInt("category")),
                    status = TaskStatus.fromCode(resultSet.getInt("status")),
                    description = resultSet.getString("description"),
                    ownership_username = resultSet.getString("user_username")
                )
            )
        }
        return@withContext result
    }

    suspend fun update(updatedTask: Task, oldTaskName: String, ownershipUsername: String) = withContext(Dispatchers.IO)
    {
        val findStm = connection.prepareStatement("SELECT ID FROM tasks WHERE name = ?")
        findStm.setString(1, oldTaskName)
        val find = findStm.executeQuery()

        if (!find.next()) throw Exception("Unable to retrieve task")

        val statement = connection.prepareStatement(UPDATE_TASK)
        statement.setString(1, updatedTask.name)
        statement.setInt(2, updatedTask.category.code)
        statement.setInt(3, updatedTask.status.code)
        statement.setString(4, updatedTask.description)
        statement.setString(5, ownershipUsername)
        statement.setString(6, oldTaskName)
        statement.executeUpdate()
    }

    suspend fun create(task: Task, ownershipUsername: String): Int = withContext(Dispatchers.IO)
    {
        val statement = connection.prepareStatement(INSERT_TASK, Statement.RETURN_GENERATED_KEYS)
        statement.setString(1, task.name)
        statement.setInt(2, task.category.code)
        statement.setInt(3, task.status.code)
        statement.setString(4, task.description)
        statement.setString(5, ownershipUsername)
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

        if (!find.next()) throw Exception("Given task doesn't exist already")

        val statement = connection.prepareStatement(DELETE_TASK)
        statement.setString(1, name)
        statement.executeUpdate()
    }
}