package homeaq.dothattask.data.DBSchema

import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import java.sql.Connection

sealed class UserGroupsSchema {
    companion object {
        const val CREATE_TABLE_USER_GROUPS_H2 =
            "CREATE TABLE IF NOT EXISTS USER_GROUPS (" +
                    "user_username VARCHAR(150) NOT NULL REFERENCES users(username) ON DELETE CASCADE," +
                    "group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE," +
                    "role INTEGER NOT NULL DEFAULT 1," +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (user_username, group_id))"

        const val CREATE_TABLE_USER_GROUPS_PG =
            "CREATE TABLE IF NOT EXISTS USER_GROUPS (" +
                    "user_username CITEXT NOT NULL REFERENCES users(username) ON DELETE CASCADE," +
                    "group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE," +
                    "role INTEGER NOT NULL DEFAULT 1," +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (user_username, group_id))"
    }
}

class UserGroupsTableFactoryH2 : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(UserGroupsSchema.CREATE_TABLE_USER_GROUPS_H2)
    }
}

class UserGroupsTableFactoryPostgres : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(UserGroupsSchema.CREATE_TABLE_USER_GROUPS_PG)
    }
}

class UserGroupsTableSeedH2 : ITableSeed {
    override fun seed(connection: Connection) {}
}

class UserGroupsTableSeedPostgres : ITableSeed {
    override fun seed(connection: Connection) {}
}
