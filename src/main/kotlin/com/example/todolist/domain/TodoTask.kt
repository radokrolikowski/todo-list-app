package com.example.todolist.domain

import java.time.LocalDate
import java.util.UUID

data class TodoTask(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val endDate: LocalDate? = null
)
