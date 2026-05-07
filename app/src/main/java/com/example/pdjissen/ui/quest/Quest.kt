package com.example.pdjissen

// QuestTypes.kt で定義する enum を先に参照
import com.example.pdjissen.QuestTypes

data class Quest(
    val id: Int,
    val title: String,
    val description: String,
    val type: QuestTypes,
    val point: Int
)
    