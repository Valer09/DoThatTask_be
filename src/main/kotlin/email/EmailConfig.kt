package homeaq.dothattask.email

import io.ktor.server.config.ApplicationConfig

/**
 * Strongly-typed view over the `ktor.email.*` block in `application.conf`.
 *
 * All values are sourced from environment variables (no hardcoded secrets):
 *   - `RESEND_API_KEY`     — Bearer token for api.resend.com (required)
 *   - `RESEND_FROM_EMAIL`  — Verified sender address (required)
 *   - `RESEND_FROM_NAME`   — Friendly sender name (optional)
 *   - `APP_BASE_URL`       — Public origin used when building verification
 *                            links (e.g. https://app.example.com)
 *   - `EMAIL_ENABLED`      — Master switch, `false` keeps the rest of the
 *                            system running in degraded mode (logs only).
 *
 * If [enabled] is true but [apiKey] / [fromEmail] are blank the service
 * still boots: outbound calls become no-ops and emit a log warning so
 * dev environments never break because a key is missing.
 */
class EmailConfig(config: ApplicationConfig) {
    val enabled: Boolean =
        config.propertyOrNull("ktor.email.enabled")?.getString()?.toBoolean() ?: false

    val apiKey: String =
        config.propertyOrNull("ktor.email.resendApiKey")?.getString().orEmpty()

    val fromEmail: String =
        config.propertyOrNull("ktor.email.fromEmail")?.getString().orEmpty()

    val fromName: String =
        config.propertyOrNull("ktor.email.fromName")?.getString().orEmpty()
            .ifBlank { "Do That Task" }

    val appBaseUrl: String =
        config.propertyOrNull("ktor.email.appBaseUrl")?.getString().orEmpty()
            .trimEnd('/')
            .ifBlank { "http://localhost:10000" }

    val verificationTtlHours: Long =
        config.propertyOrNull("ktor.email.verificationTtlHours")?.getString()?.toLongOrNull() ?: 24L

    val isOperational: Boolean
        get() = enabled && apiKey.isNotBlank() && fromEmail.isNotBlank()
}
