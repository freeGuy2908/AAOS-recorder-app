package com.example.recorderapp.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

class AudioPlayerManager {
    private val SAMPLE_RATE = 48000
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    private val BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG,AUDIO_FORMAT)

    suspend fun playPcmFile(file: File) = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AUDIO_FORMAT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(CHANNEL_CONFIG)
                    .build()
            )
            .setBufferSizeInBytes(BUFFER_SIZE)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack.play()

        val inputStream = FileInputStream(file)
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                audioTrack.write(buffer,0,bytesRead)
            }
            val silenceBuffer = ByteArray(BUFFER_SIZE)
            for (i in 0 until 5) {
                audioTrack.write(silenceBuffer,0,silenceBuffer.size)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            inputStream.close()
        }
    }
}