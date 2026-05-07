package com.example.pdjissen.ui.friend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pdjissen.R
import com.example.pdjissen.ui.shared.UserStatusViewModel

class FriendFragment : Fragment() {

    // Activity 共有の UserStatusViewModel を使う
    private val userStatusViewModel: UserStatusViewModel by activityViewModels()

    private lateinit var friendAdapter: FriendAdapter

    private lateinit var loadingIndicator: ProgressBar
    private lateinit var emptyListMessage: TextView
    private lateinit var btnAddFriend: Button
    private lateinit var btnViewRequests: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_friend, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.friendListRecyclerView)
        loadingIndicator = view.findViewById(R.id.loadingIndicator)
        emptyListMessage = view.findViewById(R.id.emptyListMessage)
        btnAddFriend = view.findViewById(R.id.btnAddFriend)
        btnViewRequests = view.findViewById(R.id.btnViewRequests)

        friendAdapter = FriendAdapter()
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = friendAdapter
        }

        // 自分のデータが更新されたらフレンド一覧を再ロード
        userStatusViewModel.userStatus.observe(viewLifecycleOwner) {
            userStatusViewModel.loadFriends()
        }

        // フレンド一覧の更新を反映
        userStatusViewModel.friendList.observe(viewLifecycleOwner) { friends ->
            loadingIndicator.visibility = View.GONE

            if (friends.isNullOrEmpty()) {
                emptyListMessage.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyListMessage.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                friendAdapter.submitList(friends)
            }
        }

        // フレンド追加画面へ遷移
        btnAddFriend.setOnClickListener {
            findNavController().navigate(R.id.action_friend_to_add)
        }

        // リクエスト一覧画面へ遷移
        btnViewRequests.setOnClickListener {
            findNavController().navigate(R.id.action_friend_to_requests)
        }
    }

    override fun onResume() {
        super.onResume()
        // 画面復帰時も再ロード
        userStatusViewModel.loadFriends()
    }
}