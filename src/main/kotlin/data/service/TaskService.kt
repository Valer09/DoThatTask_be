package homeaq.dothattask.data.service

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskStatus
import homeaq.dothattask.Model.TaskUpdate
import homeaq.dothattask.Model.UserPrincipal
import homeaq.dothattask.data.DataResponse
import homeaq.dothattask.data.DataResult
import homeaq.dothattask.data.repository.TaskRepository
import io.ktor.server.auth.principal

class TaskService(private val taskRepository: TaskRepository)
{

    suspend fun all(): DataResponse<List<Task>> = DataResponse.success(taskRepository.allTasks())

    suspend fun tasksByUser(username : String): DataResponse<List<Task>> = DataResponse.success(taskRepository.tasksByUser(username))

    suspend fun assignedTask(username : String): DataResponse<Task>
    {
        val task = taskRepository.tasksByUser(username)
        if(task.isEmpty()) return DataResponse.notFound("No tasks assigned to this user")
        return task.firstOrNull { it.status == TaskStatus.ACTIVE }?.let { DataResponse.success(it) }?: DataResponse.notFound("No active tasks assigned to this user")
    }

    suspend fun pickTask(username: String) : DataResponse<Boolean>
    {
        val tasks = taskRepository.tasksByUser(username)
        if(tasks.isEmpty()) return DataResponse.notFound("No tasks assigned to this user")

        val pickedTask = tasks.filterNot { it.status == TaskStatus.ACTIVE }.random()
        taskRepository.update(
            Task(pickedTask.name, pickedTask.description, pickedTask.category, TaskStatus.ACTIVE, pickedTask.ownership_username),
            pickedTask.name, pickedTask.ownership_username)

        return DataResponse.success(true)
    }

    suspend fun complete(taskName: String) : DataResponse<Task>
    {
        var taskByName = taskRepository.taskByName(taskName) ?: return DataResponse.notFound("No task with the provided name found")

        taskRepository.update(
            Task(
                taskName,
                taskByName.description,
                taskByName.category,
                TaskStatus.COMPLETED,
                taskByName.ownership_username), taskName, taskByName.ownership_username)

        taskByName = taskRepository.taskByName(taskName) ?: return DataResponse.databaseError("cannot retrieve updated task")

        return DataResponse.success(taskByName)
    }

    suspend fun completeActiveTask(username: String): DataResponse<Task>
    {
        val result = assignedTask(username).result
        if(result == DataResult.NOT_FOUND) return DataResponse.notFound("No active tasks assigned to this user")
        return complete(assignedTask(username).data?.name!!)
    }


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