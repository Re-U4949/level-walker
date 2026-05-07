package com.example.pdjissen.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.pdjissen.R
import com.google.firebase.auth.FirebaseAuth

/**
 * 起動時に認証をチェックし、完了後に HomeFragment へ遷移する Fragment
 */
class LoadingFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 専用レイアウト未整備のため一時的に fragment_home を流用
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAuthState()
    }

    private fun checkAuthState() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // ログイン済み
            navigateToHome()
        } else {
            // 未ログインなら匿名サインイン
            signInAnonymously()
        }
    }

    private fun signInAnonymously() {
        auth.signInAnonymously()
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    navigateToHome()
                } else {
                    // 失敗時もとりあえずホームへ進める（要：エラーハンドリング改善）
                    navigateToHome()
                }
            }
    }

    private fun navigateToHome() {
        findNavController().navigate(R.id.action_loading_to_home)
    }
}