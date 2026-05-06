package homeaq.dothattask.data.DBSchema

import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import java.sql.Connection

sealed class UserGroupsSchema {
    companion object {
        // user_username is a denormalised display hint — no FK on it. The
        // real foreign key is user_email, which always points at a row
        // whose users.email is unique by index. See GroupsSchema for the
        // rationale about not relying on a UNIQUE constraint on username.
        const val CREATE_TABLE_USER_GROUPS_H2 =
            "CREATE TABLE IF NOT EXISTS USER_GROUPS (" +
                    "user_username VARCHAR(150) NOT NULL REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE," +
                    "user_email VARCHAR(150) NOT NULL REFERENCES users(email) ON DELETE CASCADE," +
                    "group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE," +
                    "role INTEGER NOT NULL DEFAULT 1," +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (user_email, group_id))"

        const val CREATE_TABLE_USER_GROUPS_PG =
            "CREATE TABLE IF NOT EXISTS USER_GROUPS (" +
                    "user_username CITEXT NOT NULL REFERENCES users(username) ON DELETE CASCADE ON UPDATE CASCADE," +
                    "user_email CITEXT NOT NULL REFERENCES users(email) ON DELETE CASCADE," +
                    "group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE," +
                    "role INTEGER NOT NULL DEFAULT 1," +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "PRIMARY KEY (user_email, group_id))"

        // Idempotent migrations for tables that already exist with the
        // legacy `user_username` PK. We add the new column nullable so the
        // ALTER doesn't fail on rows that haven't been backfilled yet —
        // the application has very few rows in non-prod and the operator
        // backfills emails manually before relying on email-keyed lookups.
        const val ALTER_USER_GROUPS_ADD_EMAIL_H2 =
            "ALTER TABLE user_groups " +
                    "ADD COLUMN IF NOT EXISTS user_email VARCHAR(150) REFERENCES users(email) ON DELETE CASCADE"

        const val ALTER_USER_GROUPS_ADD_EMAIL_PG =
            "ALTER TABLE user_groups " +
                    "ADD COLUMN IF NOT EXISTS user_email CITEXT REFERENCES users(email) ON DELETE CASCADE"
    }
}

class UserGroupsTableFactoryH2 : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(UserGroupsSchema.CREATE_TABLE_USER_GROUPS_H2)
        connection.createStatement().executeUpdate(UserGroupsSchema.ALTER_USER_GROUPS_ADD_EMAIL_H2)
    }
}

class UserGroupsTableFactoryPostgres : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(UserGroupsSchema.CREATE_TABLE_USER_GROUPS_PG)
        connection.createStatement().executeUpdate(UserGroupsSchema.ALTER_USER_GROUPS_ADD_EMAIL_PG)
    }
}

class UserGroupsTableSeedH2 : ITableSeed {
    override fun seed(connection: Connection) {
        val lookup = connection.prepareStatement("SELECT id FROM groups WHERE name = ?")
        lookup.setString(1, GroupsTableSeedH2.DEMO_GROUP_NAME)
        val rs = lookup.executeQuery()
        if (!rs.next()) return
        val groupId = rs.getInt("id")

        // Populate both keys so any legacy code path that still joins on
        // user_username keeps returning rows during the transition.
        val insert = connection.prepareStatement(
            "MERGE INTO user_groups (user_username, user_email, group_id, role) " +
                    "KEY(user_email, group_id) VALUES (?, ?, ?, ?)"
        )
        val demoUsers = UserTableSeedH2.demoUsers()
        demoUsers.forEachIndexed { idx, user ->
            insert.setString(1, user.username)
            insert.setString(2, user.email)
            insert.setInt(3, groupId)
            // Role 2 = ADMIN for the owner (first demo user), 1 = MEMBER for the rest.
            insert.setInt(4, if (idx == 0) 2 else 1)
            insert.executeUpdate()
        }
    }
}

class UserGroupsTableSeedPostgres : ITableSeed {
    override fun seed(connection: Connection) {}
}
