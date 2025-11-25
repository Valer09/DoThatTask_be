package homeaq.dothattask.data

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskCategory
import homeaq.dothattask.Model.TaskStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection

class TaskService(private val taskRepository: TaskRepository)
{

    suspend fun all(): List<Task>
    {
        return taskRepository.allTasks()
    }


    suspend fun create(task: Task): Int
    {
        return taskRepository.create(task)
    }

    suspend fun read(id: Int): Task?
    {
        return taskRepository.taskById(id)
    }

    suspend fun addOrUpdate(task: Task)
    {
        if(taskRepository.taskByName(task.name) != null)
            taskRepository.update(task)
        else taskRepository.create(task)
    }

    suspend fun delete(id: Int) = withContext(Dispatchers.IO)
    {
        taskRepository.delete(id)
    }
}

public interface ITaskTableSeed
{
    fun seed()
}

class TaskTableSeedH2(private val connection: Connection) : ITaskTableSeed
{
    private var tasks = listOf(
        Task(1,"Cleaning", "Clean the house", TaskCategory.Career, TaskStatus.TODO),
        Task(2,"Gardening", "Mow the lawn", TaskCategory.Career,TaskStatus.TODO),
        Task(3,"Shopping", "Buy the groceries", TaskCategory.Career,TaskStatus.TODO),
        Task(4,"Painting", "Paint the fence", TaskCategory.Health,TaskStatus.COMPLETED),
        Task(5,"Cooking", "Cook the dinner", TaskCategory.Social,TaskStatus.TODO),
        Task(6,"Relaxing", "Take a walk", TaskCategory.Career,TaskStatus.TODO),
        Task(7,"Exercising", "Go to the gym", TaskCategory.Social,TaskStatus.TODO),
        Task(8,"Learning", "Read a book", TaskCategory.Career,TaskStatus.ACTIVE),
        Task(9,"Snoozing", "Go for a nap", TaskCategory.Career,TaskStatus.TODO),
        Task(10,"Socializing", "Go to a party", TaskCategory.Health,TaskStatus.TODO),
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