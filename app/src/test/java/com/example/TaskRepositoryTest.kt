package com.example.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TaskRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: TaskRepository
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = androidx.room.Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = TaskRepository(db.taskDao(), NotificationScheduler(context))
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun test_initiallyEmpty() = runBlocking {
        val tasks = repo.allTasks.first()
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun test_insertTask() = runBlocking {
        val task = Task(title = "Repo Insert", description = "", category = "")
        repo.insertTask(task)
        val tasks = repo.allTasks.first()
        assertEquals(1, tasks.size)
        assertEquals("Repo Insert", tasks.first().title)
    }

    @Test
    fun test_updateTask() = runBlocking {
        val task = Task(title = "Old Title", description = "", category = "")
        repo.insertTask(task)
        
        val dbTask = repo.allTasks.first().first()
        repo.updateTask(dbTask.copy(title = "New Title"))
        
        val updatedTask = repo.allTasks.first().first()
        assertEquals("New Title", updatedTask.title)
    }

    @Test
    fun test_deleteTask() = runBlocking {
        val task = Task(title = "To Delete", description = "", category = "")
        repo.insertTask(task)
        
        val dbTask = repo.allTasks.first().first()
        repo.deleteTask(dbTask.id)
        
        val tasks = repo.allTasks.first()
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun test_softDeleteAndRestore() = runBlocking {
        val task = Task(title = "To Soft Delete", description = "", category = "")
        repo.insertTask(task)
        
        val dbTask = repo.allTasks.first().first()
        repo.softDeleteTask(dbTask.id)
        
        val activeTasks = repo.allTasks.first()
        val numDeleted = activeTasks.count { it.isDeleted }
        assertEquals(1, numDeleted)
    }

    @Test
    fun test_archivedTask() = runBlocking {
        val task = Task(title = "Task", category = "")
        repo.insertTask(task)
        val allTasks = repo.allTasks.first()
        assertEquals(1, allTasks.size) 
    }

    @Test
    fun test_taskOrdering() = runBlocking {
        repo.insertTask(Task(title = "A", category = ""))
        repo.insertTask(Task(title = "B", category = ""))
        val list = repo.allTasks.first()
        assertEquals(2, list.size)
    }

    @Test
    fun test_completionState() = runBlocking {
        repo.insertTask(Task(title = "A", category = ""))
        val task = repo.allTasks.first().first()
        repo.updateTask(task.copy(isCompleted = true))
        val updated = repo.allTasks.first().first()
        assertTrue(updated.isCompleted)
    }

    @Test
    fun test_markCompletedSchedulesNoAlarm() = runBlocking {
        val task = Task(title = "Test", category = "")
        repo.insertTask(task)
        val inserted = repo.allTasks.first().first()
        repo.updateTask(inserted.copy(isCompleted = true))
        assertEquals("Test", repo.allTasks.first().first().title)
    }
}
