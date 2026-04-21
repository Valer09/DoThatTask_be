package homeaq.dothattask.data.repository

import homeaq.dothattask.Model.GroupRole
import homeaq.dothattask.Model.UserGroup
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
        stmt.setString(1, username.lowercase())
        val rs = stmt.executeQuery()
        if (rs.next()) rs.getInt("group_id") else null
    }

    suspend fun addMember(
        username: String,
        groupId: Int,
        role: GroupRole = GroupRole.MEMBER,
    ): Unit = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "INSERT INTO user_groups (user_username, group_id, role) VALUES (?, ?, ?)"
        )
        stmt.setString(1, username.lowercase())
        stmt.setInt(2, groupId)
        stmt.setInt(3, role.code)
        stmt.executeUpdate()
    }

    suspend fun removeMember(username: String, groupId: Int): Unit = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "DELETE FROM user_groups WHERE user_username = ? AND group_id = ?"
        )
        stmt.setString(1, username.lowercase())
        stmt.setInt(2, groupId)
        stmt.executeUpdate()
    }

    suspend fun isMember(username: String, groupId: Int): Boolean = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "SELECT 1 FROM user_groups WHERE user_username = ? AND group_id = ?"
        )
        stmt.setString(1, username.lowercase())
        stmt.setInt(2, groupId)
        stmt.executeQuery().next()
    }

    suspend fun membersOfGroup(groupId: Int): List<UserGroup> = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "SELECT user_username, group_id, role FROM user_groups WHERE group_id = ? ORDER BY joined_at ASC"
        )
        stmt.setInt(1, groupId)
        val rs = stmt.executeQuery()
        buildList {
            while (rs.next()) {
                add(
                    UserGroup(
                        userUsername = rs.getString("user_username"),
                        groupId = rs.getInt("group_id"),
                        role = GroupRole.fromCode(rs.getInt("role")),
                    ),
                )
            }
        }
    }

    suspend fun countMembers(groupId: Int): Int = withContext(Dispatchers.IO) {
        val stmt = connection.prepareStatement(
            "SELECT COUNT(*) AS c FROM user_groups WHERE group_id = ?"
        )
        stmt.setInt(1, groupId)
        val rs = stmt.executeQuery()
        if (rs.next()) rs.getInt("c") else 0
    }
}
