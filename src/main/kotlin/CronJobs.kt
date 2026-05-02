package homeaq.dothattask

import homeaq.dothattask.data.service.NotificationService
import io.ktor.server.application.Application
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.ktor.ext.get
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun Application.startDailyReminderJob(notificationService: NotificationService) {

    val log = LoggerFactory.getLogger(NotificationService::class.java)
    launch {
        val app = get<Application>()
        val notificationTime = app.environment.config.property("ktor.fcm.notificationTime").getString()
        val hour = if(notificationTime == "aft") 16 else 9
        while (true) {
            val now = LocalDateTime.now()
            val todayAt9 = LocalDate.now().atTime(hour, 0)
            val nextRun = if (now.isBefore(todayAt9)) todayAt9 else todayAt9.plusDays(1)
            val delayMillis = Duration.between(now, nextRun).toMillis()

            delay(delayMillis)
            try {
                notificationService.sendDailyReminder()
            } catch (e: Exception) {
                log.error("Error daily reminder", e)
            }
        }
    }
}