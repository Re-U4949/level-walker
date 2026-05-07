package com.example.pdjissen.ui.notifications

import android.graphics.RectF
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.*

enum class GameStatus {
    PLAYING,
    GAME_OVER
}

class GameViewModel : ViewModel() {

    val characterRect = MutableLiveData<RectF>()
    val itemRects = MutableLiveData<MutableList<RectF>>()
    val obstacleRects = MutableLiveData<MutableList<RectF>>()
    val score = MutableLiveData<Int>()

    val gameStatus = MutableLiveData<GameStatus>()

    private var charVelocityY = 0f
    private val gravity = 2f
    private var isJumping = false

    private val gameScope = CoroutineScope(Dispatchers.Default)
    private var gameJob: Job? = null

    init {

        resetGame()
    }

    fun resetGame() {
        score.postValue(0)
        characterRect.postValue(RectF(100f, 600f, 200f, 700f)) // 初期位置
        itemRects.postValue(mutableListOf(
            RectF(600f, 600f, 650f, 650f),
            RectF(1200f, 600f, 1250f, 650f)
        ))
        obstacleRects.postValue(mutableListOf(
            RectF(900f, 650f, 1000f, 700f)
        ))
        gameStatus.postValue(GameStatus.PLAYING)
    }

    fun startGameLoop() {
        if (gameStatus.value == GameStatus.GAME_OVER) {
            resetGame()
        }
        gameJob?.cancel()
        gameJob = gameScope.launch {
            while (isActive && gameStatus.value == GameStatus.PLAYING) { // プレイ中のみループ
                updateGame()
                delay(16)
            }
        }
    }

    fun stopGameLoop() {
        gameJob?.cancel()
    }

    fun jump() {
        if (!isJumping && gameStatus.value == GameStatus.PLAYING) {
            charVelocityY = -35f
            isJumping = true
        }
    }

    private fun updateGame() {
        val charRect = characterRect.value ?: return

        charVelocityY += gravity
        charRect.top += charVelocityY
        charRect.bottom += charVelocityY

        if (charRect.bottom >= 700f) {
            charRect.bottom = 700f
            charRect.top = 600f
            charVelocityY = 0f
            isJumping = false
        }

        val scrollSpeed = 5f
        itemRects.value?.forEach { it.offset(-scrollSpeed, 0f) }
        obstacleRects.value?.forEach { it.offset(-scrollSpeed, 0f) }

        val items = itemRects.value ?: mutableListOf()
        val itemsIterator = items.iterator()
        while (itemsIterator.hasNext()) {
            val itemRect = itemsIterator.next()
            if (RectF.intersects(charRect, itemRect)) {
                score.postValue((score.value ?: 0) + 10)
                itemsIterator.remove()
            }
        }

        obstacleRects.value?.forEach { obstacleRect ->
            if (RectF.intersects(charRect, obstacleRect)) {
                gameStatus.postValue(GameStatus.GAME_OVER)
                stopGameLoop()
            }
        }

        characterRect.postValue(charRect)
        itemRects.postValue(items)
        obstacleRects.postValue(obstacleRects.value)
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
    }
}
