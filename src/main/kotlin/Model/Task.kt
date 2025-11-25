package homeaq.dothattask.Model

import kotlinx.serialization.Serializable

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

@Serializable
data class Task
    (
    val id: Int,
    val name: String,
    val description: String,
    val category: TaskCategory,
    val status: TaskStatus
)
