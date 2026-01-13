package com.example.recorderapp.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.recorderapp.data.AudioFileModel

@Composable
fun DeleteConfirmationDialog(
    item: AudioFileModel,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Xóa bản ghi âm?", color = Color.White) },
        text = {
            Text(text = "Bạn có chắc chắn muốn xóa file '${item.name}' không?", color = Color.LightGray)
        },
        containerColor = Color(0xFF2D2D2D),
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCF6679))
            ) {
                Text("Xóa", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color.White)
            }
        }
    )
}