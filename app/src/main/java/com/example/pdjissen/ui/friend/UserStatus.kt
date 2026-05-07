package com.example.pdjissen.data

// Firestore との読み書き、アプリ内のデータ保持に用いるデータクラス
data class UserStatus(
    val uid: String = "",
    val name: String = "User Name",
    val totalSteps: Long = 0,
    val userLevel: Int = 1,

    val totalDistance: Long = 0,    // 総移動距離 (m)
    val todaySteps: Int = 0,        // 今日の歩数
    val todayDistance: Long = 0     // 今日の移動距離 (m)
)