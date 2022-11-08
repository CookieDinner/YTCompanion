package com.cookiedinner.ytcompanion.utilities.database

import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.delay

@Entity(tableName = "bookmarked_videos")
data class BookmarkedVideo(
    @PrimaryKey(autoGenerate = true)
    val id: Int,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "channel_name")
    val channelName: String,

    @ColumnInfo(name = "video_url")
    val videoUrl: String,

    @ColumnInfo(name = "thumbnail")
    val thumbnail: String,

    @ColumnInfo(name = "modification_date")
    val modificationDate: String
)

@Dao
interface BookmarkedVideoDao {
    @Query("SELECT * FROM bookmarked_videos ORDER BY modification_date DESC")
    suspend fun getAll(): List<BookmarkedVideo>

    @Query("SELECT * FROM bookmarked_videos WHERE id = :id")
    suspend fun findById(id: Long): BookmarkedVideo

    @Query("SELECT * FROM bookmarked_videos WHERE title LIKE :searchQuery OR channel_name LIKE :searchQuery")
    suspend fun findByTitleOrChannelName(searchQuery: String): List<BookmarkedVideo>

    @Query("DELETE FROM bookmarked_videos WHERE id = :id")
    suspend fun deleteById(id: Int) : Int

    @Insert
    suspend fun insertAll(vararg bookmarkedVideos: BookmarkedVideo): List<Long>

    @Delete
    suspend fun delete(bookmarkedVideo: BookmarkedVideo): Int
}