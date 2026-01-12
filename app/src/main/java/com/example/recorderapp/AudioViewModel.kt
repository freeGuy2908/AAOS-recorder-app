package com.example.recorderapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorderapp.audio.AudioPlayerManager
import com.example.recorderapp.audio.AudioRecorderManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

// ViewModel có Application Context truyền cho RecorderManager
class AudioViewModel(application: Application) : AndroidViewModel(application) {
    private val recorderManager = AudioRecorderManager(application)
    private val playerManager = AudioPlayerManager()

    // StateFlow để UI lắng nghe sự thay đổi trạng thái
    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    private val _statusMessage = MutableStateFlow("Sẵn sàng")
    //val statusMessage = _statusMessage.asStateFlow()

    // STATE CHO GHI ÂM (TIMER)
    private val _recordingDurationSeconds = MutableStateFlow(0L)
    val recordingDurationSeconds = _recordingDurationSeconds.asStateFlow()

    // STATE CHO PHÁT NHẠC (MEDIA PLAYER)
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f) // 0.0 đến 1.0
    val playbackProgress = _playbackProgress.asStateFlow()

    private val _currentPlayingFile = MutableStateFlow<File?>(null)
    val currentPlayingFile = _currentPlayingFile.asStateFlow()

    // STATE CHO XÓA
    private val _fileToDelete = MutableStateFlow<File?>(null)
    val fileToDelete = _fileToDelete.asStateFlow()

    private val _recordings = MutableStateFlow<List<File>>(emptyList())
    val recordings = _recordings.asStateFlow()

    private var playBackJob: Job? = null

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
                loadRecordings()
                _recordingDurationSeconds.value = 0
            }
        } else {
            if (_isPlaying.value) stopPlayback()
            recorderManager.startRecording { success ->
                if (success) {
                    _isRecording.value = true
                    startTimer()
                }
            }
        }
    }

    private fun startTimer() {
        viewModelScope.launch {
            _recordingDurationSeconds.value = 0
            while (_isRecording.value) {
                delay(1000)
                _recordingDurationSeconds.value += 1
            }
        }
    }

    fun playRecording(file: File) {
        if (_currentPlayingFile.value == file && _isPlaying.value) return

        stopPlayback()

        playBackJob = viewModelScope.launch {
            try {
                _isPlaying.value = true
                _currentPlayingFile.value = file
                _statusMessage.value = "Đang phát: ${file.name}"

                playerManager.playPcmFile(file) { progress ->
                    _playbackProgress.value = progress
                }
                //_statusMessage.value = "Đã phát xong"
            } finally {
                if (isActive) stopPlayback()
            }
        }
    }

    fun stopPlayback() {
        // Lệnh này sẽ gửi tín hiệu xuống ensureActive() ở AudioPlayerManager
        playBackJob?.cancel()
        playBackJob = null

        _isPlaying.value = false
        _playbackProgress.value = 0f
        _currentPlayingFile.value = null
        _statusMessage.value = "Sẵn sàng"
    }

    // --- LOGIC XÓA FILE ---
    fun requestDelete(file: File) {
        _fileToDelete.value = file
    }

    fun confirmDelete() {
        _fileToDelete.value?.let { file ->
            if (file.delete()) {
                loadRecordings()
                if (_currentPlayingFile.value == file) stopPlayback()
            }
        }
        _fileToDelete.value = null
    }

    fun cancelDelete() {
        _fileToDelete.value = null
    }

    override fun onCleared() {
        super.onCleared()
        recorderManager.stopRecording {  }
    }
}