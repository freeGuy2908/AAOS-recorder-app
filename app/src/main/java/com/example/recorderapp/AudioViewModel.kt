package com.example.recorderapp

import android.app.Application
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.recorderapp.audio.AudioPlayerManager
import com.example.recorderapp.audio.AudioRecorderManager
import com.example.recorderapp.data.AudioFileModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioViewModel(application: Application) : AndroidViewModel(application) {
    private val recorderManager = AudioRecorderManager(application)
    private val playerManager = AudioPlayerManager(application)

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

    private val _currentPlayingFile = MutableStateFlow<AudioFileModel?>(null)
    val currentPlayingFile = _currentPlayingFile.asStateFlow()

    // STATE CHO XÓA
//    private val _fileToDelete = MutableStateFlow<AudioFileModel?>(null)
//    val fileToDelete = _fileToDelete.asStateFlow()

    private val _isDeleteMode = MutableStateFlow(false)
    val isDeleteMode = _isDeleteMode.asStateFlow()

    private val _selectedItems = MutableStateFlow<Set<AudioFileModel>>(emptySet())
    val selectedItems = _selectedItems.asStateFlow()


    private val _recordings = MutableStateFlow<List<AudioFileModel>>(emptyList())
    val recordings = _recordings.asStateFlow()

    private var playBackJob: Job? = null

    init {
        loadRecordings()
    }

    private fun loadRecordings() {
        viewModelScope.launch(Dispatchers.IO) {
            val fileList = mutableListOf<AudioFileModel>()

            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.SIZE
            )

            val selection = "${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?"
            val selectionArgs = arrayOf("Recordings/%")

            val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

            getApplication<Application>().contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn)
                    var size = cursor.getLong(sizeColumn)

                    // Tạo Uri cho file
                    val contentUri = Uri.withAppendedPath(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )

                    if (size <= 0) {
                        try {
                            getApplication<Application>().contentResolver.openFileDescriptor(contentUri, "r")?.use {
                                size = it.statSize
                            }
                        } catch (e: Exception) {
                            Log.e("AudioViewModel", "Không thể lấy size thực: ${e.message}")
                        }
                    }

                    fileList.add(AudioFileModel(id,name,contentUri, size = size))
                }
            }
            _recordings.value = fileList
        }
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            recorderManager.stopRecording { uri ->
                _isRecording.value = false
                _recordingDurationSeconds.value = 0
                if (uri != null) {
                    viewModelScope.launch {
                        delay(500)  // Nghỉ 0.5s để hệ thống kịp đóng file hoàn toàn
                        loadRecordings()
                    }
                }
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

    fun playRecording(item: AudioFileModel) {
        if (_currentPlayingFile.value == item && _isPlaying.value) return

        stopPlayback()

        playBackJob = viewModelScope.launch {
            try {
                _isPlaying.value = true
                _currentPlayingFile.value = item
                _statusMessage.value = "Đang phát: ${item.name}"

                playerManager.playPcmFile(item.uri) { progress ->
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
    /*fun requestDelete(item: AudioFileModel) {
        _fileToDelete.value = item
    }

    fun confirmDelete() {
        _fileToDelete.value?.let { audioModel ->
            try {
                getApplication<Application>().contentResolver.delete(
                    audioModel.uri,
                    null,
                    null
                )
                loadRecordings()
                if (_currentPlayingFile.value == audioModel) stopPlayback()
            } catch (e: Exception) {
                Log.e("Delete", "Không xóa được: ${e.message}")
            }
        }
        _fileToDelete.value = null
    }

    fun cancelDelete() {
        _fileToDelete.value = null
    }*/
    fun enableDeleteMode() {
        _isDeleteMode.value = true
        stopPlayback()
    }

    fun disableDeleteMode() {
        _isDeleteMode.value = false
        _selectedItems.value = emptySet()
    }

    fun toggleSelection(item: AudioFileModel) {
        val currentSet = _selectedItems.value.toMutableSet()
        if (currentSet.contains(item)) {
            currentSet.remove(item)
        } else {
            currentSet.add(item)
        }
        _selectedItems.value = currentSet
    }

    fun deleteSelectedFiles() {
        val itemsToDelete = _selectedItems.value
        viewModelScope.launch(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver
            itemsToDelete.forEach { item ->
                try {
                    resolver.delete(item.uri,null,null)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            loadRecordings()
            _isDeleteMode.value = false
            _selectedItems.value = emptySet()
        }
    }

    override fun onCleared() {
        super.onCleared()
        recorderManager.stopRecording {  }
    }
}