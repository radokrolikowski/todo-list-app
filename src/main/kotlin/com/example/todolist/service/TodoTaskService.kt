package com.example.todolist.service

import com.example.todolist.dto.TodoTaskRequest
import com.example.todolist.dto.TodoTaskResponse
import com.example.todolist.dto.toDomain
import com.example.todolist.dto.toResponse
import com.example.todolist.exception.InvalidDateException
import com.example.todolist.repository.TodoTaskRepository
import com.example.todolist.util.DateFormatters
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TodoTaskService(
    private val repository: TodoTaskRepository,
    @Value("\${todo.max-end-date}") private val maxEndDate: String,
) {

    fun createTask(request: TodoTaskRequest): TodoTaskResponse {
        require(request.name.isNotBlank()) { "Task name cannot be empty" }
        require(!repository.existsByName(request.name)) { "Task with name '${request.name}' already exists" }

        val endDate = request.endDate?.let { validateAndParseDate(it) }
        val task = request.toDomain(endDate)
        val savedTask = repository.save(task)
        return savedTask.toResponse()
    }

    fun getTasks(containsInactive: Boolean): List<TodoTaskResponse> {
        return when (containsInactive) {
            false -> repository.findActiveTasks()
            true -> repository.findAllTasks()
        }.map { it.toResponse() }
    }

    fun deleteTask(name: String) {
        require(name.isNotBlank()) { "Task name cannot be empty" }

        repository.findByName(name)
            ?: throw IllegalArgumentException("Task with name '$name' not found")

        repository.deleteByName(name)
    }

    private fun validateAndParseDate(dateString: String): LocalDate {
        val date = runCatching { LocalDate.parse(dateString, DateFormatters.ISO_DATE) }
            .getOrElse { throw InvalidDateException("Date must be in yyyy-MM-dd format") }

        val minDate = LocalDate.now().plusDays(1)
        if (date.isBefore(minDate)) {
            throw InvalidDateException("Task end date must be at least 1 day from now")
        }

        val maxDate = LocalDate.parse(maxEndDate, DateFormatters.ISO_DATE)
        if (date.isAfter(maxDate)) {
            throw InvalidDateException("Task end date cannot be later than $maxEndDate")
        }

        return date
    }
}
