package homeaq.dothattask.data.service

import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import homeaq.dothattask.Model.NotificationData
import homeaq.dothattask.Model.NotificationType
import homeaq.dothattask.Model.TaskStatus
import homeaq.dothattask.Model.notifications.FirebaseConfig
import homeaq.dothattask.data.repository.FcmTokenRepository
import homeaq.dothattask.data.repository.TaskRepository
import homeaq.dothattask.data.repository.UserRepository
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Boilerplate notification service. Future cron jobs / event handlers can call
 * `sendToUser(...)` to dispatch FCM pushes; this class hides the SDK details
 * and prunes invalid tokens automatically.
 *
 * If Firebase is not configured (no service account), all calls become no-ops
 * with a warning log so the rest of the app keeps working in dev.
 */
class NotificationService(
    private val firebase: FirebaseConfig,
    private val fcmTokens: FcmTokenRepository,
    private val users: UserRepository,
    private val tasks: TaskRepository
) {
    private val log: Logger = LoggerFactory.getLogger(NotificationService::class.java)

    suspend fun sendToUser(
        username: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    ): Int {
        val tokens = fcmTokens.tokensFor(username)
        if (tokens.isEmpty()) {
            log.info("No FCM tokens for '$username'; skipping push")
            return 0
        }
        val messaging = firebase.messaging ?: run {
            log.warn("Firebase not configured; skipping push to '$username' ({} tokens)", tokens.size)
            return 0
        }

        val message = MulticastMessage.builder()
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .putAllData(data)
            .addAllTokens(tokens)
            .build()

        return try {
            val response = messaging.sendEachForMulticast(message)
            // Prune tokens FCM has marked as invalid so we don't keep retrying.
            val invalid = response.responses
                .mapIndexedNotNull { idx, r -> if (!r.isSuccessful) tokens[idx] else null }
            if (invalid.isNotEmpty()) {
                log.info("Pruning {} invalid FCM tokens for '{}'", invalid.size, username)
                fcmTokens.deleteTokens(invalid)
            }
            response.successCount
        } catch (e: Exception) {
            log.error("FCM send failed for '$username': ${e.message}", e)
            0
        }
    }

    /**
     * Stub for the daily-reminder use case the Android client used to schedule
     * locally. Hook this up to a scheduler (Quartz, Ktor scheduling plugin, or
     * a Postgres-driven cron) when the server takes ownership of timing.
     */
    private suspend fun sendDailyReminderToUser(username: String): Int {

        val hasAssignedTasks = tasks.assignedTasksAcrossGroups(username, null).any{ it -> it.status == TaskStatus.ACTIVE }
        val (title, body) =
            if (hasAssignedTasks) "Weekly task reminder" to "Ehy don't forget about your weekly task! 👀"
            else "Weekly task reminder" to "Hey! Don't you want to pick a new task for the week? 👻💪💪"

        return sendToUser(username, title, body,     data = NotificationData.getNotificationData(NotificationType.DailyReminder))
    }

    suspend fun sendDailyReminder() {
        val eligibleUsers = users.getUsersEligibleForReminder()
        eligibleUsers.forEach{ user ->
            sendDailyReminderToUser(user.username)
            users.incrementUnopenedReminders(user.username)
        }
    }
}
