package homeaq.dothattask.data.repository

import homeaq.dothattask.Model.Group
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import javax.sql.DataSource

class GroupRepository(
    private val dataSource: DataSource,
    factory: ITableFactory,
    seeder: ITableSeed,
) {
    init {
        dataSource.connection.use { connection ->
            factory.createTable(connection)
            seeder.seed(connection)
        }
    }

    private fun ResultSet.toGroup(): Group = Group(
        id = getInt("id"),
        name = getString("name"),
        ownerUsername = getString("owner_username"),
        color = getString("color"),
    )

    suspend fun create(name: String, ownerUsername: String, color: String): Int = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "INSERT INTO groups (name, owner_username, color) VALUES (?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS,
            )
            stmt.setString(1, name)
            stmt.setString(2, ownerUsername.lowercase())
            stmt.setString(3, color)
            stmt.executeUpdate()
            val keys = stmt.generatedKeys
            if (keys.next()) keys.getInt(1) else -1
        }
    }

    suspend fun byId(id: Int): Group? = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement("SELECT id, name, owner_username, color FROM groups WHERE id = ?")
            stmt.setInt(1, id)
            val rs = stmt.executeQuery()
            if (rs.next()) rs.toGroup() else null
        }
    }

    suspend fun byName(name: String): Group? = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement("SELECT id, name, owner_username, color FROM groups WHERE name = ?")
            stmt.setString(1, name)
            val rs = stmt.executeQuery()
            if (rs.next()) rs.toGroup() else null
        }
    }

    suspend fun delete(id: Int): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement("DELETE FROM groups WHERE id = ?")
            stmt.setInt(1, id)
            stmt.executeUpdate()
        }
    }

}
