package com.photomigrate.app.`data`.db

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
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

  private val __insertAdapterOfRemoteMetadata: EntityInsertAdapter<RemoteMetadata>

  private val __insertAdapterOfTransferJobEntity: EntityInsertAdapter<TransferJobEntity>

  private val __insertAdapterOfJobLogEntity: EntityInsertAdapter<JobLogEntity>

  private val __insertAdapterOfVaultItemEntity: EntityInsertAdapter<VaultItemEntity>

  private val __insertAdapterOfPendingCleanup: EntityInsertAdapter<PendingCleanup>

  private val __updateAdapterOfTransferJobEntity: EntityDeleteOrUpdateAdapter<TransferJobEntity>
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
    this.__insertAdapterOfRemoteMetadata = object : EntityInsertAdapter<RemoteMetadata>() {
      protected override fun createQuery(): String = "INSERT OR IGNORE INTO `remote_metadata` (`accountId`,`filename`,`sizeBytes`,`creationTime`) VALUES (?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: RemoteMetadata) {
        statement.bindText(1, entity.accountId)
        statement.bindText(2, entity.filename)
        statement.bindLong(3, entity.sizeBytes)
        statement.bindText(4, entity.creationTime)
      }
    }
    this.__insertAdapterOfTransferJobEntity = object : EntityInsertAdapter<TransferJobEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `transfer_jobs` (`id`,`sourceAccountId`,`destinationAccountId`,`destinationType`,`mode`,`orgMode`,`batchAlbumName`,`isCompressionEnabled`,`isResumed`,`totalItems`,`completedItems`,`failedItems`,`totalBytes`,`transferredBytes`,`status`,`startTime`,`endTime`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TransferJobEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.sourceAccountId)
        statement.bindText(3, entity.destinationAccountId)
        statement.bindText(4, entity.destinationType)
        statement.bindText(5, entity.mode)
        statement.bindText(6, entity.orgMode)
        val _tmpBatchAlbumName: String? = entity.batchAlbumName
        if (_tmpBatchAlbumName == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpBatchAlbumName)
        }
        val _tmp: Int = if (entity.isCompressionEnabled) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        val _tmp_1: Int = if (entity.isResumed) 1 else 0
        statement.bindLong(9, _tmp_1.toLong())
        statement.bindLong(10, entity.totalItems.toLong())
        statement.bindLong(11, entity.completedItems.toLong())
        statement.bindLong(12, entity.failedItems.toLong())
        statement.bindLong(13, entity.totalBytes)
        statement.bindLong(14, entity.transferredBytes)
        statement.bindText(15, entity.status)
        statement.bindLong(16, entity.startTime)
        val _tmpEndTime: Long? = entity.endTime
        if (_tmpEndTime == null) {
          statement.bindNull(17)
        } else {
          statement.bindLong(17, _tmpEndTime)
        }
      }
    }
    this.__insertAdapterOfJobLogEntity = object : EntityInsertAdapter<JobLogEntity>() {
      protected override fun createQuery(): String = "INSERT OR ABORT INTO `job_logs` (`logId`,`jobId`,`timestamp`,`message`,`isError`) VALUES (nullif(?, 0),?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: JobLogEntity) {
        statement.bindLong(1, entity.logId.toLong())
        statement.bindText(2, entity.jobId)
        statement.bindLong(3, entity.timestamp)
        statement.bindText(4, entity.message)
        val _tmp: Int = if (entity.isError) 1 else 0
        statement.bindLong(5, _tmp.toLong())
      }
    }
    this.__insertAdapterOfVaultItemEntity = object : EntityInsertAdapter<VaultItemEntity>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `vault_items` (`id`,`filename`,`mimeType`,`sizeBytes`,`localEncryptedPath`,`timestamp`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: VaultItemEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.filename)
        statement.bindText(3, entity.mimeType)
        statement.bindLong(4, entity.sizeBytes)
        statement.bindText(5, entity.localEncryptedPath)
        statement.bindLong(6, entity.timestamp)
      }
    }
    this.__insertAdapterOfPendingCleanup = object : EntityInsertAdapter<PendingCleanup>() {
      protected override fun createQuery(): String = "INSERT OR REPLACE INTO `pending_cleanups` (`mediaId`,`accountId`,`filename`,`errorReason`,`timestamp`) VALUES (?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: PendingCleanup) {
        statement.bindText(1, entity.mediaId)
        statement.bindText(2, entity.accountId)
        statement.bindText(3, entity.filename)
        val _tmpErrorReason: String? = entity.errorReason
        if (_tmpErrorReason == null) {
          statement.bindNull(4)
        } else {
          statement.bindText(4, _tmpErrorReason)
        }
        statement.bindLong(5, entity.timestamp)
      }
    }
    this.__updateAdapterOfTransferJobEntity = object : EntityDeleteOrUpdateAdapter<TransferJobEntity>() {
      protected override fun createQuery(): String = "UPDATE OR ABORT `transfer_jobs` SET `id` = ?,`sourceAccountId` = ?,`destinationAccountId` = ?,`destinationType` = ?,`mode` = ?,`orgMode` = ?,`batchAlbumName` = ?,`isCompressionEnabled` = ?,`isResumed` = ?,`totalItems` = ?,`completedItems` = ?,`failedItems` = ?,`totalBytes` = ?,`transferredBytes` = ?,`status` = ?,`startTime` = ?,`endTime` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TransferJobEntity) {
        statement.bindText(1, entity.id)
        statement.bindText(2, entity.sourceAccountId)
        statement.bindText(3, entity.destinationAccountId)
        statement.bindText(4, entity.destinationType)
        statement.bindText(5, entity.mode)
        statement.bindText(6, entity.orgMode)
        val _tmpBatchAlbumName: String? = entity.batchAlbumName
        if (_tmpBatchAlbumName == null) {
          statement.bindNull(7)
        } else {
          statement.bindText(7, _tmpBatchAlbumName)
        }
        val _tmp: Int = if (entity.isCompressionEnabled) 1 else 0
        statement.bindLong(8, _tmp.toLong())
        val _tmp_1: Int = if (entity.isResumed) 1 else 0
        statement.bindLong(9, _tmp_1.toLong())
        statement.bindLong(10, entity.totalItems.toLong())
        statement.bindLong(11, entity.completedItems.toLong())
        statement.bindLong(12, entity.failedItems.toLong())
        statement.bindLong(13, entity.totalBytes)
        statement.bindLong(14, entity.transferredBytes)
        statement.bindText(15, entity.status)
        statement.bindLong(16, entity.startTime)
        val _tmpEndTime: Long? = entity.endTime
        if (_tmpEndTime == null) {
          statement.bindNull(17)
        } else {
          statement.bindLong(17, _tmpEndTime)
        }
        statement.bindText(18, entity.id)
      }
    }
  }

  public override suspend fun insert(`file`: TransferredFile): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTransferredFile.insert(_connection, file)
  }

  public override suspend fun insertQueuedItems(items: List<QueuedItem>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfQueuedItem.insert(_connection, items)
  }

  public override suspend fun insertRemoteMetadata(items: List<RemoteMetadata>): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfRemoteMetadata.insert(_connection, items)
  }

  public override suspend fun insertJob(job: TransferJobEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfTransferJobEntity.insert(_connection, job)
  }

  public override suspend fun insertLog(log: JobLogEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfJobLogEntity.insert(_connection, log)
  }

  public override suspend fun insertVaultItem(item: VaultItemEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfVaultItemEntity.insert(_connection, item)
  }

  public override suspend fun insertPendingCleanup(item: PendingCleanup): Unit = performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfPendingCleanup.insert(_connection, item)
  }

  public override suspend fun updateJob(job: TransferJobEntity): Unit = performSuspending(__db, false, true) { _connection ->
    __updateAdapterOfTransferJobEntity.handle(_connection, job)
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

  public override suspend fun findRemoteMatchSize(
    accountId: String,
    filename: String,
    time: String,
  ): Long? {
    val _sql: String = "SELECT sizeBytes FROM remote_metadata WHERE accountId = ? AND filename = ? AND creationTime = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, accountId)
        _argIndex = 2
        _stmt.bindText(_argIndex, filename)
        _argIndex = 3
        _stmt.bindText(_argIndex, time)
        val _result: Long?
        if (_stmt.step()) {
          if (_stmt.isNull(0)) {
            _result = null
          } else {
            _result = _stmt.getLong(0)
          }
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllJobs(): List<TransferJobEntity> {
    val _sql: String = "SELECT * FROM transfer_jobs ORDER BY startTime DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSourceAccountId: Int = getColumnIndexOrThrow(_stmt, "sourceAccountId")
        val _columnIndexOfDestinationAccountId: Int = getColumnIndexOrThrow(_stmt, "destinationAccountId")
        val _columnIndexOfDestinationType: Int = getColumnIndexOrThrow(_stmt, "destinationType")
        val _columnIndexOfMode: Int = getColumnIndexOrThrow(_stmt, "mode")
        val _columnIndexOfOrgMode: Int = getColumnIndexOrThrow(_stmt, "orgMode")
        val _columnIndexOfBatchAlbumName: Int = getColumnIndexOrThrow(_stmt, "batchAlbumName")
        val _columnIndexOfIsCompressionEnabled: Int = getColumnIndexOrThrow(_stmt, "isCompressionEnabled")
        val _columnIndexOfIsResumed: Int = getColumnIndexOrThrow(_stmt, "isResumed")
        val _columnIndexOfTotalItems: Int = getColumnIndexOrThrow(_stmt, "totalItems")
        val _columnIndexOfCompletedItems: Int = getColumnIndexOrThrow(_stmt, "completedItems")
        val _columnIndexOfFailedItems: Int = getColumnIndexOrThrow(_stmt, "failedItems")
        val _columnIndexOfTotalBytes: Int = getColumnIndexOrThrow(_stmt, "totalBytes")
        val _columnIndexOfTransferredBytes: Int = getColumnIndexOrThrow(_stmt, "transferredBytes")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfStartTime: Int = getColumnIndexOrThrow(_stmt, "startTime")
        val _columnIndexOfEndTime: Int = getColumnIndexOrThrow(_stmt, "endTime")
        val _result: MutableList<TransferJobEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TransferJobEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSourceAccountId: String
          _tmpSourceAccountId = _stmt.getText(_columnIndexOfSourceAccountId)
          val _tmpDestinationAccountId: String
          _tmpDestinationAccountId = _stmt.getText(_columnIndexOfDestinationAccountId)
          val _tmpDestinationType: String
          _tmpDestinationType = _stmt.getText(_columnIndexOfDestinationType)
          val _tmpMode: String
          _tmpMode = _stmt.getText(_columnIndexOfMode)
          val _tmpOrgMode: String
          _tmpOrgMode = _stmt.getText(_columnIndexOfOrgMode)
          val _tmpBatchAlbumName: String?
          if (_stmt.isNull(_columnIndexOfBatchAlbumName)) {
            _tmpBatchAlbumName = null
          } else {
            _tmpBatchAlbumName = _stmt.getText(_columnIndexOfBatchAlbumName)
          }
          val _tmpIsCompressionEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsCompressionEnabled).toInt()
          _tmpIsCompressionEnabled = _tmp != 0
          val _tmpIsResumed: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsResumed).toInt()
          _tmpIsResumed = _tmp_1 != 0
          val _tmpTotalItems: Int
          _tmpTotalItems = _stmt.getLong(_columnIndexOfTotalItems).toInt()
          val _tmpCompletedItems: Int
          _tmpCompletedItems = _stmt.getLong(_columnIndexOfCompletedItems).toInt()
          val _tmpFailedItems: Int
          _tmpFailedItems = _stmt.getLong(_columnIndexOfFailedItems).toInt()
          val _tmpTotalBytes: Long
          _tmpTotalBytes = _stmt.getLong(_columnIndexOfTotalBytes)
          val _tmpTransferredBytes: Long
          _tmpTransferredBytes = _stmt.getLong(_columnIndexOfTransferredBytes)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpStartTime: Long
          _tmpStartTime = _stmt.getLong(_columnIndexOfStartTime)
          val _tmpEndTime: Long?
          if (_stmt.isNull(_columnIndexOfEndTime)) {
            _tmpEndTime = null
          } else {
            _tmpEndTime = _stmt.getLong(_columnIndexOfEndTime)
          }
          _item = TransferJobEntity(_tmpId,_tmpSourceAccountId,_tmpDestinationAccountId,_tmpDestinationType,_tmpMode,_tmpOrgMode,_tmpBatchAlbumName,_tmpIsCompressionEnabled,_tmpIsResumed,_tmpTotalItems,_tmpCompletedItems,_tmpFailedItems,_tmpTotalBytes,_tmpTransferredBytes,_tmpStatus,_tmpStartTime,_tmpEndTime)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getLogsForJob(jobId: String): List<JobLogEntity> {
    val _sql: String = "SELECT * FROM job_logs WHERE jobId = ? ORDER BY timestamp ASC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, jobId)
        val _columnIndexOfLogId: Int = getColumnIndexOrThrow(_stmt, "logId")
        val _columnIndexOfJobId: Int = getColumnIndexOrThrow(_stmt, "jobId")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _columnIndexOfMessage: Int = getColumnIndexOrThrow(_stmt, "message")
        val _columnIndexOfIsError: Int = getColumnIndexOrThrow(_stmt, "isError")
        val _result: MutableList<JobLogEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: JobLogEntity
          val _tmpLogId: Int
          _tmpLogId = _stmt.getLong(_columnIndexOfLogId).toInt()
          val _tmpJobId: String
          _tmpJobId = _stmt.getText(_columnIndexOfJobId)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          val _tmpMessage: String
          _tmpMessage = _stmt.getText(_columnIndexOfMessage)
          val _tmpIsError: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsError).toInt()
          _tmpIsError = _tmp != 0
          _item = JobLogEntity(_tmpLogId,_tmpJobId,_tmpTimestamp,_tmpMessage,_tmpIsError)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getTotalTransferredBytes(): Long? {
    val _sql: String = "SELECT SUM(transferredBytes) FROM transfer_jobs"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Long?
        if (_stmt.step()) {
          val _tmp: Long?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(0)
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getJobCount(): Int {
    val _sql: String = "SELECT COUNT(*) FROM transfer_jobs"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int
        if (_stmt.step()) {
          val _tmp: Int
          _tmp = _stmt.getLong(0).toInt()
          _result = _tmp
        } else {
          _result = 0
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getLastIncompleteJob(): TransferJobEntity? {
    val _sql: String = "SELECT * FROM transfer_jobs WHERE status NOT IN ('COMPLETED', 'CANCELLED') ORDER BY startTime DESC LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSourceAccountId: Int = getColumnIndexOrThrow(_stmt, "sourceAccountId")
        val _columnIndexOfDestinationAccountId: Int = getColumnIndexOrThrow(_stmt, "destinationAccountId")
        val _columnIndexOfDestinationType: Int = getColumnIndexOrThrow(_stmt, "destinationType")
        val _columnIndexOfMode: Int = getColumnIndexOrThrow(_stmt, "mode")
        val _columnIndexOfOrgMode: Int = getColumnIndexOrThrow(_stmt, "orgMode")
        val _columnIndexOfBatchAlbumName: Int = getColumnIndexOrThrow(_stmt, "batchAlbumName")
        val _columnIndexOfIsCompressionEnabled: Int = getColumnIndexOrThrow(_stmt, "isCompressionEnabled")
        val _columnIndexOfIsResumed: Int = getColumnIndexOrThrow(_stmt, "isResumed")
        val _columnIndexOfTotalItems: Int = getColumnIndexOrThrow(_stmt, "totalItems")
        val _columnIndexOfCompletedItems: Int = getColumnIndexOrThrow(_stmt, "completedItems")
        val _columnIndexOfFailedItems: Int = getColumnIndexOrThrow(_stmt, "failedItems")
        val _columnIndexOfTotalBytes: Int = getColumnIndexOrThrow(_stmt, "totalBytes")
        val _columnIndexOfTransferredBytes: Int = getColumnIndexOrThrow(_stmt, "transferredBytes")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfStartTime: Int = getColumnIndexOrThrow(_stmt, "startTime")
        val _columnIndexOfEndTime: Int = getColumnIndexOrThrow(_stmt, "endTime")
        val _result: TransferJobEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSourceAccountId: String
          _tmpSourceAccountId = _stmt.getText(_columnIndexOfSourceAccountId)
          val _tmpDestinationAccountId: String
          _tmpDestinationAccountId = _stmt.getText(_columnIndexOfDestinationAccountId)
          val _tmpDestinationType: String
          _tmpDestinationType = _stmt.getText(_columnIndexOfDestinationType)
          val _tmpMode: String
          _tmpMode = _stmt.getText(_columnIndexOfMode)
          val _tmpOrgMode: String
          _tmpOrgMode = _stmt.getText(_columnIndexOfOrgMode)
          val _tmpBatchAlbumName: String?
          if (_stmt.isNull(_columnIndexOfBatchAlbumName)) {
            _tmpBatchAlbumName = null
          } else {
            _tmpBatchAlbumName = _stmt.getText(_columnIndexOfBatchAlbumName)
          }
          val _tmpIsCompressionEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsCompressionEnabled).toInt()
          _tmpIsCompressionEnabled = _tmp != 0
          val _tmpIsResumed: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsResumed).toInt()
          _tmpIsResumed = _tmp_1 != 0
          val _tmpTotalItems: Int
          _tmpTotalItems = _stmt.getLong(_columnIndexOfTotalItems).toInt()
          val _tmpCompletedItems: Int
          _tmpCompletedItems = _stmt.getLong(_columnIndexOfCompletedItems).toInt()
          val _tmpFailedItems: Int
          _tmpFailedItems = _stmt.getLong(_columnIndexOfFailedItems).toInt()
          val _tmpTotalBytes: Long
          _tmpTotalBytes = _stmt.getLong(_columnIndexOfTotalBytes)
          val _tmpTransferredBytes: Long
          _tmpTransferredBytes = _stmt.getLong(_columnIndexOfTransferredBytes)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpStartTime: Long
          _tmpStartTime = _stmt.getLong(_columnIndexOfStartTime)
          val _tmpEndTime: Long?
          if (_stmt.isNull(_columnIndexOfEndTime)) {
            _tmpEndTime = null
          } else {
            _tmpEndTime = _stmt.getLong(_columnIndexOfEndTime)
          }
          _result = TransferJobEntity(_tmpId,_tmpSourceAccountId,_tmpDestinationAccountId,_tmpDestinationType,_tmpMode,_tmpOrgMode,_tmpBatchAlbumName,_tmpIsCompressionEnabled,_tmpIsResumed,_tmpTotalItems,_tmpCompletedItems,_tmpFailedItems,_tmpTotalBytes,_tmpTransferredBytes,_tmpStatus,_tmpStartTime,_tmpEndTime)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getJobById(id: String): TransferJobEntity? {
    val _sql: String = "SELECT * FROM transfer_jobs WHERE id = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfSourceAccountId: Int = getColumnIndexOrThrow(_stmt, "sourceAccountId")
        val _columnIndexOfDestinationAccountId: Int = getColumnIndexOrThrow(_stmt, "destinationAccountId")
        val _columnIndexOfDestinationType: Int = getColumnIndexOrThrow(_stmt, "destinationType")
        val _columnIndexOfMode: Int = getColumnIndexOrThrow(_stmt, "mode")
        val _columnIndexOfOrgMode: Int = getColumnIndexOrThrow(_stmt, "orgMode")
        val _columnIndexOfBatchAlbumName: Int = getColumnIndexOrThrow(_stmt, "batchAlbumName")
        val _columnIndexOfIsCompressionEnabled: Int = getColumnIndexOrThrow(_stmt, "isCompressionEnabled")
        val _columnIndexOfIsResumed: Int = getColumnIndexOrThrow(_stmt, "isResumed")
        val _columnIndexOfTotalItems: Int = getColumnIndexOrThrow(_stmt, "totalItems")
        val _columnIndexOfCompletedItems: Int = getColumnIndexOrThrow(_stmt, "completedItems")
        val _columnIndexOfFailedItems: Int = getColumnIndexOrThrow(_stmt, "failedItems")
        val _columnIndexOfTotalBytes: Int = getColumnIndexOrThrow(_stmt, "totalBytes")
        val _columnIndexOfTransferredBytes: Int = getColumnIndexOrThrow(_stmt, "transferredBytes")
        val _columnIndexOfStatus: Int = getColumnIndexOrThrow(_stmt, "status")
        val _columnIndexOfStartTime: Int = getColumnIndexOrThrow(_stmt, "startTime")
        val _columnIndexOfEndTime: Int = getColumnIndexOrThrow(_stmt, "endTime")
        val _result: TransferJobEntity?
        if (_stmt.step()) {
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpSourceAccountId: String
          _tmpSourceAccountId = _stmt.getText(_columnIndexOfSourceAccountId)
          val _tmpDestinationAccountId: String
          _tmpDestinationAccountId = _stmt.getText(_columnIndexOfDestinationAccountId)
          val _tmpDestinationType: String
          _tmpDestinationType = _stmt.getText(_columnIndexOfDestinationType)
          val _tmpMode: String
          _tmpMode = _stmt.getText(_columnIndexOfMode)
          val _tmpOrgMode: String
          _tmpOrgMode = _stmt.getText(_columnIndexOfOrgMode)
          val _tmpBatchAlbumName: String?
          if (_stmt.isNull(_columnIndexOfBatchAlbumName)) {
            _tmpBatchAlbumName = null
          } else {
            _tmpBatchAlbumName = _stmt.getText(_columnIndexOfBatchAlbumName)
          }
          val _tmpIsCompressionEnabled: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsCompressionEnabled).toInt()
          _tmpIsCompressionEnabled = _tmp != 0
          val _tmpIsResumed: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsResumed).toInt()
          _tmpIsResumed = _tmp_1 != 0
          val _tmpTotalItems: Int
          _tmpTotalItems = _stmt.getLong(_columnIndexOfTotalItems).toInt()
          val _tmpCompletedItems: Int
          _tmpCompletedItems = _stmt.getLong(_columnIndexOfCompletedItems).toInt()
          val _tmpFailedItems: Int
          _tmpFailedItems = _stmt.getLong(_columnIndexOfFailedItems).toInt()
          val _tmpTotalBytes: Long
          _tmpTotalBytes = _stmt.getLong(_columnIndexOfTotalBytes)
          val _tmpTransferredBytes: Long
          _tmpTransferredBytes = _stmt.getLong(_columnIndexOfTransferredBytes)
          val _tmpStatus: String
          _tmpStatus = _stmt.getText(_columnIndexOfStatus)
          val _tmpStartTime: Long
          _tmpStartTime = _stmt.getLong(_columnIndexOfStartTime)
          val _tmpEndTime: Long?
          if (_stmt.isNull(_columnIndexOfEndTime)) {
            _tmpEndTime = null
          } else {
            _tmpEndTime = _stmt.getLong(_columnIndexOfEndTime)
          }
          _result = TransferJobEntity(_tmpId,_tmpSourceAccountId,_tmpDestinationAccountId,_tmpDestinationType,_tmpMode,_tmpOrgMode,_tmpBatchAlbumName,_tmpIsCompressionEnabled,_tmpIsResumed,_tmpTotalItems,_tmpCompletedItems,_tmpFailedItems,_tmpTotalBytes,_tmpTransferredBytes,_tmpStatus,_tmpStartTime,_tmpEndTime)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllVaultItems(): List<VaultItemEntity> {
    val _sql: String = "SELECT * FROM vault_items ORDER BY timestamp DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfFilename: Int = getColumnIndexOrThrow(_stmt, "filename")
        val _columnIndexOfMimeType: Int = getColumnIndexOrThrow(_stmt, "mimeType")
        val _columnIndexOfSizeBytes: Int = getColumnIndexOrThrow(_stmt, "sizeBytes")
        val _columnIndexOfLocalEncryptedPath: Int = getColumnIndexOrThrow(_stmt, "localEncryptedPath")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<VaultItemEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: VaultItemEntity
          val _tmpId: String
          _tmpId = _stmt.getText(_columnIndexOfId)
          val _tmpFilename: String
          _tmpFilename = _stmt.getText(_columnIndexOfFilename)
          val _tmpMimeType: String
          _tmpMimeType = _stmt.getText(_columnIndexOfMimeType)
          val _tmpSizeBytes: Long
          _tmpSizeBytes = _stmt.getLong(_columnIndexOfSizeBytes)
          val _tmpLocalEncryptedPath: String
          _tmpLocalEncryptedPath = _stmt.getText(_columnIndexOfLocalEncryptedPath)
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item = VaultItemEntity(_tmpId,_tmpFilename,_tmpMimeType,_tmpSizeBytes,_tmpLocalEncryptedPath,_tmpTimestamp)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllPendingCleanups(): List<PendingCleanup> {
    val _sql: String = "SELECT * FROM pending_cleanups ORDER BY timestamp DESC"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfMediaId: Int = getColumnIndexOrThrow(_stmt, "mediaId")
        val _columnIndexOfAccountId: Int = getColumnIndexOrThrow(_stmt, "accountId")
        val _columnIndexOfFilename: Int = getColumnIndexOrThrow(_stmt, "filename")
        val _columnIndexOfErrorReason: Int = getColumnIndexOrThrow(_stmt, "errorReason")
        val _columnIndexOfTimestamp: Int = getColumnIndexOrThrow(_stmt, "timestamp")
        val _result: MutableList<PendingCleanup> = mutableListOf()
        while (_stmt.step()) {
          val _item: PendingCleanup
          val _tmpMediaId: String
          _tmpMediaId = _stmt.getText(_columnIndexOfMediaId)
          val _tmpAccountId: String
          _tmpAccountId = _stmt.getText(_columnIndexOfAccountId)
          val _tmpFilename: String
          _tmpFilename = _stmt.getText(_columnIndexOfFilename)
          val _tmpErrorReason: String?
          if (_stmt.isNull(_columnIndexOfErrorReason)) {
            _tmpErrorReason = null
          } else {
            _tmpErrorReason = _stmt.getText(_columnIndexOfErrorReason)
          }
          val _tmpTimestamp: Long
          _tmpTimestamp = _stmt.getLong(_columnIndexOfTimestamp)
          _item = PendingCleanup(_tmpMediaId,_tmpAccountId,_tmpFilename,_tmpErrorReason,_tmpTimestamp)
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

  public override suspend fun clearRemoteMetadata(accountId: String) {
    val _sql: String = "DELETE FROM remote_metadata WHERE accountId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, accountId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun clearHistory() {
    val _sql: String = "DELETE FROM transfer_jobs"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteVaultItem(id: String) {
    val _sql: String = "DELETE FROM vault_items WHERE id = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, id)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deletePendingCleanup(mediaId: String, accountId: String) {
    val _sql: String = "DELETE FROM pending_cleanups WHERE mediaId = ? AND accountId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, mediaId)
        _argIndex = 2
        _stmt.bindText(_argIndex, accountId)
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
