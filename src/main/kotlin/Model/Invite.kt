package homeaq.dothattask.Model

import kotlinx.serialization.Serializable

enum class InviteStatus(val code: Int) {
    PENDING(1),
    ACCEPTED(2),
    REJECTED(3),
    REVOKED(4);

    companion object {
        fun fromCode(code: Int): InviteStatus = entries.firstOrNull { it.code == code } ?: PENDING
    }
}

@Serializable
data class Invite(
    val id: Int,
    val groupId: Int,
    val groupName: String,
    val groupColor: String,
    val inviterUsername: String,
    val inviteeUsername: String,
    val status: InviteStatus,
)
