package homeaq.dothattask.Model.notifications

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.config.ApplicationConfig
import java.io.FileInputStream

/**
 * Wraps Firebase Admin SDK initialization. Tolerant of missing credentials —
 * if no service-account JSON is configured the app boots normally and
 * `messaging` returns null. Production should set GOOGLE_APPLICATION_CREDENTIALS
 * (env var) or `ktor.firebase.credentialsPath` (config) to the path of the
 * service-account JSON downloaded from the Firebase console.
 */
class FirebaseConfig(application: Application) {
    private val log = application.log

    val messaging: FirebaseMessaging? = initialize(application.environment.config, log)

    val isEnabled: Boolean get() = messaging != null

    private fun initialize(config: ApplicationConfig, log: org.slf4j.Logger): FirebaseMessaging? = try {
        val credentials = loadCredentials(config, log)
        if (credentials == null) {
            log.warn(
                "Firebase Admin SDK not initialized — no GOOGLE_APPLICATION_CREDENTIALS env var " +
                        "and no ktor.firebase.credentialsPath set. Push notifications disabled."
            )
            null
        } else {
            val options = FirebaseOptions.builder().setCredentials(credentials).build()
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                log.info("Firebase Admin SDK initialized")
            }
            FirebaseMessaging.getInstance()
        }
    } catch (e: Exception) {
        log.warn("Firebase Admin SDK initialization failed: ${e.message}. Push notifications disabled.", e)
        null
    }

    private fun loadCredentials(config: ApplicationConfig, log: org.slf4j.Logger): GoogleCredentials? {
        // 1. Explicit path via ktor config (preferred for non-default deploys).
        val explicitPath = config.propertyOrNull("ktor.firebase.credentialsPath")?.getString()
        if (!explicitPath.isNullOrBlank()) {
            log.info("Loading Firebase credentials from $explicitPath")
            return FileInputStream(explicitPath).use(GoogleCredentials::fromStream)
        }
        // 2. Default ADC path (GOOGLE_APPLICATION_CREDENTIALS or gcloud login).
        return try {
            GoogleCredentials.getApplicationDefault()
        } catch (_: Exception) {
            null
        }
    }
}
