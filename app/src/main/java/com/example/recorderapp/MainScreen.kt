package com.example.recorderapp

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MainScreen(viewModel: AudioViewModel = viewModel()) {
    val isRecording by viewModel.isRecording.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val filePath by viewModel.filePath.collectAsState()

    // --- XỬ LÝ QUYỀN (PERMISSION) ---
    var hasPermission by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
        }
    )

    // Tự động xin quyền khi màn hình khởi chạy
    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // --- GIAO DIỆN CHÍNH ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color(0xFF1E1E1E))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Voice Noise Filter",
            fontSize = 32.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(40.dp))

        if (hasPermission) {
            // Nút bấm lớn - Automotive Standard
            Button(
                onClick = { viewModel.toggleRecording() },
                modifier = Modifier
                    .size(width = 250.dp, height = 100.dp),  // Kích thước lớn dễ bấm
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRecording) Color(0xFFCF6679) else Color(0xFF03DAC5)
                )
            ) {
                Text(
                    text = if (isRecording) "DỪNG GHI" else "BẮT ĐẦU GHI",
                    fontSize = 24.sp,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = "Cần cấp quyền Microphone để sử dụng!",
                color = Color.Red,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text("Cấp quyền ngay")
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // Hiển thị trạng thái
        Text(
            text = statusMessage,
            color = Color.LightGray,
            fontSize = 18.sp
        )

        // Hiển thị đường dẫn file kết quả
        if (filePath.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("File đã lưu tại:", color = Color.Gray, fontSize = 14.sp)
                    Text(filePath, color = Color.Yellow, fontSize = 12.sp)
                }
            }
        }
    }
}