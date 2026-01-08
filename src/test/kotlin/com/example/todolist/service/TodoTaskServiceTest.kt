package com.example.todolist.service

import com.example.todolist.domain.TodoTask
import com.example.todolist.dto.TodoTaskRequest
import com.example.todolist.exception.InvalidDateException
import com.example.todolist.repository.TodoTaskRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class TodoTaskServiceTest {

    private lateinit var repository: TodoTaskRepository
    private lateinit var service: TodoTaskService
    private val maxEndDate = "2026-12-01"

    @BeforeEach
    fun setup() {
        repository = mockk()
        service = TodoTaskService(repository, maxEndDate)
    }

    @Nested
    inner class CreateTaskTests {

        @Test
        fun `creates task without end date`() {
            // given
            val request = TodoTaskRequest(name = "Task")

            every { repository.existsByName("Task") } returns false
            every { repository.save(any()) } returnsArgument 0

            // when
            val response = service.createTask(request)

            // then
            verify(exactly = 1) { repository.existsByName("Task") }
            verify(exactly = 1) { repository.save(any()) }
            assertEquals("Task", response.name)
            assertNull(response.endDate)
            assertNotNull(response.id)
        }

        @Test
        fun `creates task with valid end date`() {
            // given
            val futureDate = LocalDate.now().plusDays(5).toString()
            val request = TodoTaskRequest(name = "Task", endDate = futureDate)

            every { repository.existsByName("Task") } returns false
            every { repository.save(any()) } returnsArgument 0

            // when
            val response = service.createTask(request)

            // then
            verify(exactly = 1) { repository.existsByName("Task") }
            verify(exactly = 1) { repository.save(any()) }
            assertEquals("Task", response.name)
            assertEquals(futureDate, response.endDate)
            assertNotNull(response.endDate)
        }

        @Test
        fun `throws exception when end date is too early`() {
            // given
            val tooEarlyDate = LocalDate.now().toString()
            val request = TodoTaskRequest(name = "Task", endDate = tooEarlyDate)

            every { repository.existsByName("Task") } returns false

            // when & then
            val exception = assertThrows<InvalidDateException> {
                service.createTask(request)
            }

            assertTrue(exception.message!!.contains("at least 1 day"))
        }

        @Test
        fun `throws exception when task name already exists`() {
            // given
            val request = TodoTaskRequest(name = "Task")

            every { repository.existsByName("Task") } returns true

            // when & then
            val exception = assertThrows<IllegalArgumentException> {
                service.createTask(request)
            }

            assertTrue(exception.message!!.contains("already exists"))
            verify(exactly = 0) { repository.save(any()) }
        }

        @Test
        fun `throws exception when end date is in the past`() {
            // given
            val pastDate = LocalDate.now().minusDays(1).toString()
            val request = TodoTaskRequest(name = "Task", endDate = pastDate)

            every { repository.existsByName("Task") } returns false

            // when & then
            val exception = assertThrows<InvalidDateException> {
                service.createTask(request)
            }

            assertTrue(exception.message!!.contains("at least 1 day"))
        }

        @Test
        fun `throws exception when end date exceeds maximum date`() {
            // given
            val tooLateDate = "2027-01-01"
            val request = TodoTaskRequest(name = "Task", endDate = tooLateDate)

            every { repository.existsByName("Task") } returns false

            // when & then
            val exception = assertThrows<InvalidDateException> {
                service.createTask(request)
            }

            assertTrue(exception.message!!.contains("cannot be later than $maxEndDate"))
        }

        @Test
        fun `throws exception for invalid date format`() {
            // given
            val invalidDate = "01-12-2026"
            val request = TodoTaskRequest(name = "Task", endDate = invalidDate)

            every { repository.existsByName("Task") } returns false

            // when & then
            val exception = assertThrows<InvalidDateException> {
                service.createTask(request)
            }

            assertTrue(exception.message!!.contains("yyyy-MM-dd"))
        }

        @Test
        fun `throws exception for invalid date format with slashes`() {
            // given
            val invalidDate = "2026/12/01"
            val request = TodoTaskRequest(name = "Task", endDate = invalidDate)

            every { repository.existsByName("Task") } returns false

            // when & then
            val exception = assertThrows<InvalidDateException> {
                service.createTask(request)
            }

            assertTrue(exception.message!!.contains("yyyy-MM-dd"))
        }

        @Test
        fun `accepts date exactly at maximum date boundary`() {
            // given
            val request = TodoTaskRequest(name = "Task", endDate = maxEndDate)

            every { repository.existsByName("Task") } returns false
            every { repository.save(any()) } returnsArgument 0

            // when
            val response = service.createTask(request)

            // then
            verify(exactly = 1) { repository.save(any()) }
            assertEquals(maxEndDate, response.endDate)
        }
    }

    @Nested
    inner class GetTasksTests {

        @Test
        fun `returns only active tasks when containsInactive is false`() {
            // given
            val activeTasks = listOf(
                TodoTask(id = "1", name = "Task 1", endDate = LocalDate.now().plusDays(5)),
                TodoTask(id = "2", name = "Task 2", endDate = null),
                TodoTask(id = "3", name = "Task 3", endDate = LocalDate.now().plusDays(10))
            )

            every { repository.findActiveTasks() } returns activeTasks

            // when
            val response = service.getTasks(containsInactive = false)

            // then
            verify(exactly = 1) { repository.findActiveTasks() }
            assertEquals(3, response.size)
            assertEquals("Task 1", response[0].name)
            assertEquals("Task 2", response[1].name)
            assertEquals("Task 3", response[2].name)
        }

        @Test
        fun `returns empty list when no active tasks exist`() {
            // given
            every { repository.findActiveTasks() } returns emptyList()

            // when
            val response = service.getTasks(containsInactive = false)

            // then
            verify(exactly = 1) { repository.findActiveTasks() }
            assertTrue(response.isEmpty())
        }

        @Test
        fun `returns all tasks including inactive when containsInactive is true`() {
            // given
            val allTasks = listOf(
                TodoTask(id = "1", name = "Task 1", endDate = LocalDate.now().plusDays(5)),
                TodoTask(id = "2", name = "Task 2", endDate = null),
                TodoTask(id = "3", name = "Task 3", endDate = LocalDate.now().minusDays(1))
            )

            every { repository.findAllTasks() } returns allTasks

            // when
            val response = service.getTasks(containsInactive = true)

            // then
            verify(exactly = 1) { repository.findAllTasks() }
            assertEquals(3, response.size)
            assertEquals("Task 1", response[0].name)
            assertEquals("Task 2", response[1].name)
            assertEquals("Task 3", response[2].name)
        }
    }

    @Nested
    inner class DeleteTaskTests {

        @Test
        fun `deletes task successfully`() {
            // given
            val taskName = "Task"
            val task = TodoTask(name = taskName)

            every { repository.findByName(taskName) } returns task
            every { repository.deleteByName(taskName) } returns Unit

            // when
            service.deleteTask(taskName)

            // then
            verify(exactly = 1) { repository.findByName(taskName) }
            verify(exactly = 1) { repository.deleteByName(taskName) }
        }

        @Test
        fun `throws exception when deleting task with empty name`() {
            // given
            val emptyName = "   "

            // when & then
            val exception = assertThrows<IllegalArgumentException> {
                service.deleteTask(emptyName)
            }

            assertTrue(exception.message!!.contains("cannot be empty"))
            verify(exactly = 0) { repository.findByName(any()) }
            verify(exactly = 0) { repository.deleteByName(any()) }
        }

        @Test
        fun `throws exception when deleting non-existing task`() {
            // given
            val taskName = "Task that does not exist"

            every { repository.findByName(taskName) } returns null

            // when & then
            val exception = assertThrows<IllegalArgumentException> {
                service.deleteTask(taskName)
            }

            assertTrue(exception.message!!.contains("not found"))
            verify(exactly = 1) { repository.findByName(taskName) }
            verify(exactly = 0) { repository.deleteByName(any()) }
        }
    }
}
