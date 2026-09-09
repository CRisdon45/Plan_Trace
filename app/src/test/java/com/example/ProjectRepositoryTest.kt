package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ProjectRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ProjectRepositoryTest {

    @Test
    fun `startup resumes most recently edited project without creating a duplicate`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase("plantrace_db")

        val repository = ProjectRepository(context)
        val first = repository.getOrCreateInitialProject()
        val edited = first.copy(
            title = "Field sketch that must survive restart",
            updatedAt = System.currentTimeMillis() + 1_000L
        )
        repository.saveProject(edited)

        val reopened = repository.getOrCreateInitialProject()
        assertEquals(first.id, reopened.id)
        assertEquals(edited.title, reopened.title)

        val savedProjects = repository.projectsFlow.first()
        assertEquals(1, savedProjects.size)
        assertEquals(first.id, savedProjects.single().id)
    }
}
