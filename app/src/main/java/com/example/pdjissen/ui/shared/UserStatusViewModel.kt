package com.example.pdjissen.ui.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.example.pdjissen.data.UserDataRepository
import kotlinx.coroutines.launch

class UserStatusViewModel : ViewModel() {

    private val repository = UserDataRepository()

    // 画面表示用 LiveData
    private val _userStatus = MutableLiveData<UserDataRepository.UserStatus>()
    val userStatus: LiveData<UserDataRepository.UserStatus> = _userStatus

    // 1000 歩ごとの進捗
    val levelProgress: LiveData<Int> = userStatus.map { status ->
        (status.totalSteps % 1000).toInt()
    }

    // 次レベルまでの残り歩数
    val stepsUntilNextLevel: LiveData<Int> = userStatus.map { status ->
        (1000 - (status.totalSteps % 1000)).toInt()
    }

    init {
        startListening()
    }

    // Firestore のリアルタイム監視を開始
    fun startListening() {
        repository.listenToUserStatus { newStatus ->
            _userStatus.value = newStatus
        }
    }

    // 歩数更新
    fun updateSteps(stepsToAdd: Int) {
        if (stepsToAdd <= 0) return

        viewModelScope.launch {
            val currentStatus = _userStatus.value ?: UserDataRepository.UserStatus()

            // レベル算出
            val nextTotalSteps = currentStatus.totalSteps + stepsToAdd
            val nextLevel = (nextTotalSteps / 1000).toInt() + 1

            repository.addStepsAndLevel(stepsToAdd, nextLevel)
        }
    }

    fun changeName(newName: String) {
        if (newName.isBlank()) return

        // 即時反映
        val current = _userStatus.value ?: UserDataRepository.UserStatus()
        _userStatus.value = current.copy(name = newName)

        // Firestore へ反映
        viewModelScope.launch {
            repository.updateName(newName)
        }
    }

    // 距離更新
    fun updateDistance(distanceToAdd: Float) {
        if (distanceToAdd <= 0f) return

        // 即時反映でラグを抑える
        val current = _userStatus.value ?: UserDataRepository.UserStatus()
        val nextTotalDist = current.totalDistance + distanceToAdd.toDouble()
        val nextTodayDist = current.todayDistance + distanceToAdd.toLong()

        _userStatus.value = current.copy(
            totalDistance = nextTotalDist,
            todayDistance = nextTodayDist
        )

        viewModelScope.launch {
            repository.addDistance(distanceToAdd.toDouble())
        }
    }

    // 歩数・距離を同時に更新する用
    fun updateStatus(stepsToAdd: Int, distanceToAdd: Long) {
        viewModelScope.launch {
            repository.addStepsAndLevel(stepsToAdd, 0) // レベル計算省略
            repository.addDistance(distanceToAdd.toDouble())
        }
    }

    // デバッグ用リセット
    fun resetData() {
        viewModelScope.launch {
            repository.resetStatus()
        }
    }

    // フレンドリスト LiveData
    private val _friendList = MutableLiveData<List<UserDataRepository.UserStatus>>()
    val friendList: LiveData<List<UserDataRepository.UserStatus>> = _friendList

    // フレンドの最新データを取得
    fun loadFriends() {
        viewModelScope.launch {
            val current = _userStatus.value ?: return@launch
            val friendUids = current.friends.keys.toList()

            val loadedFriends = mutableListOf<UserDataRepository.UserStatus>()

            for (uid in friendUids) {
                val friendData = repository.findUserByUid(uid)
                if (friendData != null) {
                    loadedFriends.add(friendData)
                }
            }

            _friendList.value = loadedFriends
        }
    }
}