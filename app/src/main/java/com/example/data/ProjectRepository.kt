package com.example.data

import android.content.Context
import com.example.data.db.ProjectDao
import com.example.data.db.TraceDatabase
import com.example.model.BackgroundType
import com.example.model.ScaleCalibration
import com.example.model.TraceProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProjectRepository(private val context: Context) {
    private val dao: ProjectDao = TraceDatabase.getInstance(context).projectDao()

    val projectsFlow: Flow<List<TraceProject>> = dao.getAllProjects().map { list ->
        list.map { ProjectJsonConverter.fromEntity(it) }
    }

    suspend fun getProject(id: String): TraceProject? = withContext(Dispatchers.IO) {
        val entity = dao.getProjectById(id)
        entity?.let { ProjectJsonConverter.fromEntity(it) }
    }

    suspend fun saveProject(project: TraceProject) = withContext(Dispatchers.IO) {
        dao.insertOrUpdate(ProjectJsonConverter.toEntity(project))
    }

    suspend fun deleteProject(id: String) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    suspend fun getOrCreateInitialProject(): TraceProject = withContext(Dispatchers.IO) {
        val recent = dao.getMostRecentProject()
        if (recent != null) {
            // Saving a project updates updatedAt, so this naturally resumes the sketch the user
            // most recently worked on. The previous implementation looked for the literal id
            // "default" even though projects use UUIDs, which created another project on every
            // process restart instead of reopening the existing drawing.
            return@withContext ProjectJsonConverter.fromEntity(recent)
        }

        // First launch only: seed a useful plan so the drawing surface is immediately testable.
        val initial = TraceProject(
            title = "Dormal Pool & Patio Plan",
            backgroundType = BackgroundType.SAMPLE,
            backgroundResourceOrUri = "sample_pool",
            scaleCalibration = ScaleCalibration(
                isCalibrated = true,
                pixelDistance = 240f,
                realWorldUnits = 20f,
                unit = "ft"
            )
        )
        saveProject(initial)
        initial
    }
}
