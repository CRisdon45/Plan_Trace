package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val backgroundType: String, // "SAMPLE", "IMAGE_URI", "PDF_URI", "BLANK_GRID"
    val backgroundResourceOrUri: String,
    val backgroundOpacity: Float,
    val isBackgroundLocked: Boolean,
    val pdfPageNumber: Int,
    val pdfTotalPages: Int,
    val isCalibrated: Boolean,
    val pixelDistance: Float,
    val realWorldUnits: Float,
    val unit: String,
    val layersJson: String,
    val activeLayerId: String,
    val elementsJson: String,
    @ColumnInfo(defaultValue = "'{}'") val pageDrawingsJson: String = "{}"
)
