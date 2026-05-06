package homeaq.dothattask.data.DBSchema

import homeaq.dothattask.data.DBSchema.GroupsSchema.Companion.ALTER_GROUPS_ADD_EMAIL_COLUMNS_PG
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import java.sql.Connection

sealed class GroupsSchema {
    companion object {
        const val DEFAULT_COLOR = "#7E57C2"

        const val CREATE_TABLE_GROUPS_H2 =
            "CREATE TABLE IF NOT EXISTS GROUPS (" +
                    "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "name VARCHAR(150) NOT NULL UNIQUE," +
                    "owner_username VARCHAR(150) REFERENCES users(username)," +
                    "owner_email VARCHAR(150) NOT NULL REFERENCES users(email) ON DELETE CASCADE," +
                    "color VARCHAR(9) NOT NULL DEFAULT '$DEFAULT_COLOR'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"

        const val CREATE_TABLE_GROUPS_PG =
            "CREATE TABLE IF NOT EXISTS GROUPS (" +
                    "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "name VARCHAR(150) NOT NULL UNIQUE," +
                    "owner_username CITEXT REFERENCES users(username)," +
                    "owner_email CITEXT NOT NULL REFERENCES users(email) ON DELETE CASCADE," +
                    "color VARCHAR(9) NOT NULL DEFAULT '$DEFAULT_COLOR'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"

        const val ALTER_GROUPS_ADD_COLOR =
            "ALTER TABLE groups ADD COLUMN IF NOT EXISTS color VARCHAR(9) NOT NULL DEFAULT '$DEFAULT_COLOR'"

        const val ALTER_GROUPS_ADD_EMAIL_COLUMNS_PG =
            "ALTER TABLE groups " +
                    "ADD COLUMN IF NOT EXISTS owner_email CITEXT REFERENCES users(email) ON DELETE CASCADE"
    }
}

class GroupsTableFactoryH2 : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(GroupsSchema.CREATE_TABLE_GROUPS_H2)
        connection.createStatement().executeUpdate(GroupsSchema.ALTER_GROUPS_ADD_COLOR)
        connection.createStatement().executeUpdate(GroupsSchema.ALTER_GROUPS_ADD_EMAIL_COLUMNS_PG)
    }
}

class GroupsTableFactoryPostgres : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(GroupsSchema.CREATE_TABLE_GROUPS_PG)
        connection.createStatement().executeUpdate(GroupsSchema.ALTER_GROUPS_ADD_COLOR)
        connection.createStatement().executeUpdate(GroupsSchema.ALTER_GROUPS_ADD_EMAIL_COLUMNS_PG)
    }
}

class GroupsTableSeedH2 : ITableSeed {
    override fun seed(connection: Connection) {
        var owner = UserTableSeedH2.demoUsers().first().username
        var stmt = connection.prepareStatement(
            "MERGE INTO groups (name, owner_username, color) KEY(name) VALUES (?, ?, ?)"
        )
        stmt.setString(1, DEMO_GROUP_NAME)
        stmt.setString(2, owner)
        stmt.setString(3, GroupsSchema.DEFAULT_COLOR)
        stmt.executeUpdate()

        owner = UserTableSeedH2.demoUsersAlt().first().username
        stmt = connection.prepareStatement(
            "MERGE INTO groups (name, owner_username, color) KEY(name) VALUES (?, ?, ?)"
        )
        stmt.setString(1, DEMO_GROUP_NAME_ALT)
        stmt.setString(2, owner)
        stmt.setString(3, GroupsSchema.DEFAULT_COLOR)
        stmt.executeUpdate()
    }

    companion object {
        const val DEMO_GROUP_NAME = "Demo Group"
        const val DEMO_GROUP_NAME_ALT = "Demo Group"
    }
}

class GroupsTableSeedPostgres : ITableSeed {
    override fun seed(connection: Connection) {}
}
