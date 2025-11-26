package homeaq.dothattask.data.service

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskUpdate
import homeaq.dothattask.data.DataResponse
import homeaq.dothattask.data.DataResult
import homeaq.dothattask.data.repository.TaskRepository

class TaskService(private val taskRepository: TaskRepository)
{

    suspend fun all(): DataResponse<List<Task>> = DataResponse.Companion.success(taskRepository.allTasks())
    suspend fun read(name : String): DataResponse<Task?> = taskRepository.taskByName(name) ?.let { DataResponse.Companion.success(taskRepository.taskByName(name))}?: DataResponse.Companion.notFound()

    suspend fun addOrUpdate(taskUpdate: TaskUpdate): DataResponse<Task>
    {
        val newTask = Task.Companion.createFromTaskUpdate(taskUpdate)
        taskRepository.taskByName(taskUpdate.oldName) ?: return create(newTask)

        taskRepository.update(newTask, taskUpdate.oldName)

        return DataResponse.Companion.success(newTask, "Task updated successfully")
    }

    suspend fun delete(name: String): DataResponse<out Any>
    {
        val check = read(name)

        if(check.result == DataResult.NOT_FOUND) return DataResponse.Companion.notFound("Given task doesn't exists already")

        return DataResponse.Companion.success(taskRepository.delete(name))
    }

    private suspend fun create(task: Task): DataResponse<Task> = taskRepository.create(task).takeIf { it == -1 }?.let{ DataResponse.Companion.databaseError("Unable to retrieve the id of the newly inserted task") }?: DataResponse.Companion.success(task, "Task created successfully")
}