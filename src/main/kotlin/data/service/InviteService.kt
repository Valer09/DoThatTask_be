package homeaq.dothattask.data.service

import homeaq.dothattask.Model.GroupRole
import homeaq.dothattask.Model.Invite
import homeaq.dothattask.Model.InviteStatus
import homeaq.dothattask.Model.NotificationData
import homeaq.dothattask.Model.NotificationType
import homeaq.dothattask.data.DataResponse
import homeaq.dothattask.data.repository.GroupRepository
import homeaq.dothattask.data.repository.InviteRepository
import homeaq.dothattask.data.repository.UserGroupRepository
import homeaq.dothattask.data.repository.UserRepository
import homeaq.dothattask.email.EmailService
import org.slf4j.LoggerFactory

class InviteService(
    private val invites: InviteRepository,
    private val groups: GroupRepository,
    private val userGroups: UserGroupRepository,
    private val users: UserRepository,
    private val notification: NotificationService,
    private val emailService: EmailService,
) {

    private val log = LoggerFactory.getLogger(InviteService::class.java)

    /**
     * Sends an invite for [groupId]. The caller must be the owner of that
     * group (multi-group: owners can invite to any group they own; the
     * invitee may already belong to other groups).
     */
    suspend fun send(inviterEmail: String, groupId: Int, inviteeEmail: String): DataResponse<Invite> {
        val target = inviteeEmail.trim()
        if (target.isEmpty()) return DataResponse.validationError("Invitee email cannot be empty")
        if (target.equals(inviterEmail, ignoreCase = true)) {
            return DataResponse.validationError("Cannot invite yourself")
        }

        val group = groups.byId(groupId)
            ?: return DataResponse.notFound("Group not found")
        if (!group.ownerEmail.equals(inviterEmail, ignoreCase = true)) {
            return DataResponse.forbidden("Only the group administrators can invite new members")
        }

        val invitee = users.userByEmail(target)
            ?: return DataResponse.notFound("No user with email '$target'")

        // Group membership and pending-invite checks are now keyed by email.
        if (userGroups.isMember(invitee.email, groupId)) {
            return DataResponse.validationError("User is already in this group")
        }
        if (invites.existsPending(groupId, invitee.email)) {
            return DataResponse.validationError("A pending invite already exists for this user in this group")
        }

        val id = invites.create(groupId, inviterEmail, invitee.email)
        if (id == -1) return DataResponse.databaseError("Unable to create invite")
        val created = invites.byId(id) ?: return DataResponse.databaseError("Invite created but not retrievable")
        val invitationBody = "You received a invitation to join the group ${created.groupName} from ${created.inviterUsername}"

        // FCM tokens are still keyed by username (FcmTokenRepository), so
        // we look the invitee up and send to their username — not email.
        notification.sendToUser(
            invitee.username,
            "Group Invitation",
            invitationBody,
            NotificationData.getNotificationData(NotificationType.GroupInvitation))

        // Email notification — best effort. Skipped silently if the invitee
        // hasn't supplied an email yet (legacy account) or if the inviter
        // record can't be loaded for the "from" address.
        runCatching { dispatchInviteEmail(invitee.email, inviterEmail, created.groupName) }
            .onFailure { log.warn("Invite email dispatch failed: {}", it.message) }

        return DataResponse.success(created, "Invite sent")
    }

    private suspend fun dispatchInviteEmail(inviteeEmail: String, inviterEmail: String, groupName: String) {
        val invitee = users.userByEmail(inviteeEmail) ?: return
        val toEmail = invitee.email.takeIf { it.isNotBlank() } ?: return
        val inviter = users.userByEmail(inviterEmail)
        // Prefer the inviter's email so the message says exactly who invited
        // them. Fallback to the raw email param if no record is on file.
        val inviterContact = inviter?.email?.takeIf { it.isNotBlank() } ?: inviterEmail
        emailService.sendGroupInviteEmail(
            toEmail = toEmail,
            inviteeName = invitee.name,
            inviterEmail = inviterContact,
            groupName = groupName,
        )
    }

    suspend fun incoming(email: String): DataResponse<List<Invite>> =
        DataResponse.success(invites.incomingPendingFor(email))

    suspend fun accept(inviteId: Int, email: String): DataResponse<Invite> {
        val invite = invites.byId(inviteId) ?: return DataResponse.notFound("Invite not found")
        if (!invite.inviteeEmail.equals(email, ignoreCase = true)) {
            return DataResponse.forbidden("This invite is not addressed to you")
        }
        if (invite.status != InviteStatus.PENDING) {
            return DataResponse.validationError("Invite is no longer pending")
        }
        if (userGroups.isMember(email, invite.groupId)) {
            return DataResponse.validationError("You are already a member of this group")
        }
        userGroups.addMember(email, invite.groupId, GroupRole.MEMBER)
        invites.updateStatus(inviteId, InviteStatus.ACCEPTED)
        val updated = invites.byId(inviteId) ?: invite
        return DataResponse.success(updated, "Invite accepted")
    }

    suspend fun reject(inviteId: Int, email: String): DataResponse<Invite> {
        val invite = invites.byId(inviteId) ?: return DataResponse.notFound("Invite not found")
        if (!invite.inviteeEmail.equals(email, ignoreCase = true)) {
            return DataResponse.forbidden("This invite is not addressed to you")
        }
        if (invite.status != InviteStatus.PENDING) {
            return DataResponse.validationError("Invite is no longer pending")
        }
        invites.updateStatus(inviteId, InviteStatus.REJECTED)
        val updated = invites.byId(inviteId) ?: invite
        return DataResponse.success(updated, "Invite rejected")
    }

    suspend fun revoke(inviteId: Int, callerEmail: String): DataResponse<Invite> {
        val invite = invites.byId(inviteId) ?: return DataResponse.notFound("Invite not found")
        val group = groups.byId(invite.groupId)
            ?: return DataResponse.databaseError("Group not found")
        if (!group.ownerEmail.equals(callerEmail, ignoreCase = true)) {
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
