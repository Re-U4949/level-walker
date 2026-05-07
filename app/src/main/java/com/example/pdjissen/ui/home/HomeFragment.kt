package com.example.pdjissen.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.pdjissen.R
import com.example.pdjissen.databinding.FragmentHomeBinding
import com.example.pdjissen.ui.shared.UserStatusViewModel
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // 画面遷移をまたいで状態を保持するため activityViewModels() を使用
    private val userStatusViewModel: UserStatusViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // =========== データ監視 ===========

        // レベル・総歩数の表示更新
        userStatusViewModel.userStatus.observe(viewLifecycleOwner) { status ->
            binding.textLevelValue.text = status.userLevel.toString()
            binding.textSteps.text = "総歩数: ${status.totalSteps} 歩"
            // 編集中は表示を上書きしない
            if (!binding.editTextText.hasFocus()) {
                binding.editTextText.setText(status.name)
            }

            binding.textUid.text = "ID: ${status.uid} (タップでコピー)"

            // UID タップでクリップボードにコピー
            binding.textUid.setOnClickListener {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("PDJissen ID", status.uid)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "IDをコピーしました。", Toast.LENGTH_SHORT).show()
            }
        }

        // レベルゲージの更新
        userStatusViewModel.levelProgress.observe(viewLifecycleOwner) { progress ->
            binding.progressBarLevel.progress = progress
        }

        // 次レベルまでの残り歩数表示
        userStatusViewModel.stepsUntilNextLevel.observe(viewLifecycleOwner) { remaining ->
            binding.textNextLevelInfo.text = "あと $remaining 歩でレベルアップ"
        }

        // =========== 画面遷移 (Navigation) ===========

        // 計測画面 (Dashboard)
        binding.btnStartMeasure.setOnClickListener {
            findNavController().navigate(R.id.navigation_dashboard)
        }

        // フレンド画面
        binding.homeFriend.setOnClickListener {
            findNavController().navigate(R.id.navigation_friend)
        }

        // クエスト画面
        binding.homeQuest.setOnClickListener {
            findNavController().navigate(R.id.navigation_notifications)
        }

        // ランキング画面
        binding.homeRanking.setOnClickListener {
            findNavController().navigate(R.id.navigation_ranking)
        }

        // ステータス画面
        binding.homeStatus.setOnClickListener {
            findNavController().navigate(R.id.navigation_status)
        }

        binding.editTextText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // フォーカスが外れたら名前を保存
                val inputName = binding.editTextText.text.toString()
                userStatusViewModel.changeName(inputName)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}