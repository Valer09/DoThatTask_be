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
                    "description VARCHAR(2056)," +
                    "user_username VARCHAR(150) NOT NULL REFERENCES users(username) ON DELETE CASCADE)"

        const val CREATE_TABLE_TASKS_PG =
            "CREATE TABLE IF NOT EXISTS TASKS (ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "name VARCHAR(150) NOT NULL UNIQUE," +
                    "category INTEGER NOT NULL," +
                    "status INTEGER NOT NULL," +
                    "description VARCHAR(2056)," +
                    "user_username CITEXT NOT NULL REFERENCES users(username) ON DELETE CASCADE)"

        const val ALTER_TASKS_ADD_GROUP_ID =
            "ALTER TABLE tasks ADD COLUMN IF NOT EXISTS group_id INTEGER"

        const val ALTER_TASKS_ADD_CREATOR_H2 =
            "ALTER TABLE tasks ADD COLUMN IF NOT EXISTS creator_username VARCHAR(150)"

        const val ALTER_TASKS_ADD_CREATOR_PG =
            "ALTER TABLE tasks ADD COLUMN IF NOT EXISTS creator_username CITEXT"

        const val LEGACY_GROUP_NAME = "Legacy"
    }
}

private fun ensureLegacyGroup(connection: Connection): Int? {
    val findOwner = connection.prepareStatement("SELECT username FROM users ORDER BY id ASC LIMIT 1")
    val ownerRs = findOwner.executeQuery()
    if (!ownerRs.next()) return null
    val ownerUsername = ownerRs.getString("username")

    val findGroup = connection.prepareStatement("SELECT id FROM groups WHERE name = ?")
    findGroup.setString(1, TasksSchema.LEGACY_GROUP_NAME)
    val groupRs = findGroup.executeQuery()
    if (groupRs.next()) return groupRs.getInt("id")

    val insert = connection.prepareStatement(
        "INSERT INTO groups (name, owner_username) VALUES (?, ?)",
        java.sql.Statement.RETURN_GENERATED_KEYS,
    )
    insert.setString(1, TasksSchema.LEGACY_GROUP_NAME)
    insert.setString(2, ownerUsername)
    insert.executeUpdate()
    val keys = insert.generatedKeys
    return if (keys.next()) keys.getInt(1) else null
}

private fun backfillTasks(connection: Connection) {
    val orphanCheck = connection.prepareStatement(
        "SELECT COUNT(*) FROM tasks WHERE group_id IS NULL OR creator_username IS NULL"
    )
    val rs = orphanCheck.executeQuery()
    if (!rs.next() || rs.getInt(1) == 0) return

    val legacyGroupId = ensureLegacyGroup(connection) ?: return

    connection.prepareStatement("UPDATE tasks SET group_id = ? WHERE group_id IS NULL").apply {
        setInt(1, legacyGroupId)
        executeUpdate()
    }
    connection.prepareStatement(
        "UPDATE tasks SET creator_username = user_username WHERE creator_username IS NULL"
    ).executeUpdate()
}

class TaskTableFactoryH2 : ITableFactory
{

    override fun createTable(connection: Connection)
    {
        connection.createStatement().executeUpdate(UsersSchema.CREATE_TABLE_USERS)
        connection.createStatement().executeUpdate(TasksSchema.CREATE_TABLE_TASKS)
        connection.createStatement().executeUpdate(TasksSchema.ALTER_TASKS_ADD_GROUP_ID)
        connection.createStatement().executeUpdate(TasksSchema.ALTER_TASKS_ADD_CREATOR_H2)
        backfillTasks(connection)
    }
}


class TaskTableFactoryPostgres : ITableFactory
{
    override fun createTable(connection: Connection)
    {
        connection.createStatement().executeUpdate(TasksSchema.CREATE_TABLE_TASKS_PG)
        connection.createStatement().executeUpdate(TasksSchema.ALTER_TASKS_ADD_GROUP_ID)
        connection.createStatement().executeUpdate(TasksSchema.ALTER_TASKS_ADD_CREATOR_PG)
        backfillTasks(connection)
    }
}


class TaskTableSeedH2() : ITableSeed
{
    override fun seed(connection: Connection)
    {
        UserTableSeedH2().seed(connection)
        val demoUsers = UserTableSeedH2.Companion.demoUsers()

        val demoGroupIdStmt = connection.prepareStatement("SELECT id FROM groups WHERE name = ?")
        demoGroupIdStmt.setString(1, GroupsTableSeedH2.DEMO_GROUP_NAME)
        val rs = demoGroupIdStmt.executeQuery()
        val demoGroupId = if (rs.next()) rs.getInt("id") else error("Demo group seed missing")

        val tasks = listOf(
            Task("Cleaning", "Clean the house", TaskCategory.Career, TaskStatus.TODO, demoUsers[0].username),
            Task("Gardening", "Mow the lawn", TaskCategory.Career,TaskStatus.TODO, demoUsers[1 % demoUsers.size].username),
            Task("Shopping", "Buy the groceries", TaskCategory.Career,TaskStatus.TODO, demoUsers[2 % demoUsers.size].username),
            Task("Painting", "Paint the fence", TaskCategory.Health,TaskStatus.COMPLETED, demoUsers[3 % demoUsers.size].username),
            Task("Cooking", "Cook the dinner", TaskCategory.Social,TaskStatus.TODO, demoUsers[0 % demoUsers.size].username),
            Task("Relaxing", "Take a walk", TaskCategory.Career,TaskStatus.TODO, demoUsers[1 % demoUsers.size].username),
            Task("Exercising", "Go to the gym", TaskCategory.Social,TaskStatus.TODO, demoUsers[2 % demoUsers.size].username),
            Task("Learning", "Read a book", TaskCategory.Career,TaskStatus.ACTIVE, demoUsers[3 % demoUsers.size].username),
            Task("Snoozing", "Go for a nap", TaskCategory.Career,TaskStatus.TODO, demoUsers[0 % demoUsers.size].username),
            Task("Socializing", "Go to a party", TaskCategory.Health,TaskStatus.TODO, demoUsers[1 % demoUsers.size].username),
        )

        val statement = connection.prepareStatement(
            "MERGE INTO tasks (name, category, status, description, user_username, group_id, creator_username) " +
                    "KEY(name) VALUES (?, ?, ?, ?, ?, ?, ?)"
        )

        tasks.forEachIndexed { idx, task ->
            // Creator is a different demo user than the owner, so the creator-only rule is testable.
            val creator = demoUsers[(idx + 1) % demoUsers.size].username
            statement.setString(1, task.name)
            statement.setInt(2, task.category.code)
            statement.setInt(3, task.status.code)
            statement.setString(4, task.description)
            statement.setString(5, task.ownership_username)
            statement.setInt(6, demoGroupId)
            statement.setString(7, creator)
            statement.executeUpdate()
        }
    }
}

class TaskTableSeedPostgres() : ITableSeed
{
    override fun seed(connection: Connection){}
}
