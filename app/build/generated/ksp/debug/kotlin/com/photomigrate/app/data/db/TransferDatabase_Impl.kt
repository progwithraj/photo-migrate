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
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(2, "e4373a8eeac622c7805311df4fe0fd88", "6a796e36d02dd73f9658bd00694eaa50") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `transferred_files` (`mediaId` TEXT NOT NULL, `destinationAccountId` TEXT NOT NULL, `sha256Hash` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`mediaId`, `destinationAccountId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `transfer_queue` (`jobId` TEXT NOT NULL, `mediaId` TEXT NOT NULL, PRIMARY KEY(`jobId`, `mediaId`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e4373a8eeac622c7805311df4fe0fd88')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `transferred_files`")
        connection.execSQL("DROP TABLE IF EXISTS `transfer_queue`")
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
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "transferred_files", "transfer_queue")
  }

  public override fun clearAllTables() {
    super.performClear(false, "transferred_files", "transfer_queue")
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
