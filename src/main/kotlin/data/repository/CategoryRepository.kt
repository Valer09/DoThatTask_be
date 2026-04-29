package homeaq.dothattask.data.repository

import homeaq.dothattask.Model.TaskCategory
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement

/**
 * Two-table repository:
 *   * `categories` — global registry of every category ever created.
 *   * `group_categories` — many-to-many between groups and categories.
 *
 * Naming/uniqueness rules:
 *   * Names are stored normalized to "First-letter capital, rest lowercase"
 *     ([TaskCategory.normalizeName]).
 *   * Existence checks are case-insensitive (LOWER(name) = LOWER(?)).
 */
class CategoryRepository(
    private val connection: Connection,
    categoriesFactory: ITableFactory,
    categoriesSeeder: ITableSeed,
    groupCategoriesFactory: ITableFactory,
    groupCategoriesSeeder: ITableSeed,
) {
    init {
        categoriesFactory.createTable(connection)
        categoriesSeeder.seed(connection)
        groupCategoriesFactory.createTable(connection)
        groupCategoriesSeeder.seed(connection)
    }

    private fun ResultSet.toCategory(): TaskCategory = TaskCategory(
        id = getInt("id"),
        name = getString("name"),
        color = getString("color"),
    )

    suspend fun byId(id: Int): TaskCategory? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("SELECT id, name, color FROM categories WHERE id = ?")
        stmt.setInt(1, id)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.toCategory() else null
    }

    /** Case-insensitive lookup by name. */
    suspend fun byNameInsensitive(name: String): TaskCategory? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "SELECT id, name, color FROM categories WHERE LOWER(name) = LOWER(?)"
        )
        stmt.setString(1, name)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.toCategory() else null
    }

    suspend fun createCategory(name: String, color: String): TaskCategory = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
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

    /** Categories visible to a group: defaults + customs explicitly linked. */
    suspend fun categoriesForGroup(groupId: Int): List<TaskCategory> = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
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

    suspend fun isLinkedToGroup(groupId: Int, categoryId: Int): Boolean = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "SELECT 1 FROM group_categories WHERE group_id = ? AND category_id = ?"
        )
        stmt.setInt(1, groupId)
        stmt.setInt(2, categoryId)
        stmt.executeQuery().next()
    }

    suspend fun linkToGroup(groupId: Int, categoryId: Int): Unit = withContext(Dispatchers.IO) {
        // Idempotent: if already linked, do nothing.
        if (isLinkedToGroup(groupId, categoryId)) return@withContext
        val stmt = connection.prepareStatement(
            "INSERT INTO group_categories (group_id, category_id) VALUES (?, ?)"
        )
        stmt.setInt(1, groupId)
        stmt.setInt(2, categoryId)
        stmt.executeUpdate()
    }

    /** Unlink — never deletes the category itself, only the group↔category row. */
    suspend fun unlinkFromGroup(groupId: Int, categoryId: Int): Boolean = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "DELETE FROM group_categories WHERE group_id = ? AND category_id = ?"
        )
        stmt.setInt(1, groupId)
        stmt.setInt(2, categoryId)
        stmt.executeUpdate() > 0
    }

    suspend fun isDefault(categoryId: Int): Boolean = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("SELECT is_default FROM categories WHERE id = ?")
        stmt.setInt(1, categoryId)
        val rs = stmt.executeQuery()
        rs.next() && rs.getBoolean("is_default")
    }

    /** Default category ids — used to auto-link defaults to a freshly created group. */
    suspend fun defaultCategoryIds(): List<Int> = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement("SELECT id FROM categories WHERE is_default = TRUE ORDER BY id")
        val rs = stmt.executeQuery()
        buildList { while (rs.next()) add(rs.getInt("id")) }
    }

    /** Count tasks in a given group still using a given category (used to block unlinking). */
    suspend fun tasksInGroupWithCategory(groupId: Int, categoryId: Int): Int = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "SELECT COUNT(*) FROM tasks WHERE group_id = ? AND category = ?"
        )
        stmt.setInt(1, groupId)
        stmt.setInt(2, categoryId)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.getInt(1) else 0
    }
}
