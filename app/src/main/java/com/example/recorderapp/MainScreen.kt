package com.example.recorderapp

import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.example.recorderapp.components.DeleteConfirmationDialog
import com.example.recorderapp.components.PlayerControls
import java.io.File

@SuppressLint("DefaultLocale")
fun formatSeconds(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}

@SuppressLint("DefaultLocale")
fun getDurationFromFile(file: File): String {
    val sampleRate = 48000

    val bytesPerSecond = sampleRate * 2

    val durationSeconds = file.length() / bytesPerSecond

    val m = durationSeconds / 60
    val s = durationSeconds % 60
    return String.format("%02d:%02d", m, s)
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
    val fileToDelete by viewModel.fileToDelete.collectAsState()

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

    if (fileToDelete != null) {
        DeleteConfirmationDialog(
            file = fileToDelete!!,
            onConfirm = { viewModel.confirmDelete() },
            onDismiss = { viewModel.cancelDelete() }
        )
    }

    // --- GIAO DIỆN CHÍNH ---
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(16.dp)
    ) {
        // --- list bản ghi ---
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
                    /*RecordingItem(
                        file = file,
                        onPlay = {viewModel.playRecording(file)},
                        onDelete = {viewModel.deleteRecording(file)}
                    )*/
                    val isCurrent = (file == currentFile)
                    val backgroundColor = if (isCurrent) Color(0xFF333333) else Color(0xFF2D2D2D)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.name, color = if(isCurrent) Color.Cyan else Color.White, fontWeight = FontWeight.Bold)
                                Text(text = getDurationFromFile(file), color = Color.Gray, fontSize = 12.sp)
                            }
                            Button(onClick = { viewModel.playRecording(file) }, modifier = Modifier.padding(end=8.dp)) {
                                Text("PLAY")
                            }
                            Button(onClick = { viewModel.requestDelete(file) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679))) {
                                Text("XÓA")
                            }
                        }
                    }
                }
            }
        }

        // --- vùng control ---
        Box(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            /*RecordControls(
                isRecording = isRecording,
                statusMessage = statusMessage,
                onToggleRecord = {viewModel.toggleRecording()}
            )*/
            // đang phát nhạc thì hiện thanh media
            if (isPlaying && currentFile != null) {
                PlayerControls(
                    fileName = currentFile!!.name,
                    progress = playbackProgress,
                    onStop = { viewModel.stopPlayback() }
                )
            } else {
                // record control
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (isRecording) {
                        Text(
                            text = formatSeconds(recordingSeconds),
                            color = Color.Red,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                    } else {
                        Text("Sẵn sàng", color = Color.Gray, fontSize = 18.sp, modifier = Modifier.padding(bottom = 24.dp))
                    }

                    Button(
                        onClick = { viewModel.toggleRecording() },
                        modifier = Modifier.size(120.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) Color(0xFFB00020) else Color(0xFF03DAC5))
                    ) {
                        Text(if (isRecording) "STOP" else "REC", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}