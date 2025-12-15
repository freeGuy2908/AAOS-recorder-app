package com.example.recorderapp.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioRecorderManager(private val context: Context) {
    private val audioEngine = AudioEngine()
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null

    // Cấu hình âm thanh - BẮT BUỘC KHỚP VỚI C++
    private val SAMPLE_RATE = 16000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    // Speex frame size: 20ms tương ứng với 320 mẫu (samples) tại 16kHz
    // Công thức: (16000 / 1000) * 20 = 320
    private val FRAME_SIZE_SAMPLES = 320

    @SuppressLint("MissingPermission")  // Đã xử lý quyền ở UI layer
    fun startRecording(onStateChanged: (Boolean) -> Unit) {
        if (isRecording) return

        // 1. Tính toán buffer tối thiểu mà Android yêu cầu
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

        // Đảm bảo buffer của AudioRecord đủ lớn (lớn hơn frame size của Speex)
        val bufferSize = maxOf(minBufferSize, FRAME_SIZE_SAMPLES * 2)

        try {
            // 2. Khởi tạo AudioRecord
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "Không thể khởi tạo AudioRecord")
                return
            }
            audioRecord?.startRecording()
            isRecording = true
            onStateChanged(true)

            // 3. Chạy vòng lặp thu âm trên background thread (IO)
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                writeAudioDataToFile()
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Lỗi start: ${e.message}")
            stopRecording { onStateChanged(false) }
        }
    }

    fun stopRecording(onStateChanged: (Boolean) -> Unit) {
        if (!isRecording) return

        isRecording = false
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioRecord = null
            recordingJob?.cancel()  // Hủy coroutine
            audioEngine.destroyFilter() // Dọn dẹp bộ nhớ C++
            onStateChanged(false)
            Log.d("AudioRecorder", "Đã dừng ghi âm")
        }
    }

    private suspend fun writeAudioDataToFile() {
        // Tạo file đầu ra trong thư mục riêng của app
        val file = File(context.getExternalFilesDir(null), "recording_filtered.pcm")
        val outputStream = FileOutputStream(file)

        // Buffer chứa dữ liệu 1 frame (ShortArray vì là PCM 16bit)
        val shortBuffer = ShortArray(FRAME_SIZE_SAMPLES)
        Log.d("AudioRecorder", "Bắt đầu ghi vào file: ${file.absolutePath}")
        try {
            while (isRecording) {
                // a. Đọc dữ liệu thô từ Mic
                // read() sẽ block cho đến khi đọc đủ 320 mẫu
                val readResult = audioRecord?.read(shortBuffer,0,FRAME_SIZE_SAMPLES) ?: 0
                if (readResult > 0) {
                    // b. GỌI XUỐNG C++ ĐỂ LỌC NHIỄU
                    // Dữ liệu trong shortBuffer sẽ bị thay đổi trực tiếp (sạch hơn)
                    audioEngine.filterNoise(shortBuffer, FRAME_SIZE_SAMPLES)

                    // c. Chuyển đổi ShortArray sang ByteArray để ghi file
                    // PCM 16bit = 2 byte mỗi mẫu. Little Endian là chuẩn của WAV/PCM.
                    val byteBuffer = ByteBuffer.allocate(shortBuffer.size * 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    byteBuffer.asShortBuffer().put(shortBuffer)

                    // d. Ghi xuống file
                    outputStream.write(byteBuffer.array())
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Lỗi ghi file: ${e.message}")
        } finally {
            outputStream.close()
        }
    }
}