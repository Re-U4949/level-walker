package com.example.pdjissen.ui.status

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.pdjissen.R
import com.example.pdjissen.ui.shared.UserStatusViewModel

class StatusFragment : Fragment() {

    // Activity スコープで共有する ViewModel
    private val userStatusViewModel: UserStatusViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_status, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 画面部品の取得
        val levelText: TextView = view.findViewById(R.id.currentLevel)
        val stepsText: TextView = view.findViewById(R.id.totalSteps)
        val distanceText: TextView = view.findViewById(R.id.totalDistance)
        val userNameText: TextView = view.findViewById(R.id.userName)

        // 今日のデータ表示用（必要時に利用）
        val todayStepsText: TextView = view.findViewById(R.id.todaySteps)
        val todayDistanceText: TextView = view.findViewById(R.id.todayDistance)

        // データ更新で画面を反映
        userStatusViewModel.userStatus.observe(viewLifecycleOwner) { status ->

            userNameText.text = status.name

            levelText.text = "レベル: ${status.userLevel}"

            stepsText.text = "総歩数: ${status.totalSteps} 歩"

            // メートル → キロメートル
            val kmDistance = status.totalDistance / 1000.0
            distanceText.text = String.format("総距離: %.2f km", kmDistance)
        }


    }
    override fun onResume() {
        super.onResume()
        // ログイン完了後に監視を開始
        userStatusViewModel.startListening()
    }
}