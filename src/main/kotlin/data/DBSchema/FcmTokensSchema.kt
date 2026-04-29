package homeaq.dothattask.data.DBSchema

import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import java.sql.Connection

sealed class FcmTokensSchema {
    companion object {
        const val CREATE_TABLE_FCM_TOKENS_H2 =
            "CREATE TABLE IF NOT EXISTS FCM_TOKENS (" +
                    "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "user_username VARCHAR(150) NOT NULL REFERENCES users(username) ON DELETE CASCADE," +
                    "token VARCHAR(2048) NOT NULL UNIQUE," +
                    "platform VARCHAR(32) NOT NULL DEFAULT 'android'," +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)"

        const val CREATE_TABLE_FCM_TOKENS_PG =
            "CREATE TABLE IF NOT EXISTS FCM_TOKENS (" +
                    "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "user_username CITEXT NOT NULL REFERENCES users(username) ON DELETE CASCADE," +
                    "token VARCHAR(2048) NOT NULL UNIQUE," +
                    "platform VARCHAR(32) NOT NULL DEFAULT 'android'," +
                    "created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW())"
    }
}

class FcmTokensTableFactoryH2 : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(FcmTokensSchema.CREATE_TABLE_FCM_TOKENS_H2)
    }
}

class FcmTokensTableFactoryPostgres : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(FcmTokensSchema.CREATE_TABLE_FCM_TOKENS_PG)
    }
}

class FcmTokensTableSeedH2 : ITableSeed {
    override fun seed(connection: Connection) {}
}

class FcmTokensTableSeedPostgres : ITableSeed {
    override fun seed(connection: Connection) {}
}
