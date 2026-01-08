package com.example.todolist.repository

import com.example.todolist.domain.TodoTask

interface TodoTaskRepository {
    fun save(task: TodoTask): TodoTask
    fun findActiveTasks(): List<TodoTask>
    fun findAllTasks(): List<TodoTask>
    fun existsByName(name: String): Boolean
    fun findByName(name: String): TodoTask?
    fun deleteByName(name: String)
}
