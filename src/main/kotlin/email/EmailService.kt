package homeaq.dothattask.email

/**
 * Domain-level email helpers. Templates live here so `AuthService` and
 * `InviteService` only know what content they want to deliver, not how
 * the HTML/text bodies are composed.
 */
class EmailService(
    private val resend: ResendClient,
    private val config: EmailConfig,
) {

    suspend fun sendVerificationEmail(toEmail: String, displayName: String, verifyUrl: String): Boolean {
        val safeName = displayName.ifBlank { "there" }
        val subject = "Confirm your Do That Task account"
        val text = """
            Hi $safeName,

            welcome to Do That Task! Please confirm your email address by
            opening the link below:

            $verifyUrl

            This link is valid for ${config.verificationTtlHours} hours.
            If you did not create this account, you can safely ignore this email.
        """.trimIndent()
        val html = """
            <div style="font-family:Arial,Helvetica,sans-serif;line-height:1.5;color:#1f2937">
              <h2 style="color:#7B2FBE">Welcome to Do That Task</h2>
              <p>Hi <strong>${escape(safeName)}</strong>,</p>
              <p>Please confirm your email address by clicking the button below.</p>
              <p>
                <a href="${escape(verifyUrl)}"
                   style="display:inline-block;background:#7B2FBE;color:#fff;
                          text-decoration:none;padding:10px 18px;border-radius:8px;
                          font-weight:bold">
                  Confirm email
                </a>
              </p>
              <p>If the button does not work, paste this link into your browser:<br>
                <a href="${escape(verifyUrl)}">${escape(verifyUrl)}</a></p>
              <p style="color:#6b7280;font-size:12px">
                This link is valid for ${config.verificationTtlHours} hours.
                If you did not create this account, please ignore this message.
              </p>
            </div>
        """.trimIndent()
        return resend.send(toEmail, subject, text, html)
    }

    suspend fun sendGroupInviteEmail(
        toEmail: String,
        inviteeName: String,
        inviterEmail: String,
        groupName: String,
    ): Boolean {
        val safeName = inviteeName.ifBlank { "there" }
        val subject = "You've been invited to join \"$groupName\" on Do That Task"
        val text = """
            Hi $safeName,

            $inviterEmail has invited you to join the group "$groupName" on
            Do That Task. Open the app to accept or decline the invitation.
        """.trimIndent()
        val html = """
            <div style="font-family:Arial,Helvetica,sans-serif;line-height:1.5;color:#1f2937">
              <h2 style="color:#7B2FBE">New group invitation</h2>
              <p>Hi <strong>${escape(safeName)}</strong>,</p>
              <p><strong>${escape(inviterEmail)}</strong> has invited you to join the
                 group <strong>${escape(groupName)}</strong> on Do That Task.</p>
              <p>Open the app to accept or decline the invitation.</p>
            </div>
        """.trimIndent()
        return resend.send(toEmail, subject, text, html)
    }

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")
}
