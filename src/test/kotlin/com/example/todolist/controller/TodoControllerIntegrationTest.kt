package com.example.todolist.controller

import com.example.todolist.dto.TodoTaskRequest
import com.example.todolist.repository.TodoTaskRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
class TodoControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var repository: TodoTaskRepository

    @BeforeEach
    fun cleanup() {
        // Clean up repository before each test
        repository.findAllTasks().forEach {
            repository.deleteByName(it.name)
        }
    }

    @Nested
    inner class CreateTaskIntegrationTests {

        @Test
        fun `should create task without end date`() {
            val request = TodoTaskRequest(name = "Integration Task")

            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.name").value("Integration Task"))
                .andExpect(jsonPath("$.endDate").doesNotExist())
                .andExpect(jsonPath("$.id").exists())
        }

        @Test
        fun `should create task with valid end date`() {
            val futureDate = LocalDate.now().plusDays(10).toString()
            val request = TodoTaskRequest(name = "Task with date", endDate = futureDate)

            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.name").value("Task with date"))
                .andExpect(jsonPath("$.endDate").value(futureDate))
                .andExpect(jsonPath("$.id").exists())
        }

        @Test
        fun `should return 400 when task name already exists`() {
            val request = TodoTaskRequest(name = "Duplicate Task")

            // Create first task
            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)

            // Try to create second task with the same name
            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").value("Task with name 'Duplicate Task' already exists"))
        }

        @Test
        fun `should return 400 when end date is in the past`() {
            val pastDate = LocalDate.now().minusDays(1).toString()
            val request = TodoTaskRequest(name = "Past Task", endDate = pastDate)

            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").exists())
        }

        @Test
        fun `should return 400 when end date exceeds maximum date`() {
            val tooLateDate = "2027-01-01"
            val request = TodoTaskRequest(name = "Far Future Task", endDate = tooLateDate)

            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").exists())
        }

        @Test
        fun `should return 400 for invalid date format`() {
            val invalidDate = "01-12-2026"
            val request = TodoTaskRequest(name = "Invalid Date Task", endDate = invalidDate)

            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").exists())
        }
    }

    @Nested
    inner class GetTasksIntegrationTests {

        @Test
        fun `should return empty list when no tasks exist`() {
            mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$").isEmpty)
        }

        @Test
        fun `should return only active tasks by default`() {
            // Create active task
            val activeRequest = TodoTaskRequest(name = "Active Task", endDate = LocalDate.now().plusDays(5).toString())
            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(activeRequest))
            )

            // Create task without end date
            val noDateRequest = TodoTaskRequest(name = "No Date Task")
            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(noDateRequest))
            )

            // Get only active tasks
            mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(2))
        }

        @Test
        fun `should return all tasks including inactive when containsInactive is true`() {
            // Create active task
            val activeRequest = TodoTaskRequest(name = "Active Task", endDate = LocalDate.now().plusDays(5).toString())
            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(activeRequest))
            )

            mockMvc.perform(get("/api/tasks")
                .param("containsInactive", "true"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isArray)
                .andExpect(jsonPath("$.length()").value(1))
        }

        @Test
        fun `should return multiple tasks in order of creation`() {
            val task1 = TodoTaskRequest(name = "Task 1")
            val task2 = TodoTaskRequest(name = "Task 2")
            val task3 = TodoTaskRequest(name = "Task 3")

            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(task1))
            )
            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(task2))
            )
            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(task3))
            )

            mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[*].name").exists())
        }
    }

    @Nested
    inner class DeleteTaskIntegrationTests {

        @Test
        fun `should delete existing task`() {
            // Create task
            val request = TodoTaskRequest(name = "Task to Delete")
            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )

            // Delete task
            mockMvc.perform(delete("/api/tasks/Task to Delete"))
                .andExpect(status().isNoContent)

            // Verify task was deleted
            mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$").isEmpty)
        }

        @Test
        fun `should return 400 when deleting non-existing task`() {
            mockMvc.perform(delete("/api/tasks/NonExistingTask"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.message").exists())
        }

        @Test
        fun `should handle URL encoded task names correctly`() {
            // Create task with name containing spaces
            val request = TodoTaskRequest(name = "Task With Spaces")
            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )

            // Delete task using URL-encoded name
            mockMvc.perform(delete("/api/tasks/Task With Spaces"))
                .andExpect(status().isNoContent)
        }
    }

    @Nested
    inner class EndToEndScenarioTests {

        @Test
        fun `should handle complete lifecycle of tasks`() {
            // Create several tasks
            val task1 = TodoTaskRequest(name = "Shopping", endDate = LocalDate.now().plusDays(3).toString())
            val task2 = TodoTaskRequest(name = "Workout")
            val task3 = TodoTaskRequest(name = "Meeting", endDate = LocalDate.now().plusDays(7).toString())

            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(task1))
            ).andExpect(status().isCreated)

            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(task2))
            ).andExpect(status().isCreated)

            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(task3))
            ).andExpect(status().isCreated)

            // Get all tasks
            mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(3))

            // Delete one task
            mockMvc.perform(delete("/api/tasks/Workout"))
                .andExpect(status().isNoContent)

            // Verify 2 tasks remain
            mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.length()").value(2))

            // Try to create duplicate
            mockMvc.perform(
                post("/api/tasks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(task1))
            ).andExpect(status().isBadRequest)
        }
    }
}

