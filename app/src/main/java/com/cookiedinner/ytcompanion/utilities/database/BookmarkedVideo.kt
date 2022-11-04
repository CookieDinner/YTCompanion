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

    @ColumnInfo(name = "thumbnail", typeAffinity = ColumnInfo.BLOB)
    val thumbnail: ByteArray?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BookmarkedVideo

        if (id != other.id) return false
        if (title != other.title) return false
        if (channelName != other.channelName) return false
        if (videoUrl != other.videoUrl) return false
        if (!thumbnail.contentEquals(other.thumbnail)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + title.hashCode()
        result = 31 * result + channelName.hashCode()
        result = 31 * result + videoUrl.hashCode()
        result = 31 * result + thumbnail.contentHashCode()
        return result
    }
}

@Dao
interface BookmarkedVideoDao {
    @Query("SELECT * FROM bookmarked_videos")
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