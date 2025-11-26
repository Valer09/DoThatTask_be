package homeaq.dothattask.data

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskCategory
import homeaq.dothattask.Model.TaskStatus
import homeaq.dothattask.Model.TaskUpdate
import java.sql.Connection

class TaskService(private val taskRepository: TaskRepository)
{

    suspend fun all(): DataResponse<List<Task>> = DataResponse.success(taskRepository.allTasks())
    suspend fun read(name : String): DataResponse<Task?> = taskRepository.taskByName(name) ?.let {DataResponse.success(taskRepository.taskByName(name))}?: DataResponse.notFound()

    suspend fun addOrUpdate(taskUpdate: TaskUpdate): DataResponse<Task>
    {
        val newTask = Task.createFromTaskUpdate(taskUpdate)
        taskRepository.taskByName(taskUpdate.oldName) ?: return create(newTask)

        taskRepository.update(newTask, taskUpdate.oldName)

        return DataResponse.success(newTask, "Task updated successfully")
    }

    suspend fun delete(name: String): DataResponse<out Any>
    {
        val check = read(name)

        if(check.result == DataResult.NOT_FOUND) return DataResponse.notFound("Given task doesn't exists already")

        return DataResponse.success(taskRepository.delete(name))
    }

    private suspend fun create(task: Task): DataResponse<Task> = taskRepository.create(task).takeIf { it == -1 }?.let{ DataResponse.databaseError("Unable to retrieve the id of the newly inserted task") }?: DataResponse.success(task, "Task created successfully")
}

public interface ITaskTableSeed
{
    fun seed()
}

class TaskTableSeedH2(private val connection: Connection) : ITaskTableSeed
{
    private var tasks = listOf(
        Task("Cleaning", "Clean the house", TaskCategory.Career, TaskStatus.TODO),
        Task("Gardening", "Mow the lawn", TaskCategory.Career,TaskStatus.TODO),
        Task("Shopping", "Buy the groceries", TaskCategory.Career,TaskStatus.TODO),
        Task("Painting", "Paint the fence", TaskCategory.Health,TaskStatus.COMPLETED),
        Task("Cooking", "Cook the dinner", TaskCategory.Social,TaskStatus.TODO),
        Task("Relaxing", "Take a walk", TaskCategory.Career,TaskStatus.TODO),
        Task("Exercising", "Go to the gym", TaskCategory.Social,TaskStatus.TODO),
        Task("Learning", "Read a book", TaskCategory.Career,TaskStatus.ACTIVE),
        Task("Snoozing", "Go for a nap", TaskCategory.Career,TaskStatus.TODO),
        Task("Socializing", "Go to a party", TaskCategory.Health,TaskStatus.TODO),
    )

    override fun seed()
    {
        val statement = connection.prepareStatement("INSERT INTO tasks (name, category, status, description) VALUES (?, ?, ?, ?)")

        tasks.forEach {
            statement.setString(1, it.name)
            statement.setInt(2, it.category.code)
            statement.setInt(3, it.status.code)
            statement.setString(4, it.description)
            statement.executeUpdate()
        }
    }
}

class TaskTableSeedPostgres(private val connection: Connection) : ITaskTableSeed
{
    override fun seed(){}
}