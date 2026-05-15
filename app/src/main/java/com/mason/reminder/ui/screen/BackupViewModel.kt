package com.mason.reminder.ui.screen

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mason.reminder.data.backup.BackupExporter
import com.mason.reminder.data.backup.BackupImporter
import com.mason.reminder.data.db.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class BackupUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val lastExportResult: ExportResult? = null,
    val lastImportResult: ImportResult? = null,
    val error: String? = null
)

sealed class ExportResult {
    data class Success(val uri: Uri, val size: Long) : ExportResult()
    data class Error(val message: String) : ExportResult()
}

sealed class ImportResult {
    data class Success(val categories: Int, val tasks: Int, val logs: Int) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) : ViewModel() {

    private val exporter = BackupExporter(database)
    private val importer = BackupImporter(database)

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    /**
     * 导出所有数据为 JSON 文件。
     *
     * 通过 SAF (Storage Access Framework) 让用户选择保存位置，
     * 然后写入 JSON 数据。
     */
    fun exportToUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, error = null)
            try {
                val result = withContext(Dispatchers.IO) {
                    val outputStream = context.contentResolver.openOutputStream(uri)
                        ?: throw IllegalStateException("无法打开输出流")

                    outputStream.use { stream ->
                        exporter.exportToStream(stream)
                    }

                    // 获取文件大小
                    val fileSize = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { it.available().toLong() } ?: 0L
                    }

                    ExportResult.Success(uri, fileSize)
                }
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    lastExportResult = result,
                    lastImportResult = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    error = "导出失败：${e.message}"
                )
            }
        }
    }

    /**
     * 从 JSON 文件导入数据。
     *
     * 通过 SAF 让用户选择文件，读取 JSON 并导入。
     * 导入前会清空现有数据以防重复。
     */
    fun importFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, error = null)
            try {
                val result = withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("无法打开输入流")

                    val importResult = inputStream.use { stream ->
                        importer.importFromStream(stream)
                    }

                    when (importResult) {
                        is BackupImporter.ImportResult.Success ->
                            ImportResult.Success(importResult.categories, importResult.tasks, importResult.logs)
                        is BackupImporter.ImportResult.Error ->
                            ImportResult.Error(importResult.message)
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    lastImportResult = result,
                    lastExportResult = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    error = "导入失败：${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearResults() {
        _uiState.value = _uiState.value.copy(
            lastExportResult = null,
            lastImportResult = null,
            error = null
        )
    }
}