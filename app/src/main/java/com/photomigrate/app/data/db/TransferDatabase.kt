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

@Entity(tableName = "remote_metadata", primaryKeys = ["accountId", "filename", "sizeBytes", "creationTime"])
data class RemoteMetadata(
    val accountId: String,
    val filename: String,
    val sizeBytes: Long,
    val creationTime: String
)

@Entity(tableName = "transfer_jobs")
data class TransferJobEntity(
    @PrimaryKey val id: String,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val mode: String,
    val orgMode: String,
    val batchAlbumName: String?,
    val totalItems: Int,
    val completedItems: Int,
    val failedItems: Int,
    val totalBytes: Long,
    val transferredBytes: Long,
    val status: String,
    val startTime: Long,
    val endTime: Long?
)

@Entity(tableName = "job_logs")
data class JobLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Int = 0,
    val jobId: String,
    val timestamp: Long,
    val message: String,
    val isError: Boolean
)

@Entity(tableName = "vault_items")
data class VaultItemEntity(
    @PrimaryKey val id: String,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val localEncryptedPath: String,
    val timestamp: Long = System.currentTimeMillis()
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

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRemoteMetadata(items: List<RemoteMetadata>)

    @Query("SELECT sizeBytes FROM remote_metadata WHERE accountId = :accountId AND filename = :filename AND creationTime = :time LIMIT 1")
    suspend fun findRemoteMatchSize(accountId: String, filename: String, time: String): Long?

    @Query("DELETE FROM remote_metadata WHERE accountId = :accountId")
    suspend fun clearRemoteMetadata(accountId: String)

    // History & Analytics
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: TransferJobEntity)

    @Update
    suspend fun updateJob(job: TransferJobEntity)

    @Insert
    suspend fun insertLog(log: JobLogEntity)

    @Query("SELECT * FROM transfer_jobs ORDER BY startTime DESC")
    suspend fun getAllJobs(): List<TransferJobEntity>

    @Query("SELECT * FROM job_logs WHERE jobId = :jobId ORDER BY timestamp ASC")
    suspend fun getLogsForJob(jobId: String): List<JobLogEntity>

    @Query("SELECT SUM(transferredBytes) FROM transfer_jobs")
    suspend fun getTotalTransferredBytes(): Long?

    @Query("SELECT COUNT(*) FROM transfer_jobs")
    suspend fun getJobCount(): Int

    @Query("DELETE FROM transfer_jobs")
    suspend fun clearHistory()

    // Vault
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVaultItem(item: VaultItemEntity)

    @Query("SELECT * FROM vault_items ORDER BY timestamp DESC")
    suspend fun getAllVaultItems(): List<VaultItemEntity>

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteVaultItem(id: String)
}

@Database(entities = [TransferredFile::class, QueuedItem::class, RemoteMetadata::class, TransferJobEntity::class, JobLogEntity::class, VaultItemEntity::class], version = 8, exportSchema = false)
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
