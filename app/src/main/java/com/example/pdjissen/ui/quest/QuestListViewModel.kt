package com.example.pdjissen.ui.quest

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.example.pdjissen.Quest
import com.example.pdjissen.QuestTypes

class QuestListViewModel : ViewModel() {

    // 全クエストのマスターデータ
    private val allQuestsData: List<Quest> = listOf(
        Quest(1, "デイリー: ログインボーナス", "1日1回ログインする", QuestTypes.DAY, 1),
        Quest(2, "デイリー: 4km歩く", "1日で合計4km歩く", QuestTypes.DAY, 1),
        Quest(101, "ウィークリー: 28km歩く", "1週間で合計28km歩く", QuestTypes.WEEK, 7),
        Quest(201, "イベント: 近日公開", "ただいま検討中", QuestTypes.EVENT, 0)
    )
    private val allQuests = MutableLiveData(allQuestsData)

    /**
     * 指定タイプでフィルタした LiveData を返す。
     * タブごとに独立した LiveData を観測することで、相互干渉を防ぐ。
     *
     * @param type 表示するクエスト種別 (DAY, WEEK, EVENT)
     * @return 該当タイプのクエストリスト
     */
    fun getQuestsForType(type: QuestTypes): LiveData<List<Quest>> {
        return allQuests.map { quests ->
            quests.filter { it.type == type }
        }
    }
}
