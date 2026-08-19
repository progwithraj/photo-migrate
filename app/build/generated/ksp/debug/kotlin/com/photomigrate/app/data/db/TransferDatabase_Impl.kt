package com.photomigrate.app.`data`.db

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TransferDatabase_Impl : TransferDatabase() {
  private val _transferDao: Lazy<TransferDao> = lazy {
    TransferDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(8, "aba8f347e47893e22acb295a3b6a24b7", "b7d3693118a68d2a2171f6bd26a2e0a0") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `transferred_files` (`mediaId` TEXT NOT NULL, `destinationAccountId` TEXT NOT NULL, `sha256Hash` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`mediaId`, `destinationAccountId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `transfer_queue` (`jobId` TEXT NOT NULL, `mediaId` TEXT NOT NULL, PRIMARY KEY(`jobId`, `mediaId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `remote_metadata` (`accountId` TEXT NOT NULL, `filename` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `creationTime` TEXT NOT NULL, PRIMARY KEY(`accountId`, `filename`, `sizeBytes`, `creationTime`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `transfer_jobs` (`id` TEXT NOT NULL, `sourceAccountId` TEXT NOT NULL, `destinationAccountId` TEXT NOT NULL, `mode` TEXT NOT NULL, `orgMode` TEXT NOT NULL, `batchAlbumName` TEXT, `totalItems` INTEGER NOT NULL, `completedItems` INTEGER NOT NULL, `failedItems` INTEGER NOT NULL, `totalBytes` INTEGER NOT NULL, `transferredBytes` INTEGER NOT NULL, `status` TEXT NOT NULL, `startTime` INTEGER NOT NULL, `endTime` INTEGER, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `job_logs` (`logId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `jobId` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, `message` TEXT NOT NULL, `isError` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `vault_items` (`id` TEXT NOT NULL, `filename` TEXT NOT NULL, `mimeType` TEXT NOT NULL, `sizeBytes` INTEGER NOT NULL, `localEncryptedPath` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'aba8f347e47893e22acb295a3b6a24b7')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `transferred_files`")
        connection.execSQL("DROP TABLE IF EXISTS `transfer_queue`")
        connection.execSQL("DROP TABLE IF EXISTS `remote_metadata`")
        connection.execSQL("DROP TABLE IF EXISTS `transfer_jobs`")
        connection.execSQL("DROP TABLE IF EXISTS `job_logs`")
        connection.execSQL("DROP TABLE IF EXISTS `vault_items`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection): RoomOpenDelegate.ValidationResult {
        val _columnsTransferredFiles: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTransferredFiles.put("mediaId", TableInfo.Column("mediaId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferredFiles.put("destinationAccountId", TableInfo.Column("destinationAccountId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferredFiles.put("sha256Hash", TableInfo.Column("sha256Hash", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferredFiles.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTransferredFiles: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTransferredFiles: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTransferredFiles: TableInfo = TableInfo("transferred_files", _columnsTransferredFiles, _foreignKeysTransferredFiles, _indicesTransferredFiles)
        val _existingTransferredFiles: TableInfo = read(connection, "transferred_files")
        if (!_infoTransferredFiles.equals(_existingTransferredFiles)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |transferred_files(com.photomigrate.app.data.db.TransferredFile).
              | Expected:
              |""".trimMargin() + _infoTransferredFiles + """
              |
              | Found:
              |""".trimMargin() + _existingTransferredFiles)
        }
        val _columnsTransferQueue: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTransferQueue.put("jobId", TableInfo.Column("jobId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferQueue.put("mediaId", TableInfo.Column("mediaId", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTransferQueue: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTransferQueue: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTransferQueue: TableInfo = TableInfo("transfer_queue", _columnsTransferQueue, _foreignKeysTransferQueue, _indicesTransferQueue)
        val _existingTransferQueue: TableInfo = read(connection, "transfer_queue")
        if (!_infoTransferQueue.equals(_existingTransferQueue)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |transfer_queue(com.photomigrate.app.data.db.QueuedItem).
              | Expected:
              |""".trimMargin() + _infoTransferQueue + """
              |
              | Found:
              |""".trimMargin() + _existingTransferQueue)
        }
        val _columnsRemoteMetadata: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsRemoteMetadata.put("accountId", TableInfo.Column("accountId", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRemoteMetadata.put("filename", TableInfo.Column("filename", "TEXT", true, 2, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRemoteMetadata.put("sizeBytes", TableInfo.Column("sizeBytes", "INTEGER", true, 3, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsRemoteMetadata.put("creationTime", TableInfo.Column("creationTime", "TEXT", true, 4, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysRemoteMetadata: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesRemoteMetadata: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoRemoteMetadata: TableInfo = TableInfo("remote_metadata", _columnsRemoteMetadata, _foreignKeysRemoteMetadata, _indicesRemoteMetadata)
        val _existingRemoteMetadata: TableInfo = read(connection, "remote_metadata")
        if (!_infoRemoteMetadata.equals(_existingRemoteMetadata)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |remote_metadata(com.photomigrate.app.data.db.RemoteMetadata).
              | Expected:
              |""".trimMargin() + _infoRemoteMetadata + """
              |
              | Found:
              |""".trimMargin() + _existingRemoteMetadata)
        }
        val _columnsTransferJobs: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTransferJobs.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferJobs.put("sourceAccountId", TableInfo.Column("sourceAccountId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferJobs.put("destinationAccountId", TableInfo.Column("destinationAccountId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferJobs.put("mode", TableInfo.Column("mode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferJobs.put("orgMode", TableInfo.Column("orgMode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferJobs.put("batchAlbumName", TableInfo.Column("batchAlbumName", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferJobs.put("totalItems", TableInfo.Column("totalItems", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferJobs.put("completedItems", TableInfo.Column("completedItems", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferJobs.put("failedItems", TableInfo.Column("failedItems", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferJobs.put("totalBytes", TableInfo.Column("totalBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferJobs.put("transferredBytes", TableInfo.Column("transferredBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferJobs.put("status", TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferJobs.put("startTime", TableInfo.Column("startTime", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTransferJobs.put("endTime", TableInfo.Column("endTime", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTransferJobs: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTransferJobs: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTransferJobs: TableInfo = TableInfo("transfer_jobs", _columnsTransferJobs, _foreignKeysTransferJobs, _indicesTransferJobs)
        val _existingTransferJobs: TableInfo = read(connection, "transfer_jobs")
        if (!_infoTransferJobs.equals(_existingTransferJobs)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |transfer_jobs(com.photomigrate.app.data.db.TransferJobEntity).
              | Expected:
              |""".trimMargin() + _infoTransferJobs + """
              |
              | Found:
              |""".trimMargin() + _existingTransferJobs)
        }
        val _columnsJobLogs: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsJobLogs.put("logId", TableInfo.Column("logId", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsJobLogs.put("jobId", TableInfo.Column("jobId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsJobLogs.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsJobLogs.put("message", TableInfo.Column("message", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsJobLogs.put("isError", TableInfo.Column("isError", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysJobLogs: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesJobLogs: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoJobLogs: TableInfo = TableInfo("job_logs", _columnsJobLogs, _foreignKeysJobLogs, _indicesJobLogs)
        val _existingJobLogs: TableInfo = read(connection, "job_logs")
        if (!_infoJobLogs.equals(_existingJobLogs)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |job_logs(com.photomigrate.app.data.db.JobLogEntity).
              | Expected:
              |""".trimMargin() + _infoJobLogs + """
              |
              | Found:
              |""".trimMargin() + _existingJobLogs)
        }
        val _columnsVaultItems: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsVaultItems.put("id", TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVaultItems.put("filename", TableInfo.Column("filename", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVaultItems.put("mimeType", TableInfo.Column("mimeType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVaultItems.put("sizeBytes", TableInfo.Column("sizeBytes", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVaultItems.put("localEncryptedPath", TableInfo.Column("localEncryptedPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsVaultItems.put("timestamp", TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysVaultItems: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesVaultItems: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoVaultItems: TableInfo = TableInfo("vault_items", _columnsVaultItems, _foreignKeysVaultItems, _indicesVaultItems)
        val _existingVaultItems: TableInfo = read(connection, "vault_items")
        if (!_infoVaultItems.equals(_existingVaultItems)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |vault_items(com.photomigrate.app.data.db.VaultItemEntity).
              | Expected:
              |""".trimMargin() + _infoVaultItems + """
              |
              | Found:
              |""".trimMargin() + _existingVaultItems)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "transferred_files", "transfer_queue", "remote_metadata", "transfer_jobs", "job_logs", "vault_items")
  }

  public override fun clearAllTables() {
    super.performClear(false, "transferred_files", "transfer_queue", "remote_metadata", "transfer_jobs", "job_logs", "vault_items")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(TransferDao::class, TransferDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>): List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun transferDao(): TransferDao = _transferDao.value
}
