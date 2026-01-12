package com.example.recorderapp.audio

class AudioEngine {
    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }

    /**
     * Hàm native lọc nhiễu.
     * @param inputBuffer: Mảng âm thanh thô (PCM) từ Mic
     * @param size: Kích thước dữ liệu cần xử lý
     * @return: Trả về true nếu xử lý thành công (dữ liệu đã lọc được ghi đè vào inputBuffer)
     */
    external fun filterNoise(inputBuffer: ShortArray, size: Int): Boolean

    external fun destroyFilter()
}