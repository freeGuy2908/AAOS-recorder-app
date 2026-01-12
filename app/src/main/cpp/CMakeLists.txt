cmake_minimum_required(VERSION 3.22.1)

project("automotive-audio-filter")

# --- CẤU HÌNH SPEEXDSP ---

# 1. Định nghĩa các cờ (Flags) cho bộ tiền xử lý
# -DFIXED_POINT: Quan trọng cho Mobile/Embedded, dùng tính toán số nguyên thay vì số thực (float) để tối ưu hiệu năng.
# -DUSE_KISS_FFT: Sử dụng thuật toán FFT tích hợp sẵn gọn nhẹ.
add_definitions(-DFIXED_POINT -DUSE_KISS_FFT -DEXPORT=)

# 2. Khai báo đường dẫn đến file Header (.h) của Speex
include_directories(speexdsp/include)

# 3. Gom tất cả các file .c trong thư mục libspeexdsp
file(GLOB SPEEX_SOURCES
        "speexdsp/libspeexdsp/*.c"
)
list(FILTER SPEEX_SOURCES EXCLUDE REGEX "test.*\\.c$")

# --- CẤU HÌNH THƯ VIỆN ---

add_library(
        native-lib
        SHARED
        native-lib.cpp
        ${SPEEX_SOURCES}
)

find_library(log-lib log)

target_link_libraries(native-lib ${log-lib})