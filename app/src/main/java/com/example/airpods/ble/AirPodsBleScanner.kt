package com.example.airpods.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
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
        private const val MIN_RSSI_THRESHOLD = -80       // 신호가 너무 약한 타인의 에어팟 배제
    }

    private val _statusFlow = MutableStateFlow(AirPodsStatus())
    val statusFlow: StateFlow<AirPodsStatus> = _statusFlow.asStateFlow()

    private var isScanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result ?: return

            // RSSI 신호 세기 필터링
            if (result.rssi < MIN_RSSI_THRESHOLD) return

            val scanRecord = result.scanRecord ?: return
            val manufacturerData = scanRecord.getManufacturerSpecificData(APPLE_MANUFACTURER_ID) ?: return

            val parsedStatus = AirPodsPacketParser.parse(manufacturerData, result.rssi)
            if (parsedStatus != null && parsedStatus.isConnected) {
                Log.d(TAG, "AirPods 감지: Left=${parsedStatus.leftBattery}%, Right=${parsedStatus.rightBattery}%, Case=${parsedStatus.caseBattery}%")
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

        // Apple 기기 패킷만 스캔하도록 필터 설정
        val filter = ScanFilter.Builder()
            .setManufacturerData(APPLE_MANUFACTURER_ID, byteArrayOf())
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // 빠른 감지를 위해 저지연 모드 사용
            .setReportDelay(0)
            .build()

        try {
            scanner.startScan(listOf(filter), settings, scanCallback)
            isScanning = true
            Log.i(TAG, "에어팟 BLE 스캐너 시작됨")
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
