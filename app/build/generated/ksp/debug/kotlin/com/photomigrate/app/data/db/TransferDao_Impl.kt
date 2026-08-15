package com.photomigrate.app.`data`.db

import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TransferDao_Impl(
  __db: RoomDatabase,
) : TransferDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTransferredFile: EntityInsertAdapter<TransferredFile>

  private val __insertAdapterOfQueuedItem: EntityInsertAdapter<QueuedItem>
  init {
    this.__db = __db
    this.__insertAdapterOfTransferredFile = object : EntityInsertAdapter<TransferredFile>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `transferred_files` (`mediaId`,`destinationAccountId`,`sha256Hash`,`timestamp`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TransferredFile) {
        statement.bindText(1, entity.mediaId)
        statement.bindText(2, entity.destinationAccountId)
        statement.bindText(3, entity.sha256Hash)
        statement.bindLong(4, entity.timestamp)
      }
    }
    this.__insertAdapterOfQueuedItem = object : EntityInsertAdapter<QueuedItem>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `transfer_queue` (`jobId`,`mediaId`) VALUES (?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: QueuedItem) {
        statement.bindText(1, entity.jobId)
        statement.bindText(2, entity.mediaId)
      }
    }
  }

  public override suspend fun insert(`file`: TransferredFile): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTransferredFile.insert(_connection, file)
  }

  public override suspend fun insertQueuedItems(items: List<QueuedItem>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfQueuedItem.insert(_connection, items)
  }

  public override suspend fun getTransferredMediaIds(destId: String): List<String> {
    val _sql: String = "SELECT mediaId FROM transferred_files WHERE destinationAccountId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, destId)
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun findByHash(destId: String, hash: String): TransferredFile? {
    val _sql: String = "SELECT * FROM transferred_files WHERE destinationAccountId = ? AND sha256Hash = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, destId)
        _argIndex = 2
        _stmt.bindText(_argIndex, hash)
        val _columnIndexOfMediaId: Int = getColumnIndexOrThrow(_stmt, "mediaId")
        val _columnIndexOfDestinationAccountId: Int = getColumnIndexOrThrow(_stmt, "destinationAccountId")
        val _columnIndexOfSha256Hash: Int = getColumnIndexOrThrow(_stmt, "sha256Hash")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: TransferredFile?
        if (_stmt.step()) {
          val _tmpMediaId: String
          _tmpMediaId = _stmt.getText(_columnIndexOfMediaId)
          val _tmpDestinationAccountId: String
          _tmpDestinationAccountId = _stmt.getText(_columnIndexOfDestinationAccountId)
          val _tmpSha256Hash: String
          _tmpSha256Hash = _stmt.getText(_columnIndexOfSha256Hash)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _result = TransferredFile(_tmpMediaId,_tmpDestinationAccountId,_tmpSha256Hash,_tmpTimestamp)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getQueuedMediaIds(jobId: String): List<String> {
    val _sql: String = "SELECT mediaId FROM transfer_queue WHERE jobId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, jobId)
        val _result: MutableList<String> = mutableListOf()
        while (_stmt.step()) {
          val _item: String
          _item = _stmt.getText(0)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearForAccount(destId: String) {
    val _sql: String = "DELETE FROM transferred_files WHERE destinationAccountId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, destId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearQueue(jobId: String) {
    val _sql: String = "DELETE FROM transfer_queue WHERE jobId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, jobId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
