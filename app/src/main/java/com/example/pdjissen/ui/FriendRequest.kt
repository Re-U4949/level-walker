package com.example.pdjissen.data

/**
 * フレンドリクエストの状態をFirestoreで管理するためのデータモデル
 */
data class FriendRequest(
    // FirestoreのドキュメントID
    val requestId: String = "",

    val senderUid: String = "",        // 送信元 UID
    val receiverUid: String = "",      // 受信先 UID
    val status: String = "pending",    // "pending" / "accepted" / "rejected"
    val timestamp: Long = System.currentTimeMillis()
)