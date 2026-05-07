package com.example.pdjissen.ui.friend

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdjissen.data.UserDataRepository
import kotlinx.coroutines.launch

// UI 表示用に senderName を加えた拡張データクラス
data class DisplayRequest(
    val request: UserDataRepository.FriendRequest,
    val senderName: String,
    val requestId: String = request.requestId
)

class FriendRequestViewModel(private val repository: UserDataRepository) : ViewModel() {

    private val _pendingRequests = MutableLiveData<List<DisplayRequest>>()
    val pendingRequests: LiveData<List<DisplayRequest>> = _pendingRequests

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadPendingRequests()
    }

    // 保留中リクエストと送信者名のロード
    fun loadPendingRequests() {
        if (_isLoading.value == true) return
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val requests = repository.getPendingRequests()

                val displayRequests = requests.mapNotNull { request ->
                    val senderStatus: UserDataRepository.UserStatus? = repository.findUserByUid(request.senderUid)

                    if (senderStatus != null) {
                        DisplayRequest(request, senderStatus.name)
                    } else {
                        // 該当ユーザーが取得できない場合は除外
                        null
                    }
                }

                _pendingRequests.value = displayRequests
            } catch (e: Exception) {
                _pendingRequests.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 承認
    fun acceptRequest(request: UserDataRepository.FriendRequest) {
        viewModelScope.launch {
            val success = repository.acceptFriendRequest(request)
            if (success) {
                loadPendingRequests()
            }
        }
    }

    // 拒否
    fun rejectRequest(request: UserDataRepository.FriendRequest) {
        viewModelScope.launch {
            val success = repository.rejectFriendRequest(request)
            if (success) {
                loadPendingRequests()
            }
        }
    }
}