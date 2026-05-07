package homeaq.dothattask.data.repository

import homeaq.dothattask.Model.Group
import homeaq.dothattask.Model.GroupRole
import homeaq.dothattask.Model.UserGroup
import homeaq.dothattask.data.TableCreationAndSeed.ITableFactory
import homeaq.dothattask.data.TableCreationAndSeed.ITableSeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.sql.DataSource

class UserGroupRepository(
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

    suspend fun groupsOfUser(email: String): List<Group> = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT g.id, g.name, g.owner_email, g.color " +
                        "FROM groups g " +
                        "JOIN user_groups ug ON ug.group_id = g.id " +
                        "WHERE ug.user_email = ? " +
                        "ORDER BY ug.joined_at ASC"
            )
            stmt.setString(1, email.lowercase())
            val rs = stmt.executeQuery()
            buildList {
                while (rs.next()) {
                    add(
                        Group(
                            id = rs.getInt("id"),
                            name = rs.getString("name"),
                            ownerEmail = rs.getString("owner_email"),
                            color = rs.getString("color"),
                        ),
                    )
                }
            }
        }
    }

    suspend fun addMember(
        email: String,
        groupId: Int,
        role: GroupRole = GroupRole.MEMBER,
        username: String,
    ): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "INSERT INTO user_groups (user_username, user_email, group_id, role) VALUES (?, ?, ?, ?)"
            )
            stmt.setString(1, username.lowercase())
            stmt.setString(2, email.lowercase())
            stmt.setInt(3, groupId)
            stmt.setInt(4, role.code)
            stmt.executeUpdate()
        }
    }

    suspend fun removeMember(email: String, groupId: Int): Unit = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "DELETE FROM user_groups WHERE user_email = ? AND group_id = ?"
            )
            stmt.setString(1, email.lowercase())
            stmt.setInt(2, groupId)
            stmt.executeUpdate()
        }
    }

    suspend fun isMember(email: String, groupId: Int): Boolean = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT 1 FROM user_groups WHERE user_email = ? AND group_id = ?"
            )
            stmt.setString(1, email.lowercase())
            stmt.setInt(2, groupId)
            stmt.executeQuery().next()
        }
    }

    suspend fun membersOfGroup(groupId: Int): List<UserGroup> = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT user_username, user_email, group_id, role " +
                        "FROM user_groups WHERE group_id = ? ORDER BY joined_at ASC"
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
                            email = rs.getString("user_email"),
                        ),
                    )
                }
            }
        }
    }

    suspend fun countMembers(groupId: Int): Int = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement(
                "SELECT COUNT(*) AS c FROM user_groups WHERE group_id = ?"
            )
            stmt.setInt(1, groupId)
            val rs = stmt.executeQuery()
            if (rs.next()) rs.getInt("c") else 0
        }
    }

    suspend fun countAllGroups(): Int = withContext(Dispatchers.IO) {
        dataSource.connection.use { connection ->
            val stmt = connection.prepareStatement("SELECT COUNT(*) AS c FROM groups")
            val rs = stmt.executeQuery()
            if (rs.next()) rs.getInt("c") else 0
        }
    }
}
