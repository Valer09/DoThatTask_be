package homeaq.dothattask.data.service

import homeaq.dothattask.Model.Group
import homeaq.dothattask.Model.GroupInfo
import homeaq.dothattask.Model.GroupMember
import homeaq.dothattask.Model.GroupRole
import homeaq.dothattask.data.DataResponse
import homeaq.dothattask.data.repository.GroupRepository
import homeaq.dothattask.data.repository.UserGroupRepository
import homeaq.dothattask.data.repository.UserRepository

class GroupService(
    private val groups: GroupRepository,
    private val userGroups: UserGroupRepository,
    private val users: UserRepository,
) {
    /**
     * A small palette of font/badge colors used to visually distinguish groups
     * in the UI. Picked round-robin based on the current group count so two
     * adjacent creations get different colors.
     */
    private val palette = listOf(
        "#7E57C2", // purple
        "#26A69A", // teal
        "#EF5350", // red
        "#42A5F5", // blue
        "#FFA726", // orange
        "#66BB6A", // green
        "#5C6BC0", // indigo
        "#EC407A", // pink
    )

    suspend fun create(name: String, ownerUsername: String): DataResponse<GroupInfo> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return DataResponse.validationError("Group name cannot be empty")
        if (groups.byName(trimmed) != null) {
            return DataResponse.validationError("A group with this name already exists")
        }
        val color = palette[userGroups.countAllGroups() % palette.size]
        val id = groups.create(trimmed, ownerUsername, color)
        if (id == -1) return DataResponse.databaseError("Unable to retrieve the id of the newly created group")
        userGroups.addMember(ownerUsername, id, GroupRole.ADMIN)
        val info = loadInfo(id) ?: return DataResponse.databaseError("Group created but could not be loaded")
        return DataResponse.success(info, "Group created successfully")
    }

    suspend fun myGroups(username: String): DataResponse<List<GroupInfo>> {
        val list = userGroups.groupsOfUser(username).mapNotNull { loadInfo(it.id) }
        return DataResponse.success(list)
    }

    suspend fun leave(username: String, groupId: Int): DataResponse<Boolean> {
        if (!userGroups.isMember(username, groupId)) {
            return DataResponse.notFound("You are not a member of this group")
        }
        val group = groups.byId(groupId) ?: return DataResponse.notFound("Group not found")
        val isOwner = group.ownerUsername.equals(username, ignoreCase = true)
        val memberCount = userGroups.countMembers(groupId)
        if (isOwner && memberCount > 1) {
            return DataResponse.forbidden(
                "Owner cannot leave while other members remain. Remove the other members or transfer ownership first."
            )
        }
        userGroups.removeMember(username, groupId)
        if (memberCount <= 1) {
            // Last member — clean up the empty group.
            groups.delete(groupId)
        }
        return DataResponse.success(true, "Left the group")
    }

    suspend fun groupById(id: Int): Group? = groups.byId(id)

    private suspend fun loadInfo(groupId: Int): GroupInfo? {
        val group = groups.byId(groupId) ?: return null
        val memberships = userGroups.membersOfGroup(groupId)
        // Resolve display names per member. Skip any row whose user vanished (shouldn't happen via FK cascade).
        val members = memberships.mapNotNull { m ->
            val u = users.userByUsername(m.userUsername) ?: return@mapNotNull null
            GroupMember(username = u.username, name = u.name, role = m.role)
        }
        return GroupInfo(
            id = group.id,
            name = group.name,
            ownerUsername = group.ownerUsername,
            color = group.color,
            members = members,
        )
    }
}
