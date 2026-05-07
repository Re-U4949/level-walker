package com.example.pdjissen.ui.friend

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pdjissen.R
import com.example.pdjissen.data.UserDataRepository
import java.text.NumberFormat
import java.util.Locale

class FriendAdapter : ListAdapter<UserDataRepository.UserStatus, FriendAdapter.FriendViewHolder>(FriendDiffCallback()) {

    class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.friendName)
        private val levelTextView: TextView = itemView.findViewById(R.id.friendLevel)
        private val totalStepsTextView: TextView = itemView.findViewById(R.id.friendTotalSteps)
        private val totalDistanceTextView: TextView = itemView.findViewById(R.id.friendTotalDistance)
        private val todayStepsTextView: TextView = itemView.findViewById(R.id.friendTodaySteps)
        private val todayDistanceTextView: TextView = itemView.findViewById(R.id.friendTodayDistance)

        fun bind(status: UserDataRepository.UserStatus) {
            nameTextView.text = status.name
            levelTextView.text = "Lv. ${status.userLevel}"

            val formatter = NumberFormat.getNumberInstance(Locale.JAPAN)

            totalStepsTextView.text = "総歩数: ${formatter.format(status.totalSteps)} 歩"

            val totalKm = status.totalDistance / 1000.0
            totalDistanceTextView.text = "総距離: ${String.format("%.1f", totalKm)} km"

            todayStepsTextView.text = "今日歩数: ${formatter.format(status.todaySteps)} 歩"

            val todayKm = status.todayDistance / 1000.0
            todayDistanceTextView.text = "今日距離: ${String.format("%.1f", todayKm)} km"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        val friendStatus = getItem(position)
        holder.bind(friendStatus)
    }

    class FriendDiffCallback : DiffUtil.ItemCallback<UserDataRepository.UserStatus>() {
        override fun areItemsTheSame(oldItem: UserDataRepository.UserStatus, newItem: UserDataRepository.UserStatus): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: UserDataRepository.UserStatus, newItem: UserDataRepository.UserStatus): Boolean {
            return oldItem == newItem
        }
    }
}