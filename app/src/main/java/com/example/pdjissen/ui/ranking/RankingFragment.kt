package com.example.pdjissen.ui.ranking

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.pdjissen.R

class RankingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ranking, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ① TextView を取得
        val name = view.findViewById<TextView>(R.id.userName)
        val status = view.findViewById<TextView>(R.id.onlineStatus)
        val level = view.findViewById<TextView>(R.id.currentLevel)
        val todaySteps = view.findViewById<TextView>(R.id.todaySteps)
        val todayDistance = view.findViewById<TextView>(R.id.todayDistance)
        val totalSteps = view.findViewById<TextView>(R.id.totalSteps)
        val totalDistance = view.findViewById<TextView>(R.id.totalDistance)

        // ② とりあえず固定値で表示（Firebase連携前）
        name.text = "デモユーザー"
        status.text = "オンライン"
        status.setTextColor(0xFF4CAF50.toInt()) // 緑

        level.text = "レベル: 5"
        todaySteps.text = "今日の歩数: 3567"
        todayDistance.text = "今日の距離: 2850 m"
        totalSteps.text = "総歩数: 155432"
        totalDistance.text = "総距離: 123400 m"
    }
}
