package com.example.pdjissen

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.pdjissen.R
import com.google.firebase.auth.FirebaseAuth
import androidx.appcompat.app.AppCompatDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // 匿名認証を実行
        signInAnonymously()
    }

    private fun signInAnonymously() {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("FirebaseAuth", "Anonymous sign in successful.")
                    val user = auth.currentUser
                    // user.uid が端末固有 ID として使われる
                    Log.d("FirebaseAuth", "User ID: ${user?.uid}")
                } else {
                    // 通常はネットワーク未接続など
                    Log.w("FirebaseAuth", "Anonymous sign in failed.", task.exception)
                }
            }
    }
}