package homeaq.dothattask.Model

import kotlinx.serialization.Serializable

@Serializable
data class Task
    (
    val name: String,
    val description: String,
    val category: TaskCategory,
    val status: TaskStatus,
    val ownership_username: String,
    val groupId: Int = 0,
    val groupName: String = "",
    val groupColor: String = "",
    val createdAt: String = "",
) {
    companion object {
        fun createFromTaskUpdate(task: TaskUpdate, overrideStatus: TaskStatus? = null): Task = Task(
            name = task.newName,
            description = task.description,
            category = task.category,
            status = overrideStatus ?: task.status,
            ownership_username = task.ownership_username,
        )
    }
}

@Serializable
data class TaskUpdate
    (
    val oldName: String,
    val newName: String,
    val description: String,
    val category: TaskCategory,
    val status: TaskStatus,
    val ownership_username: String,
)

enum class TaskStatus(val code: Int) {
    TODO(1),
    ACTIVE(2),
    COMPLETED(3);

    companion object {
        fun fromCode(code: Int) = entries.first { it.code == code }
    }
}

/**
 * Reference to a row in the global `categories` table. Defaults exist for
 * all groups (Social/Career/Health, ids 1..3); custom categories are added
 * per-group via `POST /api/categories` and unlinked via DELETE.
 *
 * Default values keep deserialization tolerant: clients can send just an id
 * and the server resolves the rest. The server always echoes back the full
 * triple in responses.
 */
@Serializable
data class TaskCategory(
    val id: Int = 0,
    val name: String = "",
    val color: String = "",
) {
    companion object {
        // Built-in defaults — ids must match CategoriesSchema.DEFAULTS so the
        // pre-seeded rows in the categories table line up with these refs.
        val Social = TaskCategory(1, "Social", "#42A5F5")
        val Career = TaskCategory(2, "Career", "#26A69A")
        val Health = TaskCategory(3, "Health", "#EF5350")

        /**
         * Normalize a user-typed category name: trim, lowercase the rest,
         * capitalize the first letter. "  hOuSeWoRk " → "Housework".
         */
        fun normalizeName(raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return trimmed
            return trimmed[0].uppercaseChar() + trimmed.substring(1).lowercase()
        }
    }
}
