package com.example.recorderapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.recorderapp.audio.AudioRecorderManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// Sử dụng AndroidViewModel để có Application Context truyền cho RecorderManager
class AudioViewModel(application: Application) : AndroidViewModel(application) {
    private val recorderManager = AudioRecorderManager(application)

    // StateFlow để UI lắng nghe sự thay đổi trạng thái
    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _statusMessage = MutableStateFlow("Sẵn sàng")
    val statusMessage = _statusMessage.asStateFlow()

    private val _filePath = MutableStateFlow("")
    val filePath = _filePath.asStateFlow()

    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecording()
        } else {
            startRecording()
        }
    }

    private fun stopRecording() {
        recorderManager.stopRecording {
            _isRecording.value = false
            // Lấy đường dẫn file để hiển thị cho user
            val path = getApplication<Application>().getExternalFilesDir(null)?.absolutePath + "/recording_filtered.pcm"
            _filePath.value = path
            _statusMessage.value = "Đã lưu file"
        }
    }

    private fun startRecording() {
        recorderManager.startRecording { success ->
            if (success) {
                _isRecording.value = true
                _statusMessage.value = "Đang ghi âm và lọc nhiễu..."
            } else {
                _statusMessage.value = "Lỗi: Không thể khởi động Mic"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorderManager.stopRecording {  }  // Đảm bảo dọn dẹp khi thoát màn hình
    }
}