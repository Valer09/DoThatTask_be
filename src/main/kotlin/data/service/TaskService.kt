package homeaq.dothattask.data.service

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskCategory
import homeaq.dothattask.Model.TaskStatus
import homeaq.dothattask.Model.TaskUpdate
import homeaq.dothattask.data.DataResponse
import homeaq.dothattask.data.DataResult
import homeaq.dothattask.data.repository.TaskRepository

class TaskService(private val taskRepository: TaskRepository)
{

    suspend fun all(groupId: Int): DataResponse<List<Task>> =
        DataResponse.success(taskRepository.allTasks(groupId))

    /**
     * Search tasks within a group with optional filters. Tasks assigned to the
     * caller are always excluded (so the "secret task" rule still holds).
     */
    suspend fun search(
        groupId: Int,
        callerUsername: String,
        creator: String?,
        category: TaskCategory?,
        assignee: String?,
    ): DataResponse<List<Task>> {
        val list = taskRepository.searchTasks(
            groupId = groupId,
            callerUsername = callerUsername,
            creator = creator?.takeIf { it.isNotBlank() },
            category = category,
            assignee = assignee?.takeIf { it.isNotBlank() },
        )
        return DataResponse.success(list)
    }

    suspend fun getCompletedTasks(userName: String): DataResponse<List<Task>> =
        DataResponse.success(taskRepository.completedTasksAcrossGroups(userName))

    suspend fun tasksByUser(username: String, groupId: Int): DataResponse<List<Task>> =
        DataResponse.success(taskRepository.tasksByUser(username, groupId))

    suspend fun assignedTask(username: String): DataResponse<Task> {
        val active = taskRepository.activeTaskAcrossGroups(username)
            ?: return DataResponse.notFound("No active tasks assigned to this user")
        return DataResponse.success(active)
    }

    suspend fun pickTask(username: String, category: String): DataResponse<Boolean> {
        val parsedCategory = TaskCategory.fromName(category)
            ?: return DataResponse.validationError("Unknown category '$category'")
        val candidates = taskRepository.todoTasksAcrossGroups(username, parsedCategory)
        if (candidates.isEmpty()) return DataResponse.notFound("No tasks assigned to this user")

        val picked = candidates.random()
        taskRepository.update(
            Task(
                name = picked.name,
                description = picked.description,
                category = picked.category,
                status = TaskStatus.ACTIVE,
                ownership_username = picked.ownership_username,
            ),
            picked.name,
            picked.groupId,
        )
        return DataResponse.success(true)
    }

    suspend fun unassign(taskName: String, groupId: Int, callerUsername: String): DataResponse<Boolean> {
        val task = taskRepository.taskByName(taskName, groupId)
            ?: return DataResponse.notFound("No task found with this name")
        val creator = taskRepository.creatorOf(task.name, groupId)
        if (creator != null && !creator.equals(callerUsername, ignoreCase = true)) {
            return DataResponse.forbidden("Only the task creator can unassign it")
        }
        taskRepository.unAssign(task.name, groupId)
        return DataResponse.success(true)
    }

    suspend fun complete(taskName: String, groupId: Int): DataResponse<Task> {
        val existing = taskRepository.taskByName(taskName, groupId)
            ?: return DataResponse.notFound("No task with the provided name found")
        taskRepository.update(
            Task(
                name = existing.name,
                description = existing.description,
                category = existing.category,
                status = TaskStatus.COMPLETED,
                ownership_username = existing.ownership_username,
            ),
            existing.name,
            groupId,
        )
        val updated = taskRepository.taskByName(taskName, groupId)
            ?: return DataResponse.databaseError("cannot retrieve updated task")
        return DataResponse.success(updated)
    }

    suspend fun completeActiveTask(username: String): DataResponse<Task> {
        val active = taskRepository.activeTaskAcrossGroups(username)
            ?: return DataResponse.notFound("No active tasks assigned to this user")
        return complete(active.name, active.groupId)
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

        val creator = taskRepository.creatorOf(existing.name, groupId)
        if (creator != null && !creator.equals(callerUsername, ignoreCase = true)) {
            return DataResponse.forbidden("Only the task creator can modify it")
        }

        val newTask = Task.createFromTaskUpdate(taskUpdate, existing.status)
        taskRepository.update(newTask, taskUpdate.oldName, groupId)
        val refreshed = taskRepository.taskByName(newTask.name, groupId) ?: newTask
        return DataResponse.success(refreshed, "Task updated successfully")
    }

    suspend fun delete(name: String, groupId: Int, callerUsername: String): DataResponse<Task> {
        val existing = taskRepository.taskByName(name, groupId)
            ?: return DataResponse.notFound("Given task doesn't exists already")
        val creator = taskRepository.creatorOf(existing.name, groupId)
        if (creator != null && !creator.equals(callerUsername, ignoreCase = true)) {
            return DataResponse.forbidden("Only the task creator can delete it")
        }
        taskRepository.delete(existing.name, groupId)
        return DataResponse.success(existing)
    }

    private suspend fun create(task: Task, creatorUsername: String, groupId: Int): DataResponse<Task> {
        val id = taskRepository.create(task, creatorUsername, groupId)
        if (id == -1) return DataResponse.databaseError("Unable to retrieve the id of the newly inserted task")
        val refreshed = taskRepository.taskByName(task.name, groupId) ?: task
        return DataResponse.success(refreshed, "Task created successfully")
    }
}
