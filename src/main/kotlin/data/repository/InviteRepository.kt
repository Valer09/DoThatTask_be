package homeaq.dothattask.data.repository

import homeaq.dothattask.Model.Invite
import homeaq.dothattask.Model.InviteStatus
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Statement
import javax.sql.DataSource

class InviteRepository(
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

    private val BASE_SELECT = "SELECT i.id, i.group_id, g.name AS group_name, g.color AS group_color, " +
            "i.inviter_username, i.invitee_username, i.status " +
            "FROM invites i JOIN groups g ON g.id = i.group_id"

    private fun ResultSet.toInvite(): Invite = Invite(
        id = getInt("id"),
        groupId = getInt("group_id"),
        groupName = getString("group_name"),
        groupColor = getString("group_color"),
        inviterUsername = getString("inviter_username"),
        inviteeUsername = getString("invitee_username"),
        status = InviteStatus.fromCode(getInt("status")),
    )

    suspend fun create(groupId: Int, inviterUsername: String, inviteeUsername: String): Int =
        dataSource.connection.use { connection ->
            withContext(Dispatchers.IO) {
                val stmt = connection.prepareStatement(
                    "INSERT INTO invites (group_id, inviter_username, invitee_username, status) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS,
                )
                stmt.setInt(1, groupId)
                stmt.setString(2, inviterUsername.lowercase())
                stmt.setString(3, inviteeUsername.lowercase())
                stmt.setInt(4, InviteStatus.PENDING.code)
                stmt.executeUpdate()
                val keys = stmt.generatedKeys
                if (keys.next()) keys.getInt(1) else -1
            }
        }

    suspend fun byId(id: Int): Invite? = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement("$BASE_SELECT WHERE i.id = ?")
            stmt.setInt(1, id)
            val rs = stmt.executeQuery()
            if (rs.next()) rs.toInvite() else null
        }
    }

    suspend fun incomingPendingFor(username: String): List<Invite> = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "$BASE_SELECT WHERE i.invitee_username = ? AND i.status = ? ORDER BY i.created_at DESC"
            )
            stmt.setString(1, username.lowercase())
            stmt.setInt(2, InviteStatus.PENDING.code)
            val rs = stmt.executeQuery()
            buildList { while (rs.next()) add(rs.toInvite()) }
        }
    }

    suspend fun existsPending(groupId: Int, inviteeUsername: String): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT 1 FROM invites WHERE group_id = ? AND invitee_username = ? AND status = ?"
            )
            stmt.setInt(1, groupId)
            stmt.setString(2, inviteeUsername.lowercase())
            stmt.setInt(3, InviteStatus.PENDING.code)
            stmt.executeQuery().next()
        }
    }

    suspend fun updateStatus(id: Int, status: InviteStatus): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "UPDATE invites SET status = ?, responded_at = CURRENT_TIMESTAMP WHERE id = ?"
            )
            stmt.setInt(1, status.code)
            stmt.setInt(2, id)
            stmt.executeUpdate()
        }
    }
}
