package com.example.recorderapp.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recorderapp.cleanFileName
import com.example.recorderapp.data.AudioFileModel
import com.example.recorderapp.getDurationFromSize

@Composable
fun GridRecordingItem(
    item: AudioFileModel,
    isCurrent: Boolean,
    isDeleteMode: Boolean,
    isSelected: Boolean,
    onItemClick: () -> Unit,
) {
    val borderColor = when {
        isCurrent -> Color.Cyan
        isSelected -> Color(0xFFCF6679)
        else -> Color.Transparent
    }
    val containerColor = if (isCurrent || isSelected) Color(0xFF333333) else Color(0xFF2D2D2D)

    Card(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onItemClick() }
            .border(3.dp,borderColor,RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            if (isDeleteMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Clear,
                    contentDescription = null,
                    tint = if (isSelected) Color(0xFFCF6679) else Color.Gray,
                    modifier = Modifier.align(Alignment.TopEnd).size(32.dp)
                )
            }

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = cleanFileName(item.name),
                    color = if (isCurrent) Color.Cyan else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 2,
                    lineHeight = 22.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = getDurationFromSize(item.size),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        }
    }
}