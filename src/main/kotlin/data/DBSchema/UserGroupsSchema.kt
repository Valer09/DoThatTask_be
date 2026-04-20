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
    override fun seed(connection: Connection) {
        val lookup = connection.prepareStatement("SELECT id FROM groups WHERE name = ?")
        lookup.setString(1, GroupsTableSeedH2.DEMO_GROUP_NAME)
        val rs = lookup.executeQuery()
        if (!rs.next()) return
        val groupId = rs.getInt("id")

        val insert = connection.prepareStatement(
            "MERGE INTO user_groups (user_username, group_id, role) KEY(user_username, group_id) VALUES (?, ?, ?)"
        )
        val demoUsers = UserTableSeedH2.demoUsers()
        demoUsers.forEachIndexed { idx, user ->
            insert.setString(1, user.username)
            insert.setInt(2, groupId)
            // Role 2 = ADMIN for the owner (first demo user), 1 = MEMBER for the rest.
            insert.setInt(3, if (idx == 0) 2 else 1)
            insert.executeUpdate()
        }
    }
}

class UserGroupsTableSeedPostgres : ITableSeed {
    override fun seed(connection: Connection) {}
}
