package homeaq.dothattask.data.repository

import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

class UserGroupRepository(
    private val connection: Connection,
    factory: ITableFactory,
    seeder: ITableSeed,
) {
    init {
        factory.createTable(connection)
        seeder.seed(connection)
    }

    suspend fun groupIdOfUser(username: String): Int? = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "SELECT group_id FROM user_groups WHERE user_username = ? ORDER BY joined_at ASC LIMIT 1"
        )
        stmt.setString(1, username)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.getInt("group_id") else null
    }
}
