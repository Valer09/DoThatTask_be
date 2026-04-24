package homeaq.dothattask.Model

import kotlinx.serialization.Serializable

enum class GroupRole(val code: Int) {
    MEMBER(1),
    ADMIN(2);

    companion object {
        fun fromCode(code: Int): GroupRole = entries.firstOrNull { it.code == code } ?: MEMBER
    }
}

@Serializable
data class UserGroup(
    val userUsername: String,
    val groupId: Int,
    val role: GroupRole = GroupRole.MEMBER,
)
