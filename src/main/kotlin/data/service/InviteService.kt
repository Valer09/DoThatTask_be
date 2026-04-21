package homeaq.dothattask.data.service

import homeaq.dothattask.Model.GroupRole
import homeaq.dothattask.Model.Invite
import homeaq.dothattask.Model.InviteStatus
import homeaq.dothattask.data.DataResponse
import homeaq.dothattask.data.repository.GroupRepository
import homeaq.dothattask.data.repository.InviteRepository
import homeaq.dothattask.data.repository.UserGroupRepository
import homeaq.dothattask.data.repository.UserRepository

class InviteService(
    private val invites: InviteRepository,
    private val groups: GroupRepository,
    private val userGroups: UserGroupRepository,
    private val users: UserRepository,
) {
    suspend fun send(inviterUsername: String, inviteeUsername: String): DataResponse<Invite> {
        val target = inviteeUsername.trim()
        if (target.isEmpty()) return DataResponse.validationError("Invitee username cannot be empty")
        if (target.equals(inviterUsername, ignoreCase = true)) {
            return DataResponse.validationError("Cannot invite yourself")
        }

        val inviterGroupId = userGroups.groupIdOfUser(inviterUsername)
            ?: return DataResponse.forbidden("You are not in a group")
        val group = groups.byId(inviterGroupId)
            ?: return DataResponse.databaseError("Group not found for inviter")
        if (!group.ownerUsername.equals(inviterUsername, ignoreCase = true)) {
            return DataResponse.forbidden("Only the group owner can invite new members")
        }

        val invitee = users.userByUsername(target)
            ?: return DataResponse.notFound("No user with username '$target'")

        if (userGroups.isMember(invitee.username, inviterGroupId)) {
            return DataResponse.validationError("User is already in this group")
        }
        if (userGroups.groupIdOfUser(invitee.username) != null) {
            return DataResponse.validationError("User is already a member of another group")
        }
        if (invites.existsPending(inviterGroupId, invitee.username)) {
            return DataResponse.validationError("A pending invite already exists for this user")
        }

        val id = invites.create(inviterGroupId, inviterUsername, invitee.username)
        if (id == -1) return DataResponse.databaseError("Unable to create invite")
        val created = invites.byId(id) ?: return DataResponse.databaseError("Invite created but not retrievable")
        return DataResponse.success(created, "Invite sent")
    }

    suspend fun incoming(username: String): DataResponse<List<Invite>> =
        DataResponse.success(invites.incomingPendingFor(username))

    suspend fun accept(inviteId: Int, username: String): DataResponse<Invite> {
        val invite = invites.byId(inviteId) ?: return DataResponse.notFound("Invite not found")
        if (!invite.inviteeUsername.equals(username, ignoreCase = true)) {
            return DataResponse.forbidden("This invite is not addressed to you")
        }
        if (invite.status != InviteStatus.PENDING) {
            return DataResponse.validationError("Invite is no longer pending")
        }
        if (userGroups.groupIdOfUser(username) != null) {
            return DataResponse.validationError("You are already a member of another group")
        }
        userGroups.addMember(username, invite.groupId, GroupRole.MEMBER)
        invites.updateStatus(inviteId, InviteStatus.ACCEPTED)
        val updated = invites.byId(inviteId) ?: invite
        return DataResponse.success(updated, "Invite accepted")
    }

    suspend fun reject(inviteId: Int, username: String): DataResponse<Invite> {
        val invite = invites.byId(inviteId) ?: return DataResponse.notFound("Invite not found")
        if (!invite.inviteeUsername.equals(username, ignoreCase = true)) {
            return DataResponse.forbidden("This invite is not addressed to you")
        }
        if (invite.status != InviteStatus.PENDING) {
            return DataResponse.validationError("Invite is no longer pending")
        }
        invites.updateStatus(inviteId, InviteStatus.REJECTED)
        val updated = invites.byId(inviteId) ?: invite
        return DataResponse.success(updated, "Invite rejected")
    }

    suspend fun revoke(inviteId: Int, callerUsername: String): DataResponse<Invite> {
        val invite = invites.byId(inviteId) ?: return DataResponse.notFound("Invite not found")
        val group = groups.byId(invite.groupId)
            ?: return DataResponse.databaseError("Group not found")
        if (!group.ownerUsername.equals(callerUsername, ignoreCase = true)) {
            return DataResponse.forbidden("Only the group owner can revoke invites")
        }
        if (invite.status != InviteStatus.PENDING) {
            return DataResponse.validationError("Invite is no longer pending")
        }
        invites.updateStatus(inviteId, InviteStatus.REVOKED)
        val updated = invites.byId(inviteId) ?: invite
        return DataResponse.success(updated, "Invite revoked")
    }
}
