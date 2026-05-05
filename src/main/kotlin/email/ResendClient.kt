package homeaq.dothattask.email

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

/**
 * Single envelope passed to Resend's `POST /emails` endpoint.
 * Both `text` and `html` are optional individually but at least one must be
 * non-null per Resend's contract — [EmailService] always supplies both.
 */
@Serializable
internal data class ResendEmailRequest(
    val from: String,
    val to: List<String>,
    val subject: String,
    val text: String? = null,
    val html: String? = null,
)

@Serializable
internal data class ResendEmailResponse(val id: String? = null)

/**
 * Thin wrapper around the Resend REST API. The transport is created once
 * and reused; if the config is non-operational (missing API key, or
 * `EMAIL_ENABLED=false`) the client logs and returns false instead of
 * issuing the call — callers treat this as "best effort delivery".
 */
class ResendClient(private val config: EmailConfig) {

    private val log = LoggerFactory.getLogger(ResendClient::class.java)

    private val http: HttpClient by lazy {
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json()
            }
            expectSuccess = false
        }
    }

    /**
     * Sends a single email. Never throws — network and remote errors are
     * caught and reported as `false` so an outbound delivery problem can
     * never block a user-facing response.
     */
    suspend fun send(
        to: String,
        subject: String,
        text: String,
        html: String,
    ): Boolean {
        if (!config.isOperational) {
            log.warn(
                "Email send skipped (enabled={}, apiKeyPresent={}, fromEmail={}). " +
                        "To '{}', subject '{}'",
                config.enabled,
                config.apiKey.isNotBlank(),
                config.fromEmail.ifBlank { "<unset>" },
                to,
                subject,
            )
            return false
        }
        val recipient = to.trim()
        if (recipient.isBlank()) {
            log.warn("Email send skipped: empty recipient (subject '{}')", subject)
            return false
        }
        return runCatching {
            val sender = if (config.fromName.isNotBlank()) {
                "${config.fromName} <${config.fromEmail}>"
            } else {
                config.fromEmail
            }
            val response = http.post("https://api.resend.com/emails") {
                contentType(ContentType.Application.Json)
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
                }
                setBody(
                    ResendEmailRequest(
                        from = sender,
                        to = listOf(recipient),
                        subject = subject,
                        text = text,
                        html = html,
                    )
                )
            }
            val ok = response.status.value in 200..299
            if (!ok) {
                log.warn(
                    "Resend rejected email to '{}' subject '{}' status {} body {}",
                    recipient,
                    subject,
                    response.status.value,
                    response.bodyAsText().take(500),
                )
            }
            ok
        }.getOrElse { e ->
            log.warn("Email send to '{}' failed: {}", recipient, e.message)
            false
        }
    }
}
