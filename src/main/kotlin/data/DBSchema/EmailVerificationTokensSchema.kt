package homeaq.dothattask.data.DBSchema

import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import java.sql.Connection
import java.sql.SQLException

sealed class EmailVerificationTokensSchema {
    companion object {
        const val CREATE_TABLE_H2 =
            "CREATE TABLE IF NOT EXISTS email_verification_tokens (" +
                    "id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "user_username VARCHAR(150) NOT NULL," +
                    "token_hash VARCHAR(64) NOT NULL UNIQUE," +
                    "expires_at TIMESTAMP NOT NULL," +
                    "used_at TIMESTAMP," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "CONSTRAINT fk_evt_user FOREIGN KEY (user_username) REFERENCES users(username) ON DELETE CASCADE)"

        const val CREATE_TABLE_PG =
            "CREATE TABLE IF NOT EXISTS email_verification_tokens (" +
                    "id INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                    "user_username CITEXT NOT NULL," +
                    "token_hash VARCHAR(64) NOT NULL UNIQUE," +
                    "expires_at TIMESTAMP NOT NULL," +
                    "used_at TIMESTAMP," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "CONSTRAINT fk_evt_user FOREIGN KEY (user_username) REFERENCES users(username) ON DELETE CASCADE)"

        const val CREATE_INDEX_USERNAME =
            "CREATE INDEX IF NOT EXISTS idx_evt_user ON email_verification_tokens(user_username)"
    }
}

class EmailVerificationTokensTableFactoryH2 : ITableFactory {
    override fun createTable(connection: Connection) {
        try {
            val wasAutoCommit = connection.autoCommit
            connection.autoCommit = true
            connection.createStatement().use {
                it.execute(EmailVerificationTokensSchema.CREATE_TABLE_H2)
                it.execute(EmailVerificationTokensSchema.CREATE_INDEX_USERNAME)
            }
            connection.autoCommit = wasAutoCommit
        } catch (e: SQLException) {
            println("Error creating email_verification_tokens (H2): ${e.message}")
        }
    }
}

class EmailVerificationTokensTableFactoryPostgres : ITableFactory {
    override fun createTable(connection: Connection) {
        try {
            val wasAutoCommit = connection.autoCommit
            connection.autoCommit = true
            connection.createStatement().use {
                it.execute(EmailVerificationTokensSchema.CREATE_TABLE_PG)
                it.execute(EmailVerificationTokensSchema.CREATE_INDEX_USERNAME)
            }
            connection.autoCommit = wasAutoCommit
        } catch (e: SQLException) {
            println("Error creating email_verification_tokens (Postgres): ${e.message}")
        }
    }
}

class EmailVerificationTokensTableSeedH2 : ITableSeed {
    override fun seed(connection: Connection) {}
}

class EmailVerificationTokensTableSeedPostgres : ITableSeed {
    override fun seed(connection: Connection) {}
}
