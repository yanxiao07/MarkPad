package com.markpad.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM file_metadata ORDER BY lastOpened DESC LIMIT 20")
    fun getRecentFiles(): Flow<List<FileMetadata>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(file: FileMetadata)

    @Delete
    suspend fun delete(file: FileMetadata)

    @Query("SELECT * FROM file_metadata WHERE path = :path")
    suspend fun getByPath(path: String): FileMetadata?
}
