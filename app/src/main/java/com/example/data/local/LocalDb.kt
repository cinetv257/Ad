package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.data.model.User
import com.example.data.model.VideoProject
import com.example.data.model.Clip

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserById(id: Int): Flow<User?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    @Update
    suspend fun updateUser(user: User)
}

@Dao
interface VideoProjectDao {
    @Query("SELECT * FROM video_projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<VideoProject>>

    @Query("SELECT * FROM video_projects WHERE id = :id LIMIT 1")
    fun getProjectById(id: Int): Flow<VideoProject?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: VideoProject): Long

    @Delete
    suspend fun deleteProject(project: VideoProject)

    @Query("DELETE FROM video_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)
}

@Dao
interface ClipDao {
    @Query("SELECT * FROM clips WHERE projectId = :projectId ORDER BY engagementRate DESC")
    fun getClipsForProject(projectId: Int): Flow<List<Clip>>

    @Query("SELECT * FROM clips WHERE id = :id LIMIT 1")
    fun getClipById(id: Int): Flow<Clip?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClip(clip: Clip): Long

    @Update
    suspend fun updateClip(clip: Clip)

    @Delete
    suspend fun deleteClip(clip: Clip)
}

@Database(entities = [User::class, VideoProject::class, Clip::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun videoProjectDao(): VideoProjectDao
    abstract fun clipDao(): ClipDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "clip_forge_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
