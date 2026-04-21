package homeaq.dothattask.Model

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: Int,
    val name: String,
    val ownerUsername: String,
)

@Serializable
data class GroupMember(
    val username: String,
    val name: String,
    val role: GroupRole,
)

@Serializable
data class GroupInfo(
    val id: Int,
    val name: String,
    val ownerUsername: String,
    val members: List<GroupMember>,
)

@Serializable
data class CreateGroupRequest(val name: String)

@Serializable
data class SendInviteRequest(val inviteeUsername: String)
