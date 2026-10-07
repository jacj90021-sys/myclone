package com.recreated.clonemaster.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

/* ---------------------------------------------------------------- spaces */

@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "clones", indices = [Index("spaceId")])
data class CloneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val spaceId: Long,
    val packageName: String,
    val label: String,
    val createdAt: Long = System.currentTimeMillis(),
)

data class SpaceWithClones(
    @Embedded val space: SpaceEntity,
    @Relation(parentColumn = "id", entityColumn = "spaceId")
    val clones: List<CloneEntity>,
)

/* ------------------------------------------------- device spoofing profile */

@Entity(tableName = "device_profile")
data class DeviceProfileEntity(
    @PrimaryKey val id: Int = 1,
    val product: String = "",
    val brand: String = "",
    val model: String = "",
    val device: String = "",
)

/* ------------------------------------------------------------ scan history */

@Entity(tableName = "scan_history")
data class ScanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val at: Long = System.currentTimeMillis(),
)

/* ------------------------------------------------------------------- daos */

@Dao
interface SpaceDao {
    @Transaction
    @Query("SELECT * FROM spaces ORDER BY createdAt DESC")
    fun all(): Flow<List<SpaceWithClones>>

    @Insert suspend fun addSpace(s: SpaceEntity): Long
    @Insert suspend fun addClone(c: CloneEntity)
    @Query("SELECT * FROM spaces ORDER BY createdAt DESC LIMIT 1")
    suspend fun latest(): SpaceEntity?
    @Query("DELETE FROM clones WHERE id = :id") suspend fun deleteClone(id: Long)
    @Query("DELETE FROM spaces WHERE id = :id") suspend fun deleteSpace(id: Long)
    @Query("SELECT COUNT(*) FROM spaces") suspend fun count(): Int
}

@Dao
interface DeviceDao {
    @Query("SELECT * FROM device_profile WHERE id = 1")
    fun profile(): Flow<DeviceProfileEntity?>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(p: DeviceProfileEntity)
}

@Dao
interface ScanDao {
    @Query("SELECT * FROM scan_history ORDER BY at DESC") fun all(): Flow<List<ScanEntity>>
    @Insert suspend fun add(e: ScanEntity)
}

/* -------------------------------------------------------------- database */

@Database(
    entities = [SpaceEntity::class, CloneEntity::class, DeviceProfileEntity::class, ScanEntity::class],
    version = 1, exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun spaceDao(): SpaceDao
    abstract fun deviceDao(): DeviceDao
    abstract fun scanDao(): ScanDao

    companion object {
        fun build(ctx: Context): AppDatabase =
            Room.databaseBuilder(ctx, AppDatabase::class.java, "clonemaster.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
