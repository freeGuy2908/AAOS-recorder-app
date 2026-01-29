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
Java_com_example_recorderapp_audio_AudioEngine_initFilter(JNIEnv *env, jobject thiz, jint frame_size, jint sample_rate) {
    if (st != nullptr) {
        speex_preprocess_state_destroy(st);
        st = nullptr;
    }

    st = speex_preprocess_state_init(frame_size, sample_rate);
    if (st == nullptr) {
        return JNI_FALSE;
    }

    // Bật tính năng khử nhiễu (Denoise)
    int on = 1;
    speex_preprocess_ctl(st, SPEEX_PREPROCESS_SET_DENOISE, &on);

    // Tùy chỉnh mức độ giảm nhiễu (Noise Suppression) - Đơn vị dB âm
    // -15dB đến -30dB là mức phổ biến
    int noiseSuppress = -20;
    speex_preprocess_ctl(st, SPEEX_PREPROCESS_SET_NOISE_SUPPRESS, &noiseSuppress);

    // Bật tính năng tự động điều chỉnh gain (AGC) - Giúp âm thanh to đều
    speex_preprocess_ctl(st, SPEEX_PREPROCESS_SET_AGC, &on);

    LOGD("Speex Initialized: Size=%d, Rate=%d", frame_size, sample_rate);
    return JNI_TRUE;
}

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

    // Nếu bộ lọc chưa khởi tạo, bỏ qua xử lý (hoặc trả về false tùy logic)
    if (st == nullptr) {
        // Tùy chọn: Log warning
        // LOGD("Speex filter not initialized, skipping processing");
        env->ReleaseShortArrayElements(input_buffer, audioData, 0);
        return JNI_FALSE; 
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