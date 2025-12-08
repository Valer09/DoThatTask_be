package homeaq.dothattask.Model

import kotlinx.serialization.Serializable
@Serializable
data class Task
    (
    val name: String,
    val description: String,
    val category: TaskCategory,
    val status: TaskStatus,
    val ownership_username: String
) {
    companion object {
        fun createFromTaskUpdate(task: TaskUpdate, overrideStatus: TaskStatus? = null): Task = Task(task.newName, task.description, task.category, overrideStatus ?: task.status, task.ownership_username)
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
    val ownership_username: String
)

enum class TaskStatus(val code: Int) {
    TODO(1),
    ACTIVE(2),
    COMPLETED(3);

    companion object {
        fun fromCode(code: Int) = entries.first { it.code == code }
    }
}

enum class TaskCategory(val code: Int)
{
    Social(1),
    Career(2),
    Health(3);

    companion object {
        fun fromCode(code: Int) = entries.first { it.code == code }
    }
}
