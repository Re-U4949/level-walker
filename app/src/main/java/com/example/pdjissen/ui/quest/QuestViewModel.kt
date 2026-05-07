package com.example.pdjissen.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

enum class GameState {
    PENDING, // 開始前
    COMPLETED //完了
}

data class MiniGameQuest(
    val title: String,
    val description: String,
    val point: Int,
    var state: GameState = GameState.PENDING
)

class QuestViewModel : ViewModel() {
    private val _quest = MutableLiveData<MiniGameQuest>()
    val quest: LiveData<MiniGameQuest> get() = _quest

    init {

        loadQuest()
    }

    private fun loadQuest() {
        _quest.value = MiniGameQuest(
            title = "ミニゲームクエスト！",
            description = "キャラクターをタップしてアイテムをゲットしよう！",
            point = 10
        )
    }

    fun completeQuest() {
        val currentQuest = _quest.value
        if (currentQuest?.state == GameState.PENDING) {
            _quest.value = currentQuest.copy(state = GameState.COMPLETED)
        }
    }
}
