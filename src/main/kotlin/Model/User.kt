package homeaq.dothattask.Model

import at.favre.lib.crypto.bcrypt.BCrypt
import kotlinx.serialization.Serializable

@Serializable
data class User(val name: String, val username: String, val password_hash: String)

class PasswordHash
{
    companion object
    {
        fun hashPassword(plain: String): String
        {
            return BCrypt.withDefaults().hashToString(12, plain.toCharArray())
        }

        fun verifyPassword(plain: String, hashed: String): Boolean
        {
            val result = BCrypt.verifyer().verify(plain.toCharArray(), hashed)
            return result.verified
        }
    }
}
