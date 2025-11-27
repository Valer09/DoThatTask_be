package homeaq.dothattask.data.service

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskUpdate
import homeaq.dothattask.data.DataResponse
import homeaq.dothattask.data.DataResult
import homeaq.dothattask.data.repository.TaskRepository

class TaskService(private val taskRepository: TaskRepository)
{

    suspend fun all(): DataResponse<List<Task>> = DataResponse.success(taskRepository.allTasks())

    suspend fun tasksByUser(username : String): DataResponse<List<Task>> = DataResponse.success(taskRepository.tasksByUser(username))

    suspend fun read(name : String): DataResponse<Task?> = taskRepository.taskByName(name) ?.let { DataResponse.success(taskRepository.taskByName(name))}?: DataResponse.Companion.notFound()

    suspend fun addOrUpdate(taskUpdate: TaskUpdate): DataResponse<Task>
    {
        val newTask = Task.createFromTaskUpdate(taskUpdate)
        taskRepository.taskByName(taskUpdate.oldName) ?: return create(newTask, taskUpdate.ownership_username)

        taskRepository.update(newTask, taskUpdate.oldName, taskUpdate.ownership_username)

        return DataResponse.success(newTask, "Task updated successfully")
    }

    suspend fun delete(name: String): DataResponse<out Any>
    {
        val check = read(name)

        if(check.result == DataResult.NOT_FOUND) return DataResponse.Companion.notFound("Given task doesn't exists already")

        return DataResponse.success(taskRepository.delete(name))
    }

    private suspend fun create(task: Task, ownershipUsername: String): DataResponse<Task> =
        taskRepository.create(task, ownershipUsername).takeIf { it == -1 }?.
        let{ DataResponse.Companion.databaseError("Unable to retrieve the id of the newly inserted task") }?:
        DataResponse.success(task, "Task created successfully")
}