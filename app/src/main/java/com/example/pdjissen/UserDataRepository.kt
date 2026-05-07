package com.example.pdjissen.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class UserDataRepository {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val usersCollection = db.collection("users")
    private val requestsCollection = db.collection("friendRequests")

    // データクラス定義
    data class UserStatus(
        val uid: String = "",
        val name: String = "ゲスト",
        val totalSteps: Long = 0,
        val userLevel: Int = 1,
        val totalDistance: Double = 0.0,
        val todaySteps: Int = 0,
        val todayDistance: Long = 0,
        val friends: Map<String, Boolean> = emptyMap()
    )

    data class FriendRequest(
        val requestId: String = "",
        val senderUid: String = "",
        val receiverUid: String = "",
        val status: String = "" // "pending", "accepted", "rejected"
    )

    // =================================================================
    //  1. データの監視・取得 (Read)
    // =================================================================

    // Firestore のリアルタイム監視を開始
    fun listenToUserStatus(onUpdate: (UserStatus) -> Unit): ListenerRegistration? {
        val uid = auth.currentUser?.uid ?: return null

        return usersCollection.document(uid).addSnapshotListener { snapshot, e ->
            if (e != null) return@addSnapshotListener

            if (snapshot != null && snapshot.exists()) {
                val status = snapshot.toObject(UserStatus::class.java)
                // uid と name を補完
                status?.let {
                    onUpdate(it.copy(uid = uid, name = snapshot.getString("name") ?: "あなた"))
                }
            } else {
                // データがない場合は初期値を返す
                onUpdate(UserStatus(uid = uid))
            }
        }
    }

    // 単発でのデータ取得
    suspend fun getUserStatus(): UserStatus? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            val snapshot = usersCollection.document(uid).get().await()
            snapshot.toObject(UserStatus::class.java)?.copy(
                uid = uid,
                name = snapshot.getString("name") ?: "あなた"
            )
        } catch (e: Exception) {
            null
        }
    }

    // 全ユーザー取得（ランキング用）
    suspend fun getAllUsers(): List<UserStatus> {
        return try {
            val snapshot = usersCollection
                .orderBy("totalSteps", Query.Direction.DESCENDING)
                .get().await()

            snapshot.documents.mapNotNull { document ->
                val status = document.toObject(UserStatus::class.java)
                status?.copy(
                    uid = document.id,
                    name = document.getString("name") ?: "User-${document.id.substring(0, 4)}"
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // =================================================================
    //  2. データの更新 (Write)
    // =================================================================

    // 歩数とレベルを加算
    suspend fun addStepsAndLevel(stepsToAdd: Int, newLevel: Int) {
        val uid = auth.currentUser?.uid ?: return

        // 歩数は increment、レベルは上書き
        val updateData = hashMapOf<String, Any>(
            "totalSteps" to FieldValue.increment(stepsToAdd.toLong()),
            "userLevel" to newLevel
        )
        try {
            usersCollection.document(uid).set(updateData, SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 距離を加算
    suspend fun addDistance(distanceToAdd: Double) {
        val uid = auth.currentUser?.uid ?: return

        val updateData = hashMapOf<String, Any>(
            "totalDistance" to FieldValue.increment(distanceToAdd),
            "todayDistance" to FieldValue.increment(distanceToAdd.toLong())
        )
        try {
            usersCollection.document(uid).set(updateData, SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 複数フィールドをまとめて更新
    suspend fun updateStatus(
        newTotalSteps: Long,
        newLevel: Int,
        newTotalDistance: Long,
        newTodaySteps: Int,
        newTodayDistance: Long
    ) {
        val uid = auth.currentUser?.uid ?: return

        val updateData = mapOf(
            "totalSteps" to newTotalSteps,
            "userLevel" to newLevel,
            "totalDistance" to newTotalDistance,
            "todaySteps" to newTodaySteps,
            "todayDistance" to newTodayDistance
        )
        try {
            usersCollection.document(uid).set(updateData, SetOptions.merge()).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // デバッグ用リセット
    suspend fun resetStatus() {
        val uid = auth.currentUser?.uid ?: return
        try {
            usersCollection.document(uid).set(UserStatus(uid = uid)).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // =================================================================
    //  3. フレンド機能 (Friend Logic)
    // =================================================================

    // ユーザー検索
    suspend fun findUserByUid(targetUid: String): UserStatus? {
        val currentUid = auth.currentUser?.uid
        if (targetUid.isBlank() || targetUid == currentUid) return null

        return try {
            val snapshot = usersCollection.document(targetUid).get().await()
            if (snapshot.exists()) {
                snapshot.toObject(UserStatus::class.java)?.copy(uid = targetUid)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // フレンドリクエスト送信
    suspend fun sendFriendRequest(receiverUid: String): Boolean {
        val senderUid = auth.currentUser?.uid ?: return false

        // 重複チェック
        val existingRequests = requestsCollection
            .whereIn("senderUid", listOf(senderUid, receiverUid))
            .whereIn("receiverUid", listOf(senderUid, receiverUid))
            .whereEqualTo("status", "pending")
            .get().await()

        if (!existingRequests.isEmpty) return false

        val request = FriendRequest(
            senderUid = senderUid,
            receiverUid = receiverUid,
            status = "pending"
        )

        return try {
            requestsCollection.add(request).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // 自分宛ての保留リクエスト取得
    suspend fun getPendingRequests(): List<FriendRequest> {
        val currentUid = auth.currentUser?.uid ?: return emptyList()

        return try {
            val snapshot = requestsCollection
                .whereEqualTo("receiverUid", currentUid)
                .whereEqualTo("status", "pending")
                .get().await()

            snapshot.documents.mapNotNull { document ->
                document.toObject(FriendRequest::class.java)?.copy(requestId = document.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // リクエスト承認
    suspend fun acceptFriendRequest(request: FriendRequest): Boolean {
        val currentUid = auth.currentUser?.uid ?: return false
        if (request.receiverUid != currentUid) return false

        val friendUid = request.senderUid
        val batch = db.batch()

        // 1. ステータス更新
        val requestRef = requestsCollection.document(request.requestId)
        batch.update(requestRef, "status", "accepted")

        // 2. 相互にフレンド追加
        val currentUserRef = usersCollection.document(currentUid)
        batch.set(currentUserRef, mapOf("friends" to mapOf(friendUid to true)), SetOptions.merge())

        val friendUserRef = usersCollection.document(friendUid)
        batch.set(friendUserRef, mapOf("friends" to mapOf(currentUid to true)), SetOptions.merge())

        return try {
            batch.commit().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // リクエスト拒否
    suspend fun rejectFriendRequest(request: FriendRequest): Boolean {
        val currentUid = auth.currentUser?.uid ?: return false
        if (request.receiverUid != currentUid) return false

        return try {
            requestsCollection.document(request.requestId)
                .update("status", "rejected")
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    // 表示名のみ更新
    suspend fun updateName(newName: String) {
        val uid = auth.currentUser?.uid ?: return
        try {
            usersCollection.document(uid).update("name", newName).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}