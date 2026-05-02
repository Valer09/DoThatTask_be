package homeaq.dothattask.data.DBSchema

import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import java.sql.Connection

sealed class RefreshTokensSchema {
    companion object {
        const val CREATE_TABLE_REFRESH_TOKENS_H2 =
            "CREATE TABLE IF NOT EXISTS REFRESH_TOKENS (" +
                    "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "user_username VARCHAR(150) NOT NULL REFERENCES users(username) ON DELETE CASCADE," +
                    "token_hash VARCHAR(255) NOT NULL UNIQUE," +
                    "issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "expires_at TIMESTAMP NOT NULL," +
                    "revoked_at TIMESTAMP NULL)"

        const val CREATE_TABLE_REFRESH_TOKENS_PG =
            "CREATE TABLE IF NOT EXISTS REFRESH_TOKENS (" +
                    "ID INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "user_username CITEXT NOT NULL REFERENCES users(username) ON DELETE CASCADE," +
                    "token_hash VARCHAR(255) NOT NULL UNIQUE," +
                    "issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "expires_at TIMESTAMP NOT NULL," +
                    "revoked_at TIMESTAMP NULL)"
    }
}

class RefreshTokensTableFactoryH2 : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(RefreshTokensSchema.CREATE_TABLE_REFRESH_TOKENS_H2)
    }
}

class RefreshTokensTableFactoryPostgres : ITableFactory {
    override fun createTable(connection: Connection) {
        connection.createStatement().executeUpdate(RefreshTokensSchema.CREATE_TABLE_REFRESH_TOKENS_PG)
    }
}

class RefreshTokensTableSeedH2 : ITableSeed {
    override fun seed(connection: Connection) {}
}

class RefreshTokensTableSeedPostgres : ITableSeed {
    override fun seed(connection: Connection) {}
}

class H2FcmTokenDialectQueries : IFcmTokenDialectQueries
{
    override fun registerFcmQuery(): String {
        return """
                MERGE INTO fcm_tokens (user_username, token, platform)
                KEY (token)
                VALUES (?, ?, ?) """
    }
}

class PGFcmTokenDialectQueries : IFcmTokenDialectQueries
{
    override fun registerFcmQuery(): String {
        return """
                INSERT INTO fcm_tokens (user_username, token, platform)
                VALUES (?, ?, ?)
                ON CONFLICT (token) DO UPDATE SET
                    user_username = EXCLUDED.user_username,
                    platform      = EXCLUDED.platform """
    }
}
