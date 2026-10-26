package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Query("SELECT * FROM saved_routes ORDER BY createdAt DESC")
    fun getAllSavedRoutes(): Flow<List<SavedRouteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: SavedRouteEntity)

    @Delete
    suspend fun deleteRoute(route: SavedRouteEntity)

    @Query("DELETE FROM saved_routes WHERE id = :id")
    suspend fun deleteRouteById(id: String)
}

@Dao
interface FavoritePlaceDao {
    @Query("SELECT * FROM favorite_places ORDER BY createdAt DESC")
    fun getAllFavorites(): Flow<List<FavoritePlaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(place: FavoritePlaceEntity)

    @Delete
    suspend fun deleteFavorite(place: FavoritePlaceEntity)

    @Query("DELETE FROM favorite_places WHERE id = :id")
    suspend fun deleteFavoriteById(id: String)
}

@Dao
interface TelemetryLogDao {
    @Query("SELECT * FROM telemetry_logs ORDER BY id DESC LIMIT 200")
    fun getRecentLogs(): Flow<List<TelemetryLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: TelemetryLogEntity)

    @Query("DELETE FROM telemetry_logs")
    suspend fun clearLogs()
}

@Dao
interface CrashReportDao {
    @Query("SELECT * FROM crash_reports ORDER BY createdAt DESC LIMIT 50")
    fun getAllCrashReports(): Flow<List<CrashReportEntity>>

    @Query("SELECT * FROM crash_reports ORDER BY createdAt DESC LIMIT 50")
    fun getCrashReportsSnapshot(): List<CrashReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrashReport(report: CrashReportEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertCrashReportBlocking(report: CrashReportEntity)

    @Query("DELETE FROM crash_reports")
    suspend fun clearCrashReports()

    @Query("DELETE FROM crash_reports")
    fun clearCrashReportsBlocking()

    @Query("SELECT COUNT(*) FROM crash_reports")
    fun getCrashCount(): Flow<Int>
}

@Database(
    entities = [SavedRouteEntity::class, FavoritePlaceEntity::class, TelemetryLogEntity::class, CrashReportEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun favoritePlaceDao(): FavoritePlaceDao
    abstract fun telemetryLogDao(): TelemetryLogDao
    abstract fun crashReportDao(): CrashReportDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mock_gps_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
