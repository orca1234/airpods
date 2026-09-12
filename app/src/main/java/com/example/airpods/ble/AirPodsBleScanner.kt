package com.example.airpods.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import com.example.airpods.model.AirPodsStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AirPodsBleScanner(
    private val bluetoothAdapter: BluetoothAdapter?
) {
    companion object {
        private const val TAG = "AirPodsBleScanner"
        private const val APPLE_MANUFACTURER_ID = 0x004C // 76
        private const val MIN_RSSI_THRESHOLD = -95       // 기본형 에어팟 수신율을 위해 감도 대폭 완화
    }

    private val _statusFlow = MutableStateFlow(AirPodsStatus())
    val statusFlow: StateFlow<AirPodsStatus> = _statusFlow.asStateFlow()

    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return

            // 신호 세기 검사
            if (result.rssi < MIN_RSSI_THRESHOLD) return

            val scanRecord = result.scanRecord ?: return
            val manufacturerData = scanRecord.getManufacturerSpecificData(APPLE_MANUFACTURER_ID) ?: return

            val parsedStatus = AirPodsPacketParser.parse(manufacturerData, result.rssi)
            if (parsedStatus != null && parsedStatus.isConnected) {
                Log.i(TAG, "에어팟 실시간 감지 성공! L=${parsedStatus.leftBattery}%, R=${parsedStatus.rightBattery}%, Case=${parsedStatus.caseBattery}% (RSSI: ${result.rssi}dBm)")
                _statusFlow.value = parsedStatus
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE 스캔 실패 코드: $errorCode")
            isScanning = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (isScanning || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) return

        val scanner = bluetoothAdapter.bluetoothLeScanner ?: return

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // 저지연 최고 속도 스캔
            .setReportDelay(0)
            .build()

        try {
            // 삼성 갤탭 기기 호환성을 위해 필터 없이 소프트웨어 필터링(null 필터) 적용
            scanner.startScan(null, settings, scanCallback)
            isScanning = true
            Log.i(TAG, "에어팟 BLE 스캐너 가동 시작 (전체 수신 모드)")
        } catch (e: Exception) {
            Log.e(TAG, "스캔 시작 예외 발생", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!isScanning || bluetoothAdapter == null) return
        val scanner = bluetoothAdapter.bluetoothLeScanner ?: return

        try {
            scanner.stopScan(scanCallback)
            isScanning = false
            Log.i(TAG, "에어팟 BLE 스캐너 중지됨")
        } catch (e: Exception) {
            Log.e(TAG, "스캔 중지 예외 발생", e)
        }
    }
}
