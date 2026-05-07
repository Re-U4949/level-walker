package com.example.pdjissen.ui.friend

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdjissen.data.UserDataRepository
import kotlinx.coroutines.launch

class FriendAddViewModel(private val repository: UserDataRepository) : ViewModel() {

    // 検索結果（UserStatus または null）
    private val _searchResult = MutableLiveData<UserDataRepository.UserStatus?>()
    val searchResult: LiveData<UserDataRepository.UserStatus?> = _searchResult

    // リクエスト送信状態 ("idle", "sending", "success", "failure")
    private val _requestStatus = MutableLiveData<String>("idle")
    val requestStatus: LiveData<String> = _requestStatus

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading


    fun searchUserByUid(uid: String) {
        _searchResult.value = null
        _isLoading.value = true
        _requestStatus.value = "idle"

        viewModelScope.launch {
            val user = repository.findUserByUid(uid)
            _searchResult.value = user
            _isLoading.value = false
        }
    }

    fun sendRequest(targetUid: String) {
        if (targetUid.isBlank()) return

        _requestStatus.value = "sending"
        viewModelScope.launch {
            val success = repository.sendFriendRequest(targetUid)
            _requestStatus.value = if (success) "success" else "failure"
        }
    }

    // 状態リセット
    fun resetStatus() {
        _searchResult.value = null
        _requestStatus.value = "idle"
        _isLoading.value = false
    }
}