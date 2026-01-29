package com.example.recorderapp.audio

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
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

    private var currentFileUri: Uri? = null

    @SuppressLint("MissingPermission")
    fun startRecording(onStateChanged: (Boolean) -> Unit): Uri? {
        if (audioRecord != null) {
            try {
                audioRecord?.stop()
                audioRecord?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                audioRecord = null
            }
        }

        if (isRecording) return null

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "REC_$timeStamp.pcm"
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Recordings/")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        // Insert vào hệ thống để lấy Uri
        val contentResolver = context.contentResolver
        val uri = contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,values)
        if (uri == null) {
            Log.e("AudioRecorder", "Không thể tạo file trong MediaStore")
            return null
        }
        currentFileUri = uri

        // buffer tối thiểu Android cần
        val minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        // Đảm bảo buffer của AudioRecord đủ lớn (lớn hơn frame size của Speex)
        val bufferSize = maxOf(minBufferSize, FRAME_SIZE_SAMPLES * 2)
        //val bufferSize = (minBufferSize * 2).coerceAtLeast(FRAME_SIZE_SAMPLES * 2)

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

            // Khởi tạo Speex DSP trước khi bắt đầu ghi
            audioEngine.initFilter(FRAME_SIZE_SAMPLES, SAMPLE_RATE)

            audioRecord?.startRecording()
            isRecording = true
            onStateChanged(true)

            // Chạy vòng lặp thu âm trên background thread (IO)
            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                writeAudioDataToFile(uri)
            }
        } catch (e: Exception) {
            Log.e("AudioRecorder", "Lỗi start: ${e.message}")
            stopRecording { onStateChanged(false) }
        }

        return uri
    }

    fun stopRecording(onCompleted: (Uri?) -> Unit) {
        if (!isRecording) {
            if (audioRecord != null) {
                audioRecord?.release()
                audioRecord = null
            }
            return
        }

        isRecording = false
        val finishedUri = currentFileUri
        try {
            audioRecord?.stop()
            //audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                audioRecord?.release()
            } catch (e: Exception) { }
            audioRecord = null
            recordingJob?.invokeOnCompletion {
                audioEngine.destroyFilter()
                onCompleted(finishedUri)
                currentFileUri = null
                Log.d("AudioRecorder", "Đã dừng ghi âm và đóng luồng file")
            }
            recordingJob?.cancel()
        }
    }

    private suspend fun writeAudioDataToFile(uri: Uri) {
        val outputStream = context.contentResolver.openOutputStream(uri)
        if (outputStream == null) return

        // ShortArray PCM 16bit
        val shortBuffer = ShortArray(FRAME_SIZE_SAMPLES)

        // Buffer để chuyển đổi Short -> Byte (được tái sử dụng)
        val byteBuffer = ByteBuffer.allocate(FRAME_SIZE_SAMPLES * 2)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)

        Log.d("AudioRecorder", "Bắt đầu ghi vào file: ${uri.path}")
        try {
            while (isRecording) {
                var totalSamplesRead = 0
                var isReadError = false

                while (totalSamplesRead < FRAME_SIZE_SAMPLES && isRecording) {
                    val samplesRemain = FRAME_SIZE_SAMPLES - totalSamplesRead
                    val result = audioRecord?.read(
                        shortBuffer,
                        totalSamplesRead,
                        samplesRemain
                    ) ?: -1

                    if (result < 0) {
                        Log.e("AudioRecorder", "Lỗi đọc âm thanh: $result")
                        isReadError = true
                        break
                    }
                    totalSamplesRead += result
                }

                if (!isReadError && totalSamplesRead == FRAME_SIZE_SAMPLES) {
                    audioEngine.filterNoise(shortBuffer, FRAME_SIZE_SAMPLES)
                    
                    // Reuse byteBuffer
                    byteBuffer.clear()
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
                // Đánh dấu là đã ghi xong (IS_PENDING = 0) để app khác nhìn thấy
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(uri,values,null,null)
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}