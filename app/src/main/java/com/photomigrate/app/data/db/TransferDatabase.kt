package com.photomigrate.app.data.db

import android.content.Context
import androidx.room.*

@Entity(tableName = "transferred_files", primaryKeys = ["mediaId", "destinationAccountId"])
data class TransferredFile(
    val mediaId: String,
    val destinationAccountId: String,
    val sha256Hash: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "transfer_queue", primaryKeys = ["jobId", "mediaId"])
data class QueuedItem(
    val jobId: String,
    val mediaId: String
)

@Dao
interface TransferDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: TransferredFile)

    @Query("SELECT mediaId FROM transferred_files WHERE destinationAccountId = :destId")
    suspend fun getTransferredMediaIds(destId: String): List<String>

    @Query("SELECT * FROM transferred_files WHERE destinationAccountId = :destId AND sha256Hash = :hash LIMIT 1")
    suspend fun findByHash(destId: String, hash: String): TransferredFile?

    @Query("DELETE FROM transferred_files WHERE destinationAccountId = :destId")
    suspend fun clearForAccount(destId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQueuedItems(items: List<QueuedItem>)

    @Query("SELECT mediaId FROM transfer_queue WHERE jobId = :jobId")
    suspend fun getQueuedMediaIds(jobId: String): List<String>

    @Query("DELETE FROM transfer_queue WHERE jobId = :jobId")
    suspend fun clearQueue(jobId: String)
}

@Database(entities = [TransferredFile::class, QueuedItem::class], version = 2, exportSchema = false)
abstract class TransferDatabase : RoomDatabase() {
    abstract fun transferDao(): TransferDao

    companion object {
        @Volatile
        private var INSTANCE: TransferDatabase? = null

        fun getDatabase(context: Context): TransferDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TransferDatabase::class.java,
                    "transfer_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
