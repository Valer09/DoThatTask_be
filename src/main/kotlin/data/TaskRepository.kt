package homeaq.dothattask.data

import homeaq.dothattask.Model.Task

interface TaskRepository
{
    fun allTasks(): List<Task>
    fun taskByName(name: String): Task?
    fun addOrUpdateTask(task: Task)
    fun removeTask(name: String): Boolean
}