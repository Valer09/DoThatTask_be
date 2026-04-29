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
    private val SELECT_BASE =
        "SELECT t.name, t.category, t.status, t.description, t.user_username, " +
                "t.group_id, g.name AS group_name, g.color AS group_color, t.creator_username, " +
                "t.created_at, c.name AS category_name, c.color AS category_color " +
                "FROM tasks t " +
                "JOIN groups g ON g.id = t.group_id " +
                "LEFT JOIN categories c ON c.id = t.category"

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
        category = TaskCategory(
            id = getInt("category"),
            name = getString("category_name").orEmpty(),
            color = getString("category_color").orEmpty(),
        ),
        status = TaskStatus.fromCode(getInt("status")),
        description = getString("description"),
        ownership_username = getString("user_username"),
        groupId = getInt("group_id"),
        groupName = getString("group_name"),
        groupColor = getString("group_color"),
        createdAt = getTimestamp("created_at")?.toInstant()?.toString().orEmpty(),
    )

    suspend fun allTasks(groupId: Int): List<Task> = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("$SELECT_BASE WHERE t.group_id = ?")
        stmt.setInt(1, groupId)
        val rs = stmt.executeQuery()
        buildList { while (rs.next()) add(rs.toTask()) }
    }

    /**
     * Search tasks within a group with optional filters. The caller's own
     * assigned tasks are always excluded so the "secret task" rule holds.
     */
    suspend fun searchTasks(
        groupId: Int,
        callerUsername: String,
        creator: String?,
        categoryId: Int?,
        assignee: String?,
    ): List<Task> = withContext(Dispatchers.IO) {
        val sql = StringBuilder("$SELECT_BASE WHERE t.group_id = ? AND LOWER(t.user_username) <> LOWER(?)")
        if (creator != null) sql.append(" AND LOWER(t.creator_username) = LOWER(?)")
        if (categoryId != null) sql.append(" AND t.category = ?")
        if (assignee != null) sql.append(" AND LOWER(t.user_username) = LOWER(?)")
        val stmt = connection.prepareStatement(sql.toString())
        var idx = 1
        stmt.setInt(idx++, groupId)
        stmt.setString(idx++, callerUsername)
        if (creator != null) stmt.setString(idx++, creator)
        if (categoryId != null) stmt.setInt(idx++, categoryId)
        if (assignee != null) stmt.setString(idx++, assignee)
        val rs = stmt.executeQuery()
        buildList { while (rs.next()) add(rs.toTask()) }
    }

    suspend fun taskByName(name: String, groupId: Int): Task? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("$SELECT_BASE WHERE t.name = ? AND t.group_id = ?")
        stmt.setString(1, name)
        stmt.setInt(2, groupId)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.toTask() else null
    }

    suspend fun tasksByUser(username: String, groupId: Int): List<Task> = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("$SELECT_BASE WHERE t.user_username = ? AND t.group_id = ?")
        stmt.setString(1, username.lowercase())
        stmt.setInt(2, groupId)
        val rs = stmt.executeQuery()
        buildList { while (rs.next()) add(rs.toTask()) }
    }

    suspend fun completedTasks(username: String, groupId: Int): List<Task> = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "$SELECT_BASE WHERE t.user_username = ? AND t.status = ? AND t.group_id = ?"
        )
        stmt.setString(1, username.lowercase())
        stmt.setInt(2, TaskStatus.COMPLETED.code)
        stmt.setInt(3, groupId)
        val rs = stmt.executeQuery()
        buildList { while (rs.next()) add(rs.toTask()) }
    }

    /** Aggregate completed tasks across every group the user belongs to. */
    suspend fun completedTasksAcrossGroups(username: String): List<Task> = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "$SELECT_BASE WHERE t.user_username = ? AND t.status = ?"
        )
        stmt.setString(1, username.lowercase())
        stmt.setInt(2, TaskStatus.COMPLETED.code)
        val rs = stmt.executeQuery()
        buildList { while (rs.next()) add(rs.toTask()) }
    }

    /** Active task assigned to the user across every group they belong to (at most one). */
    suspend fun activeTaskAcrossGroups(username: String): Task? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "$SELECT_BASE WHERE t.user_username = ? AND t.status = ?"
        )
        stmt.setString(1, username.lowercase())
        stmt.setInt(2, TaskStatus.ACTIVE.code)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.toTask() else null
    }

    /** Active task assigned to the user, scoped to a single group. */
    suspend fun activeTaskInGroup(username: String, groupId: Int): Task? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "$SELECT_BASE WHERE t.user_username = ? AND t.status = ? AND t.group_id = ?"
        )
        stmt.setString(1, username.lowercase())
        stmt.setInt(2, TaskStatus.ACTIVE.code)
        stmt.setInt(3, groupId)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.toTask() else null
    }

    /** Returns every TODO task assigned to the user across all groups, optionally filtered by category id. */
    suspend fun todoTasksAcrossGroups(username: String, categoryId: Int?): List<Task> = withContext(Dispatchers.IO) {
        val sql = StringBuilder("$SELECT_BASE WHERE t.user_username = ? AND t.status = ?")
        if (categoryId != null) sql.append(" AND t.category = ?")
        val stmt = connection.prepareStatement(sql.toString())
        stmt.setString(1, username.lowercase())
        stmt.setInt(2, TaskStatus.TODO.code)
        if (categoryId != null) stmt.setInt(3, categoryId)
        val rs = stmt.executeQuery()
        buildList { while (rs.next()) add(rs.toTask()) }
    }

    /** TODO tasks assigned to the user inside a specific group, optionally filtered by category id. */
    suspend fun todoTasksInGroup(username: String, categoryId: Int?, groupId: Int): List<Task> = withContext(Dispatchers.IO) {
        val sql = StringBuilder("$SELECT_BASE WHERE t.user_username = ? AND t.status = ? AND t.group_id = ?")
        if (categoryId != null) sql.append(" AND t.category = ?")
        val stmt = connection.prepareStatement(sql.toString())
        stmt.setString(1, username.lowercase())
        stmt.setInt(2, TaskStatus.TODO.code)
        stmt.setInt(3, groupId)
        if (categoryId != null) stmt.setInt(4, categoryId)
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
        stmt.setInt(2, task.category.id)
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
        stmt.setInt(2, updatedTask.category.id)
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
