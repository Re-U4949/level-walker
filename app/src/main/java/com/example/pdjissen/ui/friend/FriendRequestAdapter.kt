package com.example.pdjissen.ui.friend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pdjissen.R
import com.example.pdjissen.data.UserDataRepository

class FriendRequestAdapter(
    private val onAccept: (UserDataRepository.FriendRequest) -> Unit,
    private val onReject: (UserDataRepository.FriendRequest) -> Unit
) : ListAdapter<DisplayRequest, FriendRequestAdapter.RequestViewHolder>(RequestDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val displayRequest = getItem(position)
        holder.bind(displayRequest, onAccept, onReject)
    }

    class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textSenderName: TextView = itemView.findViewById(R.id.textSenderName)
        private val btnAccept: Button = itemView.findViewById(R.id.btnAccept)
        private val btnReject: Button = itemView.findViewById(R.id.btnReject)

        fun bind(
            displayRequest: DisplayRequest,
            onAccept: (UserDataRepository.FriendRequest) -> Unit,
            onReject: (UserDataRepository.FriendRequest) -> Unit
        ) {
            textSenderName.text = "${displayRequest.senderName}からフレンドリクエスト"

            btnAccept.setOnClickListener {
                onAccept(displayRequest.request)
            }

            btnReject.setOnClickListener {
                onReject(displayRequest.request)
            }
        }
    }

    class RequestDiffCallback : DiffUtil.ItemCallback<DisplayRequest>() {
        override fun areItemsTheSame(oldItem: DisplayRequest, newItem: DisplayRequest): Boolean {
            return oldItem.requestId == newItem.requestId
        }

        override fun areContentsTheSame(oldItem: DisplayRequest, newItem: DisplayRequest): Boolean {
            return oldItem == newItem
        }
    }
}