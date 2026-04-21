package homeaq.dothattask.data.service

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskStatus
import homeaq.dothattask.Model.TaskUpdate
import homeaq.dothattask.data.DataResponse
import homeaq.dothattask.data.DataResult
import homeaq.dothattask.data.repository.TaskRepository

class TaskService(private val taskRepository: TaskRepository)
{

    suspend fun all(groupId: Int): DataResponse<List<Task>> =
        DataResponse.success(taskRepository.allTasks(groupId))

    suspend fun getCompletedTasks(userName: String, groupId: Int): DataResponse<List<Task>> =
        DataResponse.success(taskRepository.completedTasks(userName, groupId))

    suspend fun tasksByUser(username: String, groupId: Int): DataResponse<List<Task>> =
        DataResponse.success(taskRepository.tasksByUser(username, groupId))

    suspend fun assignedTask(username: String, groupId: Int): DataResponse<Task> {
        val tasks = taskRepository.tasksByUser(username, groupId)
        if (tasks.isEmpty()) return DataResponse.notFound("No tasks assigned to this user")
        return tasks.firstOrNull { it.status == TaskStatus.ACTIVE }
            ?.let { DataResponse.success(it) }
            ?: DataResponse.notFound("No active tasks assigned to this user")
    }

    suspend fun pickTask(username: String, category: String, groupId: Int): DataResponse<Boolean> {
        val tasks = taskRepository.tasksByUser(username, groupId)
        if (tasks.isEmpty()) return DataResponse.notFound("No tasks assigned to this user")

        val candidates = tasks.filter { it.status == TaskStatus.TODO && it.category.name == category }
        if (candidates.isEmpty()) return DataResponse.notFound("No tasks assigned to this user")

        val picked = candidates.random()
        taskRepository.update(
            Task(picked.name, picked.description, picked.category, TaskStatus.ACTIVE, picked.ownership_username),
            picked.name,
            groupId,
        )
        return DataResponse.success(true)
    }

    suspend fun unassign(taskName: String, groupId: Int): DataResponse<Boolean> {
        val task = taskRepository.taskByName(taskName, groupId)
            ?: return DataResponse.notFound("No task found with this name")
        taskRepository.unAssign(task.name, groupId)
        return DataResponse.success(true)
    }

    suspend fun complete(taskName: String, groupId: Int): DataResponse<Task> {
        val existing = taskRepository.taskByName(taskName, groupId)
            ?: return DataResponse.notFound("No task with the provided name found")
        taskRepository.update(
            Task(existing.name, existing.description, existing.category, TaskStatus.COMPLETED, existing.ownership_username),
            existing.name,
            groupId,
        )
        val updated = taskRepository.taskByName(taskName, groupId)
            ?: return DataResponse.databaseError("cannot retrieve updated task")
        return DataResponse.success(updated)
    }

    suspend fun completeActiveTask(username: String, groupId: Int): DataResponse<Task> {
        val active = assignedTask(username, groupId)
        if (active.result == DataResult.NOT_FOUND) return DataResponse.notFound("No active tasks assigned to this user")
        return complete(active.data!!.name, groupId)
    }

    suspend fun read(name: String, groupId: Int): DataResponse<Task?> =
        taskRepository.taskByName(name, groupId)
            ?.let { DataResponse.success<Task?>(it) }
            ?: DataResponse.notFound()

    suspend fun addOrUpdate(taskUpdate: TaskUpdate, callerUsername: String, groupId: Int): DataResponse<Task> {
        val existing = taskRepository.taskByName(taskUpdate.oldName, groupId)
        if (existing == null) {
            return create(
                Task.createFromTaskUpdate(taskUpdate),
                creatorUsername = callerUsername,
                groupId = groupId,
            )
        }

        val newTask = Task.createFromTaskUpdate(taskUpdate, existing.status)
        taskRepository.update(newTask, taskUpdate.oldName, groupId)
        return DataResponse.success(newTask, "Task updated successfully")
    }

    suspend fun delete(name: String, groupId: Int): DataResponse<out Any> {
        val existing = taskRepository.taskByName(name, groupId)
            ?: return DataResponse.notFound("Given task doesn't exists already")
        taskRepository.delete(existing.name, groupId)
        return DataResponse.success(Unit)
    }

    private suspend fun create(task: Task, creatorUsername: String, groupId: Int): DataResponse<Task> {
        val id = taskRepository.create(task, creatorUsername, groupId)
        return if (id == -1) DataResponse.databaseError("Unable to retrieve the id of the newly inserted task")
        else DataResponse.success(task, "Task created successfully")
    }
}
