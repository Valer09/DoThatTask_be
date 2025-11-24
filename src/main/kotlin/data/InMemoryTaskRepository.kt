package homeaq.dothattask.data

import homeaq.dothattask.Model.Task
import homeaq.dothattask.Model.TaskStatus
import org.jetbrains.exposed.sql.not

class InMemoryTaskRepository: TaskRepository
{
    private var tasks = listOf(
        Task("Cleaning", "Clean the house", TaskStatus.TODO),
        Task("Gardening", "Mow the lawn", TaskStatus.TODO),
        Task("Shopping", "Buy the groceries", TaskStatus.TODO),
        Task("Painting", "Paint the fence", TaskStatus.COMPLETED),
        Task("Cooking", "Cook the dinner", TaskStatus.TODO),
        Task("Relaxing", "Take a walk", TaskStatus.TODO),
        Task("Exercising", "Go to the gym", TaskStatus.TODO),
        Task("Learning", "Read a book", TaskStatus.ACTIVE),
        Task("Snoozing", "Go for a nap", TaskStatus.TODO),
        Task("Socializing", "Go to a party", TaskStatus.TODO),
    )

    override fun allTasks(): List<Task> = tasks

    override fun taskByName(name: String): Task? = tasks.find { it.name.equals(name, ignoreCase = true)  }

    override fun addOrUpdateTask(task: Task)
    {
        var notFound = true

        tasks = tasks.map {
            if (it.name == task.name)
            {
                notFound = false
                task
            } else it
        }
        if (notFound) tasks = tasks.plus(task)
    }

    override fun removeTask(name: String): Boolean
    {
        val oldTasks = tasks
        tasks = tasks.filterNot { it.name == name }
        return oldTasks.size > tasks.size
    }

}