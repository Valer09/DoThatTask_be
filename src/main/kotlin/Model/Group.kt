package homeaq.dothattask.Model

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: Int,
    val name: String,
    val ownerEmail: String,
    val color: String,
)

@Serializable
data class GroupMember(
    val username: String?,
    val name: String,
    val role: GroupRole,
    val email: String
)

@Serializable
data class GroupInfo(
    val id: Int,
    val name: String,
    val ownerEmail: String,
    val color: String,
    val members: List<GroupMember>,
)

@Serializable
data class GroupSummary(
    val id: Int,
    val name: String,
    val color: String,
)

@Serializable
data class CreateGroupRequest(val name: String)

@Serializable
data class SendInviteRequest(val inviteeEmail: String)
