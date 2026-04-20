package homeaq.dothattask.Model

import kotlinx.serialization.Serializable

@Serializable
data class Group(
    val id: Int,
    val name: String,
    val ownerUsername: String,
)

@Serializable
data class GroupInfo(
    val id: Int,
    val name: String,
    val ownerUsername: String,
    val members: List<User>,
)
