package com.example.todolist.dto

import com.example.todolist.domain.TodoTask
import com.example.todolist.util.DateFormatters

data class TodoTaskResponse(
    val id: String,
    val name: String,
    val endDate: String?
)

fun TodoTask.toResponse(): TodoTaskResponse {
    return TodoTaskResponse(
        id = this.id,
        name = this.name,
        endDate = this.endDate?.format(DateFormatters.ISO_DATE)
    )
}
