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
    suspend fun create(name: String, ownerUsername: String): DataResponse<GroupInfo> {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return DataResponse.validationError("Group name cannot be empty")
        if (userGroups.groupIdOfUser(ownerUsername) != null) {
            return DataResponse.forbidden("User is already a member of a group")
        }
        if (groups.byName(trimmed) != null) {
            return DataResponse.validationError("A group with this name already exists")
        }
        val id = groups.create(trimmed, ownerUsername)
        if (id == -1) return DataResponse.databaseError("Unable to retrieve the id of the newly created group")
        userGroups.addMember(ownerUsername, id, GroupRole.ADMIN)
        val info = loadInfo(id) ?: return DataResponse.databaseError("Group created but could not be loaded")
        return DataResponse.success(info, "Group created successfully")
    }

    suspend fun myGroup(username: String): DataResponse<GroupInfo> {
        val gid = userGroups.groupIdOfUser(username) ?: return DataResponse.notFound("User is not in any group")
        val info = loadInfo(gid) ?: return DataResponse.notFound("Group not found")
        return DataResponse.success(info)
    }

    suspend fun leave(username: String): DataResponse<Boolean> {
        val gid = userGroups.groupIdOfUser(username) ?: return DataResponse.notFound("User is not in any group")
        val group = groups.byId(gid) ?: return DataResponse.notFound("Group not found")
        val isOwner = group.ownerUsername.equals(username, ignoreCase = true)
        val memberCount = userGroups.countMembers(gid)
        if (isOwner && memberCount > 1) {
            return DataResponse.forbidden(
                "Owner cannot leave while other members remain. Remove the other members or transfer ownership first."
            )
        }
        userGroups.removeMember(username, gid)
        if (memberCount <= 1) {
            // Last member — clean up the empty group.
            groups.delete(gid)
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
            members = members,
        )
    }
}
