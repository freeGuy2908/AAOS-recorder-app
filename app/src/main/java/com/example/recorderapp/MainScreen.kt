package com.example.recorderapp

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.recorderapp.components.ControlMenu
import com.example.recorderapp.components.GridRecordingItem
import com.example.recorderapp.components.PlayerControls

@SuppressLint("DefaultLocale")
fun formatSeconds(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}

@SuppressLint("DefaultLocale")
fun getDurationFromSize(size: Long): String {
    val sampleRate = 48000
    val bytesPerSecond = sampleRate * 2
    val durationSeconds = size / bytesPerSecond

    val m = durationSeconds / 60
    val s = durationSeconds % 60
    return String.format("%02d:%02d", m, s)
}

fun cleanFileName(originalName: String): String {
    return originalName
        .replace(".pcm.mp3", "")
        .replace(".mp3", "")
        .replace(".wav", "")
        .replace(".pcm", "")
}

@Composable
fun MainScreen(viewModel: AudioViewModel = viewModel()) {
    val isRecording by viewModel.isRecording.collectAsState()
    //val statusMessage by viewModel.statusMessage.collectAsState()
    val recordings by viewModel.recordings.collectAsState()
    val recordingSeconds by viewModel.recordingDurationSeconds.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackProgress by viewModel.playbackProgress.collectAsState()
    val currentFile by viewModel.currentPlayingFile.collectAsState()
    //val fileToDelete by viewModel.fileToDelete.collectAsState()
    val isDeleteMode by viewModel.isDeleteMode.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()


    // --- XỬ LÝ QUYỀN (PERMISSION) ---
    var hasPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasPermission = isGranted
        }
    )
    LaunchedEffect(Unit) {  // Tự động xin quyền khi màn hình khởi chạy
        launcher.launch(Manifest.permission.RECORD_AUDIO)
    }

    /*if (fileToDelete != null) {
        DeleteConfirmationDialog(
            item = fileToDelete!!,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.cancelDelete() }
        )
    }*/

    // --- GIAO DIỆN CHÍNH ---
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        // Vung control
        Box(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            if (!isDeleteMode && !isRecording) {
                ControlMenu(
                    modifier = Modifier.align(Alignment.TopStart),
                    onDeleteOptionClick = { viewModel.enableDeleteMode() }
                )
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isDeleteMode) {
                    // --- GIAO DIỆN KHI ĐANG CHỌN XÓA ---
                    Text(
                        text = "Đã chọn: ${selectedItems.size}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Nút XÁC NHẬN XÓA
                    Button(
                        onClick = { viewModel.deleteSelectedFiles() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679)),
                        enabled = selectedItems.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(70.dp)
                    ) {
                        Text("XÓA", fontSize = 20.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Nút HỦY
                    TextButton(onClick = { viewModel.disableDeleteMode() }) {
                        Text("Hủy", color = Color.Gray, fontSize = 20.sp)
                    }

                } else if (isPlaying && currentFile != null) {
                    // --- MEDIA PLAYER ---
                    PlayerControls(
                        fileName = cleanFileName(currentFile!!.name),
                        progress = playbackProgress,
                        onStop = { viewModel.stopPlayback() }
                    )
                } else {
                    // --- GHI ÂM (MẶC ĐỊNH) ---
                    if (isRecording) {
                        Text(
                            text = formatSeconds(recordingSeconds),
                            color = Color.Red,
                            fontSize = 60.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 32.dp)
                        )
                    } else {
                        Text("Sẵn sàng", color = Color.Gray, fontSize = 24.sp, modifier = Modifier.padding(bottom = 32.dp))
                    }

                    Button(
                        onClick = {
                            if (hasPermission) viewModel.toggleRecording()
                            else launcher.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        modifier = Modifier.size(220.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color(0xFFB00020) else Color(0xFF03DAC5)
                        )
                    ) {
                        Text(
                            text = if (isRecording) "STOP" else "REC",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isRecording) Color.White else Color.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // --- grid ban ghi ---
        Column(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
        ) {
            Text(
                text = if (isDeleteMode) "Chọn bản ghi để xóa" else "Bản ghi âm",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            if (recordings.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Trống", color = Color.Gray, fontSize = 20.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(recordings) { item ->
                        GridRecordingItem(
                            item = item,
                            isCurrent = (item == currentFile),
                            isDeleteMode = isDeleteMode,
                            isSelected = selectedItems.contains(item),
                            onItemClick = {
                                if (isDeleteMode) {
                                    viewModel.toggleSelection(item)
                                } else {
                                    viewModel.playRecording(item)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}