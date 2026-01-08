package com.example.todolist.controller

import com.example.todolist.dto.TodoTaskRequest
import com.example.todolist.dto.TodoTaskResponse
import com.example.todolist.service.TodoTaskService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/tasks")
class TodoController(private val todoTaskService: TodoTaskService) {

    @PostMapping
    fun createTask(@RequestBody request: TodoTaskRequest): ResponseEntity<TodoTaskResponse> {
        val response = todoTaskService.createTask(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    fun getTasks(@RequestParam(defaultValue = "false") containsInactive: Boolean): ResponseEntity<List<TodoTaskResponse>> {
        val tasks = todoTaskService.getTasks(containsInactive)
        return ResponseEntity.ok(tasks)
    }

    @DeleteMapping("/{name}")
    fun deleteTask(@PathVariable name: String): ResponseEntity<Void> {
        todoTaskService.deleteTask(name)
        return ResponseEntity.noContent().build()
    }
}
