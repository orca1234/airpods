package com.example.airpods.model

/**
 * 에어팟 기기 모델 구분
 */
enum class AirPodsModel(val displayName: String) {
    AIRPODS_1("AirPods (1세대)"),
    AIRPODS_2("AirPods (2세대)"),
    AIRPODS_3("AirPods (3세대)"),
    AIRPODS_PRO("AirPods Pro (1세대)"),
    AIRPODS_PRO_2("AirPods Pro (2세대)"),
    AIRPODS_MAX("AirPods Max"),
    UNKNOWN("AirPods")
}

/**
 * 에어팟 실시간 배터리 및 상태 데이터
 */
data class AirPodsStatus(
    val leftBattery: Int? = null,        // 0 ~ 100% (null이면 미연결/감지불가)
    val rightBattery: Int? = null,       // 0 ~ 100%
    val caseBattery: Int? = null,        // 0 ~ 100%
    val isLeftCharging: Boolean = false,
    val isRightCharging: Boolean = false,
    val isCaseCharging: Boolean = false,
    val isLeftInEar: Boolean = false,
    val isRightInEar: Boolean = false,
    val model: AirPodsModel = AirPodsModel.UNKNOWN,
    val rssi: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val isConnected: Boolean
        get() = leftBattery != null || rightBattery != null
}
