package com.example.recorderapp.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun RecordingItem(
    file: File,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Tên file
            Column(modifier = Modifier.weight(1f)) {
                Text(text = file.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(text = "${file.length() / 1024} KB", color = Color.Gray, fontSize = 12.sp)
            }

            // Nút Play
            Button(
                onClick = onPlay,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03DAC5)),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("PLAY", color = Color.Black)
            }

            // Nút Xóa
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679))
            ) {
                Text("XÓA")
            }
        }
    }
}