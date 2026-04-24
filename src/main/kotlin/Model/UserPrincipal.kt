package homeaq.dothattask.Model

import java.security.Principal

data class UserPrincipal(
    val username: String,
    val firstname: String,
    val groupId: Int?,
) : Principal {
    override fun getName(): String = firstname
    fun getUserName(): String = username
}
