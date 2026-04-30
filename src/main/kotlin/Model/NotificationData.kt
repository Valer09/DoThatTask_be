package homeaq.dothattask.Model

enum class NotificationType{DailyReminder, GroupInvitation,}

class NotificationData
{
    companion object {

        fun getNotificationData(type : NotificationType) : Map<String, String>
        {
            return when(type) {

                NotificationType.DailyReminder -> mapOf(
                    "type" to "task_reminder",
                    "targetId" to ""
                )
                NotificationType.GroupInvitation -> mapOf(
                    "type" to "group_invitation",
                    "targetId" to ""
                )
            }
        }
    }
}