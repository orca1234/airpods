package com.example.airpods.ble

import com.example.airpods.model.AirPodsModel
import com.example.airpods.model.AirPodsStatus

object AirPodsPacketParser {

    private const val APPLE_BEACON_TYPE_AIRPODS: Byte = 0x07
    private const val EXPECTED_PAYLOAD_LENGTH: Byte = 0x19

    /**
     * Apple 제조사 데이터(0x004C) 바이트 배열을 분석하여 에어팟 상태로 변환합니다.
     */
    fun parse(data: ByteArray, rssi: Int): AirPodsStatus? {
        // 유효한 에어팟 비콘 패킷은 최소 25바이트 이상이어야 합니다.
        if (data.size < 24) return null

        val type = data[0]
        val length = data[1]

        // 0x07(Nearby Action / Proximity Pairing) 타입 검증
        if (type != APPLE_BEACON_TYPE_AIRPODS || length != EXPECTED_PAYLOAD_LENGTH) {
            return null
        }

        // 1. 모델 판별 (Byte 3 & Byte 4)
        val modelByte = data[3].toInt() and 0xFF
        val model = when (modelByte) {
            0x02 -> AirPodsModel.AIRPODS_1
            0x0F -> AirPodsModel.AIRPODS_2
            0x13 -> AirPodsModel.AIRPODS_3
            0x0E -> AirPodsModel.AIRPODS_PRO
            0x14 -> AirPodsModel.AIRPODS_PRO_2
            0x0A -> AirPodsModel.AIRPODS_MAX
            else -> AirPodsModel.UNKNOWN
        }

        // 2. 좌/우 유닛 반전 여부 플래그 (L/R Flip)
        val flipByte = (data[5].toInt() and 0xFF)
        val isFlipped = (flipByte and 0x02) != 0

        // 3. 배터리 원시값 파싱 (0~10: 10% 단위 배터리, 15: 미연결/알 수 없음)
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

        // 5. 착용 감지 플래그 (In-Ear Status)
        val inEarByte = if (data.size > 9) (data[9].toInt() and 0xFF) else 0
        val isLeftInEar = (inEarByte and 0x01) != 0
        val isRightInEar = (inEarByte and 0x02) != 0

        return AirPodsStatus(
            leftBattery = toPercentage(rawLeft),
            rightBattery = toPercentage(rawRight),
            caseBattery = toPercentage(rawCase),
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

    /**
     * 4-bit 원시 수치(0~10, 15)를 백분율(0~100%)로 변환
     */
    private fun toPercentage(raw: Int): Int? {
        return when (raw) {
            in 0..10 -> (raw * 10).coerceIn(0, 100)
            else -> null // 15(0xF)이거나 범위를 벗어나면 연결되지 않음으로 처리
        }
    }
}
