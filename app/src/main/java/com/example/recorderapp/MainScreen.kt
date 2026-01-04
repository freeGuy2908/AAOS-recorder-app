package com.example.recorderapp

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.recorderapp.components.RecordControls
import com.example.recorderapp.components.RecordingItem

@Composable
fun MainScreen(viewModel: AudioViewModel = viewModel()) {
    val isRecording by viewModel.isRecording.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val recordings by viewModel.recordings.collectAsState()

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
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        // --- CỘT TRÁI: DANH SÁCH BẢN GHI ---
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .padding(end = 16.dp)
        ) {
            Text(
                "Danh sách ghi âm",
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn {
                items(recordings) { file ->
                    RecordingItem(
                        file = file,
                        onPlay = {viewModel.playRecording(file)},
                        onDelete = {viewModel.deleteRecording(file)}
                    )
                }
            }
        }

        // --- CỘT PHẢI: ĐIỀU KHIỂN ---
        Box(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            RecordControls(
                isRecording = isRecording,
                statusMessage = statusMessage,
                onToggleRecord = {viewModel.toggleRecording()}
            )
        }
    }
}