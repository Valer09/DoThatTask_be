package homeaq.dothattask.data.DBSchema

import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import java.sql.Connection

sealed class InvitesSchema {
    companion object {
        const val CREATE_TABLE_INVITES_H2 =
            "CREATE TABLE IF NOT EXISTS INVITES (" +
                    "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE," +
                    "inviter_username VARCHAR(150) NOT NULL REFERENCES users(username) ON DELETE CASCADE," +
                    "invitee_username VARCHAR(150) NOT NULL REFERENCES users(username) ON DELETE CASCADE," +
                    "status INTEGER NOT NULL DEFAULT 1," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "responded_at TIMESTAMP NULL)"

        const val CREATE_INDEX_INVITES_INVITEE_STATUS =
            "CREATE INDEX IF NOT EXISTS idx_invites_invitee_status ON invites(invitee_username, status)"

        const val CREATE_TABLE_INVITES_PG =
            "CREATE TABLE IF NOT EXISTS INVITES (" +
                    "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "group_id INTEGER NOT NULL REFERENCES groups(id) ON DELETE CASCADE," +
                    "inviter_username CITEXT NOT NULL REFERENCES users(username) ON DELETE CASCADE," +
                    "invitee_username CITEXT NOT NULL REFERENCES users(username) ON DELETE CASCADE," +
                    "status INTEGER NOT NULL DEFAULT 1," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "responded_at TIMESTAMP NULL)"
    }
}

class InvitesTableFactoryH2 : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(InvitesSchema.CREATE_TABLE_INVITES_H2)
        connection.createStatement().executeUpdate(InvitesSchema.CREATE_INDEX_INVITES_INVITEE_STATUS)
    }
}

class InvitesTableFactoryPostgres : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(InvitesSchema.CREATE_TABLE_INVITES_PG)
        connection.createStatement().executeUpdate(InvitesSchema.CREATE_INDEX_INVITES_INVITEE_STATUS)
    }
}

class InvitesTableSeedH2 : ITableSeed {
    override fun seed(connection: Connection) {}
}

class InvitesTableSeedPostgres : ITableSeed {
    override fun seed(connection: Connection) {}
}
