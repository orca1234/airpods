package com.example.airpods.ui.popup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airpods.model.AirPodsStatus
import com.example.airpods.service.AirPodsMonitorService
import com.example.airpods.ui.theme.AirPodsTheme
import com.example.airpods.ui.theme.BatteryGreen

class AirPodsPopupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AirPodsTheme {
                val status by AirPodsMonitorService.statusFlow.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            finish()
                        },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    AirPodsPopupContent(
                        status = status,
                        onClose = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun AirPodsPopupContent(
    status: AirPodsStatus,
    onClose: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 헤더: 모델명 및 닫기 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = status.model.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "닫기")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3단 배터리 표시기: Left, Case, Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BatteryItem(
                    title = "왼쪽 유닛",
                    icon = Icons.Default.Headphones,
                    battery = status.leftBattery,
                    isCharging = status.isLeftCharging
                )
                BatteryItem(
                    title = "충전 케이스",
                    icon = Icons.Default.Inventory2,
                    battery = status.caseBattery,
                    isCharging = status.isCaseCharging
                )
                BatteryItem(
                    title = "오른쪽 유닛",
                    icon = Icons.Default.Headphones,
                    battery = status.rightBattery,
                    isCharging = status.isRightCharging
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 하단 연결 상태 메시지
            if (status.isConnected) {
                Text(
                    text = "● 실시간 연결됨 (신호강도: ${status.rssi} dBm)",
                    color = BatteryGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "○ 에어팟 뚜껑을 열어 신호를 보내주세요",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@Composable
fun BatteryItem(
    title: String,
    icon: ImageVector,
    battery: Int?,
    isCharging: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }

        Text(text = title, fontSize = 12.sp, color = Color.Gray)

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = battery?.let { "$it%" } ?: "-",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            if (isCharging) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "충전 중",
                    tint = BatteryGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
