package homeaq.dothattask.data.repository

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskCategory
import homeaq.dothattask.Model.TaskStatus
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

class TaskRepository(private val connection: Connection, private val factory: ITableFactory, seeder: ITableSeed)
{
    private val SELECT_ALL_IN_GROUP = "SELECT * FROM tasks WHERE group_id = ?"
    private val SELECT_TASK_BY_NAME_IN_GROUP = "SELECT * FROM tasks WHERE name = ? AND group_id = ?"
    private val SELECT_TASK_BY_USER_IN_GROUP = "SELECT * FROM tasks WHERE user_username = ? AND group_id = ?"
    private val SELECT_COMPLETED_TASK_BY_USER_IN_GROUP =
        "SELECT * FROM tasks WHERE user_username = ? AND status = ? AND group_id = ?"
    private val INSERT_TASK =
        "INSERT INTO tasks (name, category, status, description, user_username, group_id, creator_username) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)"
    private val UPDATE_TASK =
        "UPDATE tasks SET name = ?, category = ?, status = ?, description = ?, user_username = ? " +
                "WHERE name = ? AND group_id = ?"
    private val DELETE_TASK = "DELETE FROM tasks WHERE name = ? AND group_id = ?"
    private val UNASSIGN_TASK = "UPDATE tasks SET status = ? WHERE name = ? AND group_id = ?"
    private val SELECT_CREATOR = "SELECT creator_username FROM tasks WHERE name = ? AND group_id = ?"

    init {
        factory.createTable(connection)
        seeder.seed(connection)
    }

    private fun ResultSet.toTask(): Task = Task(
        name = getString("name"),
        category = TaskCategory.fromCode(getInt("category")),
        status = TaskStatus.fromCode(getInt("status")),
        description = getString("description"),
        ownership_username = getString("user_username"),
    )

    suspend fun allTasks(groupId: Int): List<Task> = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(SELECT_ALL_IN_GROUP)
        stmt.setInt(1, groupId)
        val rs = stmt.executeQuery()
        buildList { while (rs.next()) add(rs.toTask()) }
    }

    suspend fun taskByName(name: String, groupId: Int): Task? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(SELECT_TASK_BY_NAME_IN_GROUP)
        stmt.setString(1, name)
        stmt.setInt(2, groupId)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.toTask() else null
    }

    suspend fun tasksByUser(username: String, groupId: Int): List<Task> = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(SELECT_TASK_BY_USER_IN_GROUP)
        stmt.setString(1, username.lowercase())
        stmt.setInt(2, groupId)
        val rs = stmt.executeQuery()
        buildList { while (rs.next()) add(rs.toTask()) }
    }

    suspend fun completedTasks(username: String, groupId: Int): List<Task> = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(SELECT_COMPLETED_TASK_BY_USER_IN_GROUP)
        stmt.setString(1, username.lowercase())
        stmt.setInt(2, TaskStatus.COMPLETED.code)
        stmt.setInt(3, groupId)
        val rs = stmt.executeQuery()
        buildList { while (rs.next()) add(rs.toTask()) }
    }

    suspend fun creatorOf(name: String, groupId: Int): String? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(SELECT_CREATOR)
        stmt.setString(1, name)
        stmt.setInt(2, groupId)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.getString("creator_username") else null
    }

    suspend fun create(task: Task, creatorUsername: String, groupId: Int): Int = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(INSERT_TASK, Statement.RETURN_GENERATED_KEYS)
        stmt.setString(1, task.name)
        stmt.setInt(2, task.category.code)
        stmt.setInt(3, task.status.code)
        stmt.setString(4, task.description)
        stmt.setString(5, task.ownership_username.lowercase())
        stmt.setInt(6, groupId)
        stmt.setString(7, creatorUsername.lowercase())
        stmt.executeUpdate()
        val keys = stmt.generatedKeys
        if (keys.next()) keys.getInt(1) else -1
    }

    suspend fun update(updatedTask: Task, oldTaskName: String, groupId: Int): Unit = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(UPDATE_TASK)
        stmt.setString(1, updatedTask.name)
        stmt.setInt(2, updatedTask.category.code)
        stmt.setInt(3, updatedTask.status.code)
        stmt.setString(4, updatedTask.description)
        stmt.setString(5, updatedTask.ownership_username.lowercase())
        stmt.setString(6, oldTaskName)
        stmt.setInt(7, groupId)
        stmt.executeUpdate()
    }

    suspend fun delete(name: String, groupId: Int): Unit = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(DELETE_TASK)
        stmt.setString(1, name)
        stmt.setInt(2, groupId)
        stmt.executeUpdate()
    }

    suspend fun unAssign(taskName: String, groupId: Int): Unit = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(UNASSIGN_TASK)
        stmt.setInt(1, TaskStatus.TODO.code)
        stmt.setString(2, taskName)
        stmt.setInt(3, groupId)
        stmt.executeUpdate()
    }
}
