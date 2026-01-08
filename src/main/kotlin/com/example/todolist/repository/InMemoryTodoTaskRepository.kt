package com.example.todolist.repository

import com.example.todolist.domain.TodoTask
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class InMemoryTodoTaskRepository : TodoTaskRepository {
    private val tasks = mutableListOf<TodoTask>()

    override fun save(task: TodoTask): TodoTask {
        tasks.add(task)
        return task
    }

    override fun findActiveTasks(): List<TodoTask> {
        val today = LocalDate.now()
        return tasks.filter { task ->
            task.endDate == null || task.endDate.isAfter(today)
        }
    }


    override fun findAllTasks(): List<TodoTask> {
        return tasks.toList()
    }

    override fun existsByName(name: String): Boolean {
        return tasks.any { it.name == name }
    }

    override fun findByName(name: String): TodoTask? {
        return tasks.find { it.name == name }
    }

    override fun deleteByName(name: String) {
        tasks.removeIf { it.name == name }
    }
}
