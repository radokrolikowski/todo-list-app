package com.example.todolist.repository

import com.example.todolist.domain.TodoTask
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class TodoTaskRepositoryTest {

    private lateinit var repository: TodoTaskRepository

    @BeforeEach
    fun setup() {
        repository = InMemoryTodoTaskRepository()
    }

    @Nested
    inner class SaveTests {

        @Test
        fun `should save task successfully`() {
            val task = TodoTask(name = "Task")

            val savedTask = repository.save(task)

            assertEquals("Task", savedTask.name)
            assertTrue(repository.existsByName("Task"))
        }

        @Test
        fun `should save task with end date`() {
            val endDate = LocalDate.now().plusDays(5)
            val task = TodoTask(name = "Task", endDate = endDate)

            val savedTask = repository.save(task)

            assertEquals("Task", savedTask.name)
            assertEquals(endDate, savedTask.endDate)
        }

        @Test
        fun `should update task when saving with same name`() {
            val task1 = TodoTask(id = "1", name = "Task", endDate = LocalDate.now().plusDays(5))
            val task2 = TodoTask(id = "2", name = "Task", endDate = LocalDate.now().plusDays(10))

            repository.save(task1)
            repository.save(task2)

            val found = repository.findByName("Task")
            assertNotNull(found)
            assertEquals("2", found?.id)
            assertEquals(LocalDate.now().plusDays(10), found?.endDate)
        }
    }

    @Nested
    inner class FindActiveTasksTests {

        @Test
        fun `should return tasks without end date as active`() {
            val task1 = TodoTask(name = "Task 1")
            val task2 = TodoTask(name = "Task 2")

            repository.save(task1)
            repository.save(task2)

            val activeTasks = repository.findActiveTasks()

            assertEquals(2, activeTasks.size)
        }

        @Test
        fun `should return tasks with future end date as active`() {
            val task = TodoTask(name = "Task", endDate = LocalDate.now().plusDays(5))

            repository.save(task)

            val activeTasks = repository.findActiveTasks()

            assertEquals(1, activeTasks.size)
            assertEquals("Task", activeTasks[0].name)
        }

        @Test
        fun `should not return tasks with past end date as active`() {
            val task = TodoTask(name = "Task", endDate = LocalDate.now().minusDays(1))

            repository.save(task)

            val activeTasks = repository.findActiveTasks()

            assertTrue(activeTasks.isEmpty())
        }

        @Test
        fun `should not return tasks with today's date as active`() {
            val task = TodoTask(name = "Task", endDate = LocalDate.now())

            repository.save(task)

            val activeTasks = repository.findActiveTasks()

            assertTrue(activeTasks.isEmpty())
        }

        @Test
        fun `should return mixed active tasks correctly`() {
            repository.save(TodoTask(name = "No Date Task"))
            repository.save(TodoTask(name = "Future Task", endDate = LocalDate.now().plusDays(5)))
            repository.save(TodoTask(name = "Past Task", endDate = LocalDate.now().minusDays(1)))
            repository.save(TodoTask(name = "Today Task", endDate = LocalDate.now()))

            val activeTasks = repository.findActiveTasks()

            assertEquals(2, activeTasks.size)
            assertTrue(activeTasks.any { it.name == "No Date Task" })
            assertTrue(activeTasks.any { it.name == "Future Task" })
        }
    }

    @Nested
    inner class FindAllTasksTests {

        @Test
        fun `should return all tasks including inactive`() {
            repository.save(TodoTask(name = "Active Task", endDate = LocalDate.now().plusDays(5)))
            repository.save(TodoTask(name = "Inactive Task", endDate = LocalDate.now().minusDays(1)))
            repository.save(TodoTask(name = "No Date Task"))

            val allTasks = repository.findAllTasks()

            assertEquals(3, allTasks.size)
        }

        @Test
        fun `should return empty list when no tasks exist`() {
            val allTasks = repository.findAllTasks()

            assertTrue(allTasks.isEmpty())
        }
    }

    @Nested
    inner class ExistsByNameTests {

        @Test
        fun `should return true when task exists`() {
            repository.save(TodoTask(name = "Existing Task"))

            assertTrue(repository.existsByName("Existing Task"))
        }

        @Test
        fun `should return false when task does not exist`() {
            assertFalse(repository.existsByName("Non-existing Task"))
        }
    }

    @Nested
    inner class FindByNameTests {

        @Test
        fun `should find task by name`() {
            val task = TodoTask(name = "Task", endDate = LocalDate.now().plusDays(5))
            repository.save(task)

            val todoTask = repository.findByName("Task")

            assertNotNull(todoTask)
            assertEquals("Task", todoTask?.name)
            assertEquals(LocalDate.now().plusDays(5), todoTask?.endDate)
        }

        @Test
        fun `should return null when task not found`() {
            val todoTask = repository.findByName("Not Found")

            assertNull(todoTask)
        }
    }

    @Nested
    inner class DeleteByNameTests {

        @Test
        fun `should delete task by name`() {
            repository.save(TodoTask(name = "Delete Me"))

            repository.deleteByName("Delete Me")

            assertFalse(repository.existsByName("Delete Me"))
        }

        @Test
        fun `should not throw exception when deleting non-existing task`() {
            assertDoesNotThrow {
                repository.deleteByName("Non-existing Task")
            }
        }

        @Test
        fun `should delete only specified task`() {
            repository.save(TodoTask(name = "Task 1"))
            repository.save(TodoTask(name = "Task 2"))
            repository.save(TodoTask(name = "Task 3"))

            repository.deleteByName("Task 2")

            assertTrue(repository.existsByName("Task 1"))
            assertFalse(repository.existsByName("Task 2"))
            assertTrue(repository.existsByName("Task 3"))
        }
    }

    @Nested
    inner class ConcurrencyTests {

        @Test
        fun `should handle concurrent saves safely`() {
            val threadCount = 100
            val executor = Executors.newFixedThreadPool(10)
            val latch = CountDownLatch(threadCount)

            repeat(threadCount) { i ->
                executor.submit {
                    try {
                        repository.save(TodoTask(name = "Task $i"))
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(10, TimeUnit.SECONDS)
            executor.shutdown()

            val allTasks = repository.findAllTasks()
            assertEquals(threadCount, allTasks.size)
        }

        @Test
        fun `should handle concurrent reads and writes safely`() {
            val threadCount = 50
            val executor = Executors.newFixedThreadPool(10)
            val latch = CountDownLatch(threadCount * 2)
            val exceptions = AtomicInteger(0)

            // Writing threads
            repeat(threadCount) { i ->
                executor.submit {
                    try {
                        repository.save(TodoTask(name = "Task $i"))
                    } catch (e: Exception) {
                        exceptions.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            // Reading threads
            repeat(threadCount) {
                executor.submit {
                    try {
                        repository.findAllTasks()
                        repository.findActiveTasks()
                    } catch (e: Exception) {
                        exceptions.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(10, TimeUnit.SECONDS)
            executor.shutdown()

            assertEquals(0, exceptions.get(), "No exceptions should occur during concurrent operations")
            assertEquals(threadCount, repository.findAllTasks().size)
        }

        @Test
        fun `should handle concurrent updates to same task`() {
            val taskName = "Concurrent Task"
            val threadCount = 100
            val executor = Executors.newFixedThreadPool(10)
            val latch = CountDownLatch(threadCount)

            repeat(threadCount) { i ->
                executor.submit {
                    try {
                        repository.save(TodoTask(id = "id-$i", name = taskName, endDate = LocalDate.now().plusDays(i.toLong())))
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(10, TimeUnit.SECONDS)
            executor.shutdown()

            // Should be only one task with this name (last one saved)
            val allTasks = repository.findAllTasks()
            val tasksWithName = allTasks.filter { it.name == taskName }
            assertEquals(1, tasksWithName.size)
            assertNotNull(repository.findByName(taskName))
        }

        @Test
        fun `should handle concurrent deletes safely`() {
            // Prepare data
            repeat(100) { i ->
                repository.save(TodoTask(name = "Task $i"))
            }

            val threadCount = 50
            val executor = Executors.newFixedThreadPool(10)
            val latch = CountDownLatch(threadCount)

            repeat(threadCount) { i ->
                executor.submit {
                    try {
                        repository.deleteByName("Task $i")
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(10, TimeUnit.SECONDS)
            executor.shutdown()

            val remainingTasks = repository.findAllTasks()
            assertEquals(50, remainingTasks.size)
        }

        @Test
        fun `should handle concurrent mixed operations safely`() {
            val operationsPerType = 30
            val executor = Executors.newFixedThreadPool(10)
            val latch = CountDownLatch(operationsPerType * 4)
            val exceptions = AtomicInteger(0)

            // Write operations
            repeat(operationsPerType) { i ->
                executor.submit {
                    try {
                        repository.save(TodoTask(name = "SaveTask $i"))
                    } catch (e: Exception) {
                        exceptions.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            // Read all operations
            repeat(operationsPerType) {
                executor.submit {
                    try {
                        repository.findAllTasks()
                    } catch (e: Exception) {
                        exceptions.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            // Read active operations
            repeat(operationsPerType) {
                executor.submit {
                    try {
                        repository.findActiveTasks()
                    } catch (e: Exception) {
                        exceptions.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            // Check existence operations
            repeat(operationsPerType) { i ->
                executor.submit {
                    try {
                        repository.existsByName("SaveTask $i")
                    } catch (e: Exception) {
                        exceptions.incrementAndGet()
                    } finally {
                        latch.countDown()
                    }
                }
            }

            latch.await(15, TimeUnit.SECONDS)
            executor.shutdown()

            assertEquals(0, exceptions.get(), "No exceptions should occur during concurrent mixed operations")
            assertTrue(repository.findAllTasks().isNotEmpty())
        }
    }
}

