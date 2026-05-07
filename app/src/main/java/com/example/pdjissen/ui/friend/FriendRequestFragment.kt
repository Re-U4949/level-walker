package com.example.pdjissen.ui.friend

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.pdjissen.R
import com.example.pdjissen.data.UserDataRepository

class FriendRequestFragment : Fragment() {

    private lateinit var viewModel: FriendRequestViewModel
    private lateinit var adapter: FriendRequestAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var emptyMessage: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_friend_request, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.requestListRecyclerView)
        loadingIndicator = view.findViewById(R.id.requestLoadingIndicator)
        emptyMessage = view.findViewById(R.id.emptyRequestMessage)

        val repository = UserDataRepository()
        val factory = FriendRequestViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(FriendRequestViewModel::class.java)

        adapter = FriendRequestAdapter(
            onAccept = { request ->
                viewModel.acceptRequest(request)
                Toast.makeText(context, "${request.senderUid.take(5)}...のリクエストを承認しました", Toast.LENGTH_SHORT).show()
            },
            onReject = { request ->
                viewModel.rejectRequest(request)
                Toast.makeText(context, "${request.senderUid.take(5)}...のリクエストを拒否しました", Toast.LENGTH_SHORT).show()
            }
        )
        recyclerView.adapter = adapter

        viewModel.pendingRequests.observe(viewLifecycleOwner) { requests ->
            adapter.submitList(requests)
            val isEmpty = requests.isNullOrEmpty()
            emptyMessage.visibility = if (isEmpty) View.VISIBLE else View.GONE
            recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) {
                recyclerView.visibility = View.GONE
                emptyMessage.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 画面復帰時に再ロード
        viewModel.loadPendingRequests()
    }
}