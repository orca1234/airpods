package com.example.airpods.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.airpods.service.AirPodsMonitorService
import com.example.airpods.ui.popup.AirPodsPopupActivity
import com.example.airpods.ui.popup.AirPodsPopupContent
import com.example.airpods.ui.theme.AirPodsTheme
import com.example.airpods.ui.theme.BatteryGreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AirPodsTheme {
                MainScreen(
                    onRestartService = { restartMonitorService() },
                    onTestPopup = {
                        startActivity(Intent(this, AirPodsPopupActivity::class.java))
                    }
                )
            }
        }
    }

    private fun restartMonitorService() {
        val intent = Intent(this, AirPodsMonitorService::class.java)
        stopService(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onRestartService: () -> Unit,
    onTestPopup: () -> Unit
) {
    val context = LocalContext.current
    val status by AirPodsMonitorService.statusFlow.collectAsState()
    var permissionsGranted by remember { mutableStateOf(false) }

    // 블루투스 및 위치 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
        if (permissionsGranted) {
            onRestartService()
        }
    }

    LaunchedEffect(Unit) {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        // 안드로이드 BLE 비콘 감지에 필수적인 위치 권한 요청
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("에어팟 매니저", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onRestartService) {
                        Icon(Icons.Default.Refresh, contentDescription = "스캔 새로고침")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 상태 요약 카드
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = if (status.isConnected) BatteryGreen else MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (status.isConnected) "${status.model.displayName} 신호 수신 중!" else "기본형 에어팟 탐색 중...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Text(
                        text = if (status.isConnected)
                            "신호 세기: ${status.rssi} dBm (실시간 수신 완료)"
                        else
                            "태블릿의 '위치(GPS)'를 켜고, 에어팟 케이스 뚜껑을 연 상태로 태블릿 가까이에 두세요.",
                        fontSize = 13.sp,
                        color = if (status.isConnected) BatteryGreen else Color.Gray
                    )
                }
            }

            // 실시간 상태 표시 (Compose StateFlow 실시간 연동)
            AirPodsPopupContent(
                status = status,
                onClose = {}
            )

            Spacer(modifier = Modifier.weight(1f))

            // 스캔 새로고침 버튼
            OutlinedButton(
                onClick = onRestartService,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("블루투스 감지 새로고침 / 다시 시작")
            }

            // 팝업 테스트 버튼
            OutlinedButton(
                onClick = onTestPopup,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("iOS 팝업 화면 테스트 미리보기")
            }

            // 다른 앱 위에 그리기 권한 설정 버튼 (팝업용)
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Security, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("다른 앱 위에 표시 권한 설정 (팝업 필수)")
            }
        }
    }
}
