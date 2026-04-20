package homeaq.dothattask.data.DBSchema

import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import java.sql.Connection

sealed class GroupsSchema {
    companion object {
        const val CREATE_TABLE_GROUPS_H2 =
            "CREATE TABLE IF NOT EXISTS GROUPS (" +
                    "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "name VARCHAR(150) NOT NULL UNIQUE," +
                    "owner_username VARCHAR(150) NOT NULL REFERENCES users(username) ON DELETE CASCADE," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"

        const val CREATE_TABLE_GROUPS_PG =
            "CREATE TABLE IF NOT EXISTS GROUPS (" +
                    "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "name VARCHAR(150) NOT NULL UNIQUE," +
                    "owner_username CITEXT NOT NULL REFERENCES users(username) ON DELETE CASCADE," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)"
    }
}

class GroupsTableFactoryH2 : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(GroupsSchema.CREATE_TABLE_GROUPS_H2)
    }
}

class GroupsTableFactoryPostgres : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(GroupsSchema.CREATE_TABLE_GROUPS_PG)
    }
}

class GroupsTableSeedH2 : ITableSeed {
    override fun seed(connection: Connection) {
        val owner = UserTableSeedH2.demoUsers().first().username
        val stmt = connection.prepareStatement(
            "MERGE INTO groups (name, owner_username) KEY(name) VALUES (?, ?)"
        )
        stmt.setString(1, DEMO_GROUP_NAME)
        stmt.setString(2, owner)
        stmt.executeUpdate()
    }

    companion object {
        const val DEMO_GROUP_NAME = "Demo Group"
    }
}

class GroupsTableSeedPostgres : ITableSeed {
    override fun seed(connection: Connection) {}
}
