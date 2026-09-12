package com.example.airpods.ble

import android.util.Log
import com.example.airpods.model.AirPodsModel
import com.example.airpods.model.AirPodsStatus

object AirPodsPacketParser {

    private const val TAG = "AirPodsPacketParser"

    /**
     * Apple 제조사 데이터(0x004C) 바이트 배열을 분석하여 에어팟 상태로 변환합니다.
     * 기본형 에어팟(1세대, 2세대, 3세대) 및 프로/맥스를 폭넓게 지원합니다.
     */
    fun parse(data: ByteArray, rssi: Int): AirPodsStatus? {
        // Apple 비콘 데이터는 최소 16바이트 이상이어야 함
        if (data.size < 16) return null

        val type = data[0].toInt() and 0xFF

        // 0x07: 근접 페어링/상태 비콘, 0x10: 근처 동작 비콘 등
        // 기본형 에어팟의 다양한 펌웨어 패킷 허용 (엄격한 length 검사 제거)
        if (type != 0x07 && type != 0x10 && type != 0x05 && data.size < 24) {
            return null
        }

        // 1. 모델 판별 (Byte 3 & Byte 4)
        val modelByte = if (data.size > 3) (data[3].toInt() and 0xFF) else 0
        val model = when (modelByte) {
            0x01, 0x02 -> AirPodsModel.AIRPODS_1
            0x0F -> AirPodsModel.AIRPODS_2
            0x13 -> AirPodsModel.AIRPODS_3
            0x0E -> AirPodsModel.AIRPODS_PRO
            0x14 -> AirPodsModel.AIRPODS_PRO_2
            0x0A -> AirPodsModel.AIRPODS_MAX
            else -> AirPodsModel.UNKNOWN
        }

        // 2. 좌/우 유닛 반전 여부 플래그 (Byte 5)
        val flipByte = if (data.size > 5) (data[5].toInt() and 0xFF) else 0
        val isFlipped = (flipByte and 0x02) != 0

        // 3. 배터리 원시값 파싱 (Byte 6: 좌/우, Byte 7: 케이스)
        if (data.size < 8) return null
        val byte6 = data[6].toInt() and 0xFF
        val rawLeft = if (isFlipped) (byte6 and 0x0F) else ((byte6 ushr 4) and 0x0F)
        val rawRight = if (isFlipped) ((byte6 ushr 4) and 0x0F) else (byte6 and 0x0F)

        val byte7 = data[7].toInt() and 0xFF
        val rawCase = (byte7 ushr 4) and 0x0F

        // 4. 충전 상태 플래그 (Byte 8)
        val chargeByte = if (data.size > 8) (data[8].toInt() and 0xFF) else 0
        val isLeftCharging = (chargeByte and 0x01) != 0
        val isRightCharging = (chargeByte and 0x02) != 0
        val isCaseCharging = (chargeByte and 0x04) != 0

        // 5. 착용 감지 플래그 (Byte 9)
        val inEarByte = if (data.size > 9) (data[9].toInt() and 0xFF) else 0
        val isLeftInEar = (inEarByte and 0x01) != 0
        val isRightInEar = (inEarByte and 0x02) != 0

        val leftPerc = toPercentage(rawLeft)
        val rightPerc = toPercentage(rawRight)
        val casePerc = toPercentage(rawCase)

        // 최소 한 개 이상의 배터리 정보가 유효해야 에어팟으로 인정
        if (leftPerc == null && rightPerc == null && casePerc == null) {
            return null
        }

        Log.d(TAG, "성공적으로 파싱됨: $model | L: $leftPerc%, R: $rightPerc%, Case: $casePerc%")

        return AirPodsStatus(
            leftBattery = leftPerc,
            rightBattery = rightPerc,
            caseBattery = casePerc,
            isLeftCharging = isLeftCharging,
            isRightCharging = isRightCharging,
            isCaseCharging = isCaseCharging,
            isLeftInEar = isLeftInEar,
            isRightInEar = isRightInEar,
            model = model,
            rssi = rssi,
            lastUpdated = System.currentTimeMillis()
        )
    }

    private fun toPercentage(raw: Int): Int? {
        return when (raw) {
            in 0..10 -> (raw * 10).coerceIn(0, 100)
            else -> null // 15(0xF)이거나 연결 안 됨
        }
    }
}
