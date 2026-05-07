package com.example.pdjissen.ui.friend

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pdjissen.data.UserDataRepository
import kotlinx.coroutines.launch

class FriendViewModel(private val repository: UserDataRepository) : ViewModel() {

    private val _friendList = MutableLiveData<List<UserDataRepository.UserStatus>>()
    val friendList: LiveData<List<UserDataRepository.UserStatus>> = _friendList

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadFriendList()
    }

    fun loadFriendList() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val users = repository.getAllUsers()
                _friendList.value = users
            } catch (e: Exception) {
                _friendList.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}