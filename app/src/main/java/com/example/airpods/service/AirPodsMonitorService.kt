package com.example.airpods.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.airpods.ble.AirPodsBleScanner
import com.example.airpods.model.AirPodsStatus
import com.example.airpods.ui.MainActivity
import com.example.airpods.ui.popup.AirPodsPopupActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AirPodsMonitorService : Service() {

    companion object {
        const val CHANNEL_ID = "airpods_monitor_channel"
        const val NOTIFICATION_ID = 1001

        private val _statusFlow = MutableStateFlow(AirPodsStatus())
        val statusFlow: StateFlow<AirPodsStatus> = _statusFlow.asStateFlow()

        var latestStatus: AirPodsStatus
            get() = _statusFlow.value
            set(value) { _statusFlow.value = value }
    }

    private var scanner: AirPodsBleScanner? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var lastPopupTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("에어팟 감지 대기 중...", "에어팟 뚜껑을 열거나 연결해 주세요."))

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        scanner = AirPodsBleScanner(bluetoothManager.adapter)
        scanner?.startScan()

        // 상태 수신 및 알림/팝업/UI 업데이트
        serviceScope.launch {
            scanner?.statusFlow?.collectLatest { status ->
                if (status.isConnected) {
                    _statusFlow.value = status
                    updateNotification(status)
                    checkAndShowPopup(status)
                }
            }
        }
    }

    private fun checkAndShowPopup(status: AirPodsStatus) {
        val currentTime = System.currentTimeMillis()
        // 너무 빈번한 팝업 방지 (최소 20초 간격)
        if (currentTime - lastPopupTime > 20_000) {
            lastPopupTime = currentTime
            val popupIntent = Intent(this, AirPodsPopupActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(popupIntent)
        }
    }

    @SuppressLint("NotificationPermission")
    private fun updateNotification(status: AirPodsStatus) {
        val title = "${status.model.displayName} 배터리"
        val left = status.leftBattery?.let { "$it%" } ?: "-"
        val right = status.rightBattery?.let { "$it%" } ?: "-"
        val case = status.caseBattery?.let { "$it%" } ?: "-"

        val content = "L: $left ${if (status.isLeftCharging) "⚡" else ""} | R: $right ${if (status.isRightCharging) "⚡" else ""} | Case: $case ${if (status.isCaseCharging) "⚡" else ""}"

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "에어팟 배터리 모니터",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "에어팟 실시간 배터리 표시 및 감지 서비스"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        scanner?.stopScan()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
