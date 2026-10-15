package com.example.engine.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val lastModifiedTimestamp: Long,
    val sceneJson: String,
    val entityCount: Int,
    val targetPlatform: String,
    val isEncrypted: Boolean = true,
    val checksumSha256: String = ""
)

@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey val id: String,
    val playerName: String,
    val score: Long,
    val timeAchievedFormatted: String,
    val deviceTier: String,
    val fpsAvg: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_assets")
data class CustomAssetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // OBJ, SCRIPT, SHADER, SCENE
    val dataContent: String,
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY lastModifiedTimestamp DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)
}

@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM leaderboard ORDER BY score DESC LIMIT 50")
    fun getTopLeaderboard(): Flow<List<LeaderboardEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LeaderboardEntry)

    @Query("DELETE FROM leaderboard")
    suspend fun clearLeaderboard()
}

@Dao
interface AssetDao {
    @Query("SELECT * FROM custom_assets ORDER BY createdTimestamp DESC")
    fun getAllAssets(): Flow<List<CustomAssetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAsset(asset: CustomAssetEntity)

    @Query("DELETE FROM custom_assets WHERE id = :id")
    suspend fun deleteAssetById(id: String)
}

@Database(
    entities = [ProjectEntity::class, LeaderboardEntry::class, CustomAssetEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun leaderboardDao(): LeaderboardDao
    abstract fun assetDao(): AssetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "t3d_engine_database.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class EngineRepository(private val db: AppDatabase) {
    val allProjects: Flow<List<ProjectEntity>> = db.projectDao().getAllProjects()
    val topLeaderboard: Flow<List<LeaderboardEntry>> = db.leaderboardDao().getTopLeaderboard()
    val allAssets: Flow<List<CustomAssetEntity>> = db.assetDao().getAllAssets()

    suspend fun saveProject(project: ProjectEntity) = db.projectDao().insertProject(project)
    suspend fun deleteProject(id: String) = db.projectDao().deleteProjectById(id)

    suspend fun submitLeaderboardScore(entry: LeaderboardEntry) = db.leaderboardDao().insertEntry(entry)
    suspend fun saveAsset(asset: CustomAssetEntity) = db.assetDao().insertAsset(asset)
    suspend fun deleteAsset(id: String) = db.assetDao().deleteAssetById(id)
}
