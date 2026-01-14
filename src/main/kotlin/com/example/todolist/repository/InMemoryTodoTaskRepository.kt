package com.example.todolist.repository

import com.example.todolist.domain.TodoTask
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

@Repository
class InMemoryTodoTaskRepository : TodoTaskRepository {
    private val tasks = ConcurrentHashMap<String, TodoTask>()

    override fun save(task: TodoTask): TodoTask {
        tasks[task.name] = task
        return task
    }

    override fun findActiveTasks(): List<TodoTask> {
        val today = LocalDate.now()
        return tasks.values.filter { task ->
            task.endDate == null || task.endDate.isAfter(today)
        }
    }


    override fun findAllTasks(): List<TodoTask> {
        return tasks.values.toList()
    }

    override fun existsByName(name: String): Boolean {
        return tasks.containsKey(name)
    }

    override fun findByName(name: String): TodoTask? {
        return tasks[name]
    }

    override fun deleteByName(name: String) {
        tasks.remove(name)
    }
}
