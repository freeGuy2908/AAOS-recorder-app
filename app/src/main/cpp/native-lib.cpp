#include <jni.h>
#include <string>
#include <android/log.h>
#include "speex/speex_preprocess.h"

// Định nghĩa TAG để in log
#define TAG "NativeAudio"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

// Biến toàn cục lưu trạng thái bộ lọc
static SpeexPreprocessState *st = nullptr;
//static int current_sample_rate = 0;
//static int current_frame_size = 0;

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_recorderapp_audio_AudioEngine_filterNoise(
        JNIEnv *env,
        jobject /* this */,
        jshortArray input_buffer,
        jint size) {

    // 1. Lấy con trỏ trỏ tới mảng dữ liệu từ Java
    jshort *audioData = env->GetShortArrayElements(input_buffer, NULL);
    if (audioData == NULL) {
        return JNI_FALSE;   // Không lấy được bộ nhớ
    }

    // ---------------- CẤU HÌNH SPEEX (CHẠY 1 LẦN ĐẦU) ----------------
    // Giả định frame size là kích thước buffer truyền xuống (thường là 10ms hoặc 20ms âm thanh)
    if (st == nullptr) {
        // Cấu hình mẫu: Sample rate 16000Hz (chuẩn cho giọng nói)
        // cần đảm bảo AudioRecord bên Kotlin cũng set là 16000Hz
        int sampleRate = 48000;
        st = speex_preprocess_state_init(size, sampleRate);

        // Bật tính năng khử nhiễu (Denoise)
        int on = 1;
        speex_preprocess_ctl(st, SPEEX_PREPROCESS_SET_DENOISE, &on);

        // Tùy chỉnh mức độ giảm nhiễu (Noise Suppression) - Đơn vị dB âm
        // -15dB đến -30dB là mức phổ biến
        int noiseSuppress = -20;
        speex_preprocess_ctl(st, SPEEX_PREPROCESS_SET_NOISE_SUPPRESS, &noiseSuppress);

        // Bật tính năng tự động điều chỉnh gain (AGC) - Giúp âm thanh to đều
        speex_preprocess_ctl(st, SPEEX_PREPROCESS_SET_AGC, &on);

        LOGD("Speex Intialized: Size=%d, Rate=%d", size, sampleRate);
    }

    // ---------------- XỬ LÝ LỌC NHIỄU ----------------
    // Hàm này thực hiện lọc nhiễu và GHI ĐÈ kết quả vào chính mảng audioData
    speex_preprocess_run(st, audioData);

    // ---------------------------------------------------------------------------

    // 2. Release con trỏ để cập nhật dữ liệu ngược lại cho mảng bên Java
    // 0 có nghĩa là copy dữ liệu đã sửa về mảng gốc trong Java và giải phóng bộ nhớ C++
    env->ReleaseShortArrayElements(input_buffer, audioData, 0);

    return JNI_TRUE;
}

// Hàm dọn dẹp bộ nhớ khi tắt app (Cần thêm vào AudioEngine.kt bên Kotlin để gọi lúc onDestroy)
extern "C"
JNIEXPORT void JNICALL
Java_com_example_recorderapp_audio_AudioEngine_destroyFilter(
        JNIEnv *env,
        jobject /* this */) {
    if (st != nullptr) {
        speex_preprocess_state_destroy(st);
        st = nullptr;
        LOGD("Speex Destroyed");
    }
}