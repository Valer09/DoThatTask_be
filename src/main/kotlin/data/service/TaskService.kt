package homeaq.dothattask.data.service

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskStatus
import homeaq.dothattask.Model.TaskUpdate
import homeaq.dothattask.data.DataResponse
import homeaq.dothattask.data.DataResult
import homeaq.dothattask.data.repository.TaskRepository
import io.ktor.server.plugins.NotFoundException

class TaskService(private val taskRepository: TaskRepository)
{

    suspend fun all(): DataResponse<List<Task>> = DataResponse.success(taskRepository.allTasks())

    suspend fun getCompletedTasks(userName: String) : DataResponse<List<Task>>
    {
        try
        {
            val taskList= taskRepository.completedTasks(userName)
            if(taskList.isEmpty()) return DataResponse.success(ArrayList<Task>())
            return DataResponse.success(taskList)
        }
        catch(ex : NotFoundException)
        {
            return DataResponse.notFound("User not found")
        }
    }

    suspend fun tasksByUser(username : String): DataResponse<List<Task>> = DataResponse.success(taskRepository.tasksByUser(username))

    suspend fun assignedTask(username : String): DataResponse<Task>
    {
        val task = taskRepository.tasksByUser(username)
        if(task.isEmpty()) return DataResponse.notFound("No tasks assigned to this user")
        return task.firstOrNull { it.status == TaskStatus.ACTIVE }?.let { DataResponse.success(it) }?: DataResponse.notFound("No active tasks assigned to this user")
    }

    suspend fun pickTask(username: String, taskName: String) : DataResponse<Boolean>
    {
        val tasks = taskRepository.tasksByUser(username)
        if(tasks.isEmpty()) return DataResponse.notFound("No tasks assigned to this user")

        val filteredTasks = tasks.filter { it.status == TaskStatus.TODO && it.category.name == taskName }
        if(filteredTasks.count() < 1) return DataResponse.notFound("No tasks assigned to this user")

        val pickedTask = filteredTasks.random()

        taskRepository.update(
            Task(pickedTask.name, pickedTask.description, pickedTask.category, TaskStatus.ACTIVE, pickedTask.ownership_username),
            pickedTask.name, pickedTask.ownership_username)


        return DataResponse.success(true)
    }

    suspend fun unassign(taskName: String) : DataResponse<Boolean>
    {
        val task = taskRepository.taskByName(taskName)
        if(task == null ) return DataResponse.notFound("No task found with this name")


        taskRepository.unAssign(taskName)
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