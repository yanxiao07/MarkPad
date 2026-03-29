package com.markpad.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "file_metadata")
data class FileMetadata(
    @PrimaryKey val path: String,
    val name: String,
    val lastModified: Long,
    val isFavorite: Boolean = false,
    val lastOpened: Long = System.currentTimeMillis()
)
