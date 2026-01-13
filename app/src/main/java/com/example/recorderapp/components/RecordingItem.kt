package com.example.recorderapp.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.recorderapp.cleanFileName
import com.example.recorderapp.data.AudioFileModel
import com.example.recorderapp.getDurationFromSize

@Composable
fun RecordingItem(
    item: AudioFileModel,
    isCurrent: Boolean,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    val backgroundColor = if (isCurrent) Color(0xFF333333) else Color(0xFF2D2D2D)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cleanFileName(item.name),
                    color = if (isCurrent) Color.Cyan else Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = getDurationFromSize(item.size),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Row {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.padding(end = 8.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("PLAY")
                }
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("XÓA")
                }
            }
        }
    }
}