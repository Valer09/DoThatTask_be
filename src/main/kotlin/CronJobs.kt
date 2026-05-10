package homeaq.dothattask

import homeaq.dothattask.data.service.NotificationService
import io.ktor.server.application.Application
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.ktor.ext.get
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Daily push reminder job.
 *
 * Two configuration knobs in `application.conf`:
 *  - `ktor.fcm.notificationTime` — `"aft"` (17:00) or `"mor"` (09:00)
 *  - `ktor.fcm.timezone` — IANA zone id, defaults to `Europe/Rome`. We
 *    explicitly schedule in this zone so the alarm fires at the wall-clock
 *    hour the operator configured, regardless of the JVM/container default
 *    (which on most Docker images is UTC and would shift Italian users by
 *    1–2 hours depending on DST).
 */
fun Application.startDailyReminderJob(notificationService: NotificationService) {

    val log = LoggerFactory.getLogger(NotificationService::class.java)
    launch {
        val app = get<Application>()
        val notificationTime = app.environment.config.property("ktor.fcm.notificationTime").getString()
        val hour = if (notificationTime == "aft") 15 else 9
        val zoneId = runCatching {
            ZoneId.of(app.environment.config.property("ktor.fcm.timezone").getString())
        }.getOrElse { ZoneId.of("Europe/Rome") }

        log.info("Daily reminder scheduled at $hour:00 in zone $zoneId (JVM default = ${ZoneId.systemDefault()})")

        while (true) {
            val now = ZonedDateTime.now(zoneId)
            val todayAtHour = now
                .withHour(hour).withMinute(0).withSecond(0).withNano(0)
            val nextRun = if (now.isBefore(todayAtHour)) todayAtHour else todayAtHour.plusDays(1)
            val delayMillis = Duration.between(now, nextRun).toMillis()

            log.info("Next daily reminder at $nextRun (in ${delayMillis / 1000 / 60} minutes)")
            delay(delayMillis)
            try {
                notificationService.sendDailyReminder()
            } catch (e: Exception) {
                log.error("Error daily reminder", e)
            }
        }
    }
}
