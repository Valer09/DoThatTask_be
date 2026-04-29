package homeaq.dothattask.data.DBSchema

import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import java.sql.Connection

/**
 * Many-to-many between groups and categories. Default categories are linked
 * to every group at group-creation time; users can add custom categories
 * (which auto-create a row in [CategoriesSchema] if not already present) or
 * unlink any category from a group. Unlinking a category never deletes it
 * from the global [CategoriesSchema] — other groups may still use it.
 */
sealed class GroupCategoriesSchema {
    companion object {
        const val CREATE_TABLE_GROUP_CATEGORIES_H2 =
            "CREATE TABLE IF NOT EXISTS GROUP_CATEGORIES (" +
                    "group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE," +
                    "category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE CASCADE," +
                    "PRIMARY KEY (group_id, category_id))"

        const val CREATE_TABLE_GROUP_CATEGORIES_PG =
            "CREATE TABLE IF NOT EXISTS GROUP_CATEGORIES (" +
                    "group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE," +
                    "category_id INTEGER NOT NULL REFERENCES categories(id) ON DELETE CASCADE," +
                    "PRIMARY KEY (group_id, category_id))"
    }
}

private fun backfillExistingGroups(connection: Connection) {
    // For every existing group that has no category links yet, attach the
    // default categories. Idempotent — safe to run on every boot.
    connection.createStatement().executeUpdate(
        """
        INSERT INTO group_categories (group_id, category_id)
        SELECT g.id, c.id
          FROM groups g
          CROSS JOIN categories c
         WHERE c.is_default = TRUE
           AND NOT EXISTS (
                SELECT 1 FROM group_categories gc
                 WHERE gc.group_id = g.id AND gc.category_id = c.id
           )
        """.trimIndent()
    )
}

class GroupCategoriesTableFactoryH2 : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(GroupCategoriesSchema.CREATE_TABLE_GROUP_CATEGORIES_H2)
        backfillExistingGroups(connection)
    }
}

class GroupCategoriesTableFactoryPostgres : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(GroupCategoriesSchema.CREATE_TABLE_GROUP_CATEGORIES_PG)
        backfillExistingGroups(connection)
    }
}

class GroupCategoriesTableSeedH2 : ITableSeed {
    override fun seed(connection: Connection) {}
}

class GroupCategoriesTableSeedPostgres : ITableSeed {
    override fun seed(connection: Connection) {}
}
