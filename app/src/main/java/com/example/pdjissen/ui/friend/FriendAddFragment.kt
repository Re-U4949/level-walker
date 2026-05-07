package com.example.pdjissen.ui.friend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.pdjissen.R
import com.example.pdjissen.data.UserDataRepository
import java.text.NumberFormat
import java.util.*

class FriendAddFragment : Fragment() {

    private lateinit var viewModel: FriendAddViewModel

    // UIコンポーネント
    private lateinit var editFriendUid: EditText
    private lateinit var btnSearch: Button
    private lateinit var searchResultLayout: LinearLayout
    private lateinit var textResultName: TextView
    private lateinit var textResultSteps: TextView
    private lateinit var btnSendRequest: Button
    private lateinit var textStatusMessage: TextView
    private lateinit var searchProgressBar: ProgressBar

    private var foundUserUid: String? = null // 検索ヒット中のユーザー UID

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_friend_add, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editFriendUid = view.findViewById(R.id.editFriendUid)
        btnSearch = view.findViewById(R.id.btnSearch)
        searchResultLayout = view.findViewById(R.id.searchResultLayout)
        textResultName = view.findViewById(R.id.textResultName)
        textResultSteps = view.findViewById(R.id.textResultSteps)
        btnSendRequest = view.findViewById(R.id.btnSendRequest)
        textStatusMessage = view.findViewById(R.id.textStatusMessage)
        searchProgressBar = view.findViewById(R.id.searchProgressBar)

        val repository = UserDataRepository()
        val factory = FriendAddViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(FriendAddViewModel::class.java)

        btnSearch.setOnClickListener {
            searchUser()
        }

        btnSendRequest.setOnClickListener {
            foundUserUid?.let { uid ->
                viewModel.sendRequest(uid)
            }
        }

        observeViewModel()
    }

    private fun searchUser() {
        val uid = editFriendUid.text.toString().trim()
        if (uid.isEmpty()) {
            Toast.makeText(context, "ユーザーIDを入力してください。", Toast.LENGTH_SHORT).show()
            return
        }
        viewModel.searchUserByUid(uid)
    }

    private fun observeViewModel() {
        viewModel.searchResult.observe(viewLifecycleOwner) { userStatus ->
            searchProgressBar.visibility = View.GONE

            if (userStatus != null) {
                displayUserResult(userStatus)
                foundUserUid = userStatus.uid
                textStatusMessage.text = ""
            } else {
                // 該当ユーザーなし、または自分自身を検索した場合
                searchResultLayout.visibility = View.GONE
                foundUserUid = null
                textStatusMessage.text = "ユーザーが見つかりません。UIDを確認してください。"
            }
        }

        viewModel.requestStatus.observe(viewLifecycleOwner) { status ->
            when (status) {
                "sending" -> {
                    btnSendRequest.isEnabled = false
                    textStatusMessage.text = "リクエストを送信中..."
                }
                "success" -> {
                    btnSendRequest.text = "リクエスト送信済み"
                    btnSendRequest.isEnabled = false
                    textStatusMessage.text = "フレンドリクエストを送信しました。"
                }
                "failure" -> {
                    btnSendRequest.isEnabled = true
                    textStatusMessage.text = "リクエスト送信に失敗しました。（既にリクエスト済みかもしれません）"
                }
                "idle" -> {
                    btnSendRequest.isEnabled = true
                    btnSendRequest.text = "フレンドリクエストを送信"
                }
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            searchProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            btnSearch.isEnabled = !isLoading
        }
    }

    private fun displayUserResult(status: UserDataRepository.UserStatus) {
        val formatter = NumberFormat.getNumberInstance(Locale.JAPAN)

        searchResultLayout.visibility = View.VISIBLE
        textResultName.text = "ユーザー名: ${status.name} (UID: ${status.uid.take(5)}...)"
        textResultSteps.text = "総歩数: ${formatter.format(status.totalSteps)} 歩"

        btnSendRequest.isEnabled = true
        btnSendRequest.text = "フレンドリクエストを送信"
    }
}