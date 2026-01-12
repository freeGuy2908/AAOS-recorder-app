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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class AudioRecorderManager(private val context: Context) {
    private val audioEngine = AudioEngine()
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingJob: Job? = null

    // Cấu hình âm thanh
    private val SAMPLE_RATE = 48000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

    // Speex frame size: 20ms tương ứng với 320 mẫu (samples) tại 16kHz
    // Công thức: (48000 / 1000) * 20 = 960
    private val FRAME_SIZE_SAMPLES = 960

    @SuppressLint("MissingPermission")
    fun startRecording(onStateChanged: (Boolean) -> Unit): File? {
        if (isRecording) return null

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "REC_$timeStamp.pcm"
        val currentFile = File(context.getExternalFilesDir(null), fileName)

        // buffer tối thiểu Android cần
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

        // Đảm bảo buffer của AudioRecord đủ lớn (lớn hơn frame size của Speex)
        val bufferSize = maxOf(minBufferSize, FRAME_SIZE_SAMPLES * 2)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioRecorder", "Không thể khởi tạo AudioRecord")
                return null
            }
            audioRecord?.startRecording()
            isRecording = true
            onStateChanged(true)

            // Chạy vòng lặp thu âm trên background thread (IO)
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                writeAudioDataToFile(currentFile)
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Lỗi start: ${e.message}")
            stopRecording { onStateChanged(false) }
        }

        return currentFile
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
            recordingJob?.cancel()
            audioEngine.destroyFilter()
            onStateChanged(false)
            Log.d("AudioRecorder", "Đã dừng ghi âm")
        }
    }

    private suspend fun writeAudioDataToFile(file: File) {
        // val file = File(context.getExternalFilesDir(null), "recording_filtered.pcm")
        val outputStream = FileOutputStream(file)

        // ShortArray PCM 16bit
        val shortBuffer = ShortArray(FRAME_SIZE_SAMPLES)
        Log.d("AudioRecorder", "Bắt đầu ghi vào file: ${file.absolutePath}")
        try {
            while (isRecording) {
                // Đọc dữ liệu thô từ Mic
                // read() sẽ block cho đến khi đọc đủ mẫu
                val readResult = audioRecord?.read(shortBuffer,0,FRAME_SIZE_SAMPLES) ?: 0
                if (readResult > 0 && isRecording) {
                    // gọi xuống native lib lọc nhiễu
                    audioEngine.filterNoise(shortBuffer, FRAME_SIZE_SAMPLES)

                    // PCM 16bit = 2 byte mỗi mẫu. Little Endian là chuẩn của WAV/PCM.
                    val byteBuffer = ByteBuffer.allocate(shortBuffer.size * 2)
                    byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
                    byteBuffer.asShortBuffer().put(shortBuffer)

                    outputStream.write(byteBuffer.array())
                }
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Lỗi ghi file: ${e.message}")
        } finally {
            try {
                outputStream.flush()
                outputStream.close()
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}