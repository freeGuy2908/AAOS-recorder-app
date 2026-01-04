package com.example.recorderapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorderapp.audio.AudioPlayerManager
import com.example.recorderapp.audio.AudioRecorderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

// Sử dụng AndroidViewModel để có Application Context truyền cho RecorderManager
class AudioViewModel(application: Application) : AndroidViewModel(application) {
    private val recorderManager = AudioRecorderManager(application)
    private val playerManager = AudioPlayerManager()

    // StateFlow để UI lắng nghe sự thay đổi trạng thái
    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _statusMessage = MutableStateFlow("Sẵn sàng")
    val statusMessage = _statusMessage.asStateFlow()

//    private val _filePath = MutableStateFlow("")
//    val filePath = _filePath.asStateFlow()

    private val _recordings = MutableStateFlow<List<File>>(emptyList())
    val recordings = _recordings.asStateFlow()

    // Khởi tạo: Load danh sách file cũ
    init {
        loadRecordings()
    }

    private fun loadRecordings() {
        val dir = getApplication<Application>().getExternalFilesDir(null)
        val files = dir?.listFiles { _, name -> name.endsWith(".pcm") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
        _recordings.value = files
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            recorderManager.stopRecording {
                _isRecording.value = false
                loadRecordings()    // Reload list sau khi dừng
            }
        } else {
            recorderManager.startRecording { success ->
                if (success) _isRecording.value = true
            }
        }
    }

    fun playRecording(file: File) {
        viewModelScope.launch {
            _statusMessage.value = "Đang phát: ${file.name}"
            playerManager.playPcmFile(file)
            _statusMessage.value = "Đã phát xong"
        }
    }

    fun deleteRecording(file: File) {
        if (file.delete()) {
            loadRecordings()
            _statusMessage.value = "Đã xóa file"
        }
    }

    /*private fun stopRecording() {
        recorderManager.stopRecording {
            _isRecording.value = false
            // Lấy đường dẫn file để hiển thị cho user
            val path = getApplication<Application>().getExternalFilesDir(null)?.absolutePath + "/recording_filtered.pcm"
            _filePath.value = path
            _statusMessage.value = "Đã lưu file"
        }
    }*/

    /*private fun startRecording() {
        recorderManager.startRecording { success ->
            if (success) {
                _isRecording.value = true
                _statusMessage.value = "Đang ghi âm và lọc nhiễu..."
            } else {
                _statusMessage.value = "Lỗi: Không thể khởi động Mic"
            }
        }
    }*/

    override fun onCleared() {
        super.onCleared()
        recorderManager.stopRecording {  }  // Đảm bảo dọn dẹp khi thoát màn hình
    }
}