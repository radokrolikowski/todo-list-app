package com.example.todolist.dto

import com.example.todolist.domain.TodoTask
import java.time.LocalDate

data class TodoTaskRequest(
    val name: String,
    val endDate: String? = null
)

fun TodoTaskRequest.toDomain(parsedEndDate: LocalDate?): TodoTask {
    return TodoTask(
        name = this.name,
        endDate = parsedEndDate
    )
}
