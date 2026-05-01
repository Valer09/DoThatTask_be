package homeaq.dothattask.data.repository

import homeaq.dothattask.Model.TaskCategory
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.ResultSet
import java.sql.Statement
import javax.sql.DataSource

class CategoryRepository(
    private val dataSource: DataSource,
    categoriesFactory: ITableFactory,
    categoriesSeeder: ITableSeed,
    groupCategoriesFactory: ITableFactory,
    groupCategoriesSeeder: ITableSeed,
) {
    init {
        dataSource.connection.use { conn ->
            categoriesFactory.createTable(conn)
            categoriesSeeder.seed(conn)
            groupCategoriesFactory.createTable(conn)
            groupCategoriesSeeder.seed(conn)
        }
    }

    private fun ResultSet.toCategory(): TaskCategory = TaskCategory(
        id = getInt("id"),
        name = getString("name"),
        color = getString("color"),
    )

    suspend fun byId(id: Int): TaskCategory? = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement("SELECT id, name, color FROM categories WHERE id = ?")
            stmt.setInt(1, id)
            val rs = stmt.executeQuery()
            if (rs.next()) rs.toCategory() else null
        }
    }

    suspend fun byNameInsensitive(name: String): TaskCategory? = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement(
                "SELECT id, name, color FROM categories WHERE LOWER(name) = LOWER(?)"
            )
            stmt.setString(1, name)
            val rs = stmt.executeQuery()
            if (rs.next()) rs.toCategory() else null
        }
    }

    suspend fun createCategory(name: String, color: String): TaskCategory = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO categories (name, color) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS,
            )
            stmt.setString(1, name)
            stmt.setString(2, color)
            stmt.executeUpdate()
            val keys = stmt.generatedKeys
            val id = if (keys.next()) keys.getInt(1) else error("Insert into categories returned no id")
            TaskCategory(id = id, name = name, color = color)
        }
    }

    suspend fun categoriesForGroup(groupId: Int): List<TaskCategory> = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement(
                """
                SELECT c.id, c.name, c.color
                  FROM categories c
                  JOIN group_categories gc ON gc.category_id = c.id
                 WHERE gc.group_id = ?
                 ORDER BY c.is_default DESC, LOWER(c.name) ASC
                """.trimIndent()
            )
            stmt.setInt(1, groupId)
            val rs = stmt.executeQuery()
            buildList { while (rs.next()) add(rs.toCategory()) }
        }
    }

    suspend fun isLinkedToGroup(groupId: Int, categoryId: Int): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement(
                "SELECT 1 FROM group_categories WHERE group_id = ? AND category_id = ?"
            )
            stmt.setInt(1, groupId)
            stmt.setInt(2, categoryId)
            stmt.executeQuery().next()
        }
    }

    suspend fun linkToGroup(groupId: Int, categoryId: Int): Unit = withContext(Dispatchers.IO) {
        if (isLinkedToGroup(groupId, categoryId)) return@withContext
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement(
                "INSERT INTO group_categories (group_id, category_id) VALUES (?, ?)"
            )
            stmt.setInt(1, groupId)
            stmt.setInt(2, categoryId)
            stmt.executeUpdate()
        }
    }

    suspend fun unlinkFromGroup(groupId: Int, categoryId: Int): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement(
                "DELETE FROM group_categories WHERE group_id = ? AND category_id = ?"
            )
            stmt.setInt(1, groupId)
            stmt.setInt(2, categoryId)
            stmt.executeUpdate() > 0
        }
    }

    suspend fun isDefault(categoryId: Int): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement("SELECT is_default FROM categories WHERE id = ?")
            stmt.setInt(1, categoryId)
            val rs = stmt.executeQuery()
            rs.next() && rs.getBoolean("is_default")
        }
    }

    suspend fun defaultCategoryIds(): List<Int> = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement("SELECT id FROM categories WHERE is_default = TRUE ORDER BY id")
            val rs = stmt.executeQuery()
            buildList { while (rs.next()) add(rs.getInt("id")) }
        }
    }

    suspend fun tasksInGroupWithCategory(groupId: Int, categoryId: Int): Int = withContext(Dispatchers.IO) {
        dataSource.connection.use { conn ->
            val stmt = conn.prepareStatement(
                "SELECT COUNT(*) FROM tasks WHERE group_id = ? AND category = ?"
            )
            stmt.setInt(1, groupId)
            stmt.setInt(2, categoryId)
            val rs = stmt.executeQuery()
            if (rs.next()) rs.getInt(1) else 0
        }
    }
}
