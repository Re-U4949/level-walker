package com.example.pdjissen.ui.quest

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.pdjissen.Quest
import com.example.pdjissen.databinding.ItemQuestBinding

/**
 * クエスト一覧 RecyclerView 用アダプター
 */
class QuestAdapter : ListAdapter<Quest, QuestAdapter.QuestViewHolder>(QuestDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestViewHolder {
        val binding = ItemQuestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return QuestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestViewHolder, position: Int) {
        val quest = getItem(position)
        holder.bind(quest)
    }

    class QuestViewHolder(private val binding: ItemQuestBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(quest: Quest) {
            binding.textQuestTitle.text = quest.title
            binding.textQuestDescription.text = quest.description
            binding.textQuestReward.text = "報酬: ${quest.point} ポイント"
            // TODO: 完了状態に応じた見た目の切り替え
        }
    }

    class QuestDiffCallback : DiffUtil.ItemCallback<Quest>() {
        override fun areItemsTheSame(oldItem: Quest, newItem: Quest): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Quest, newItem: Quest): Boolean {
            return oldItem == newItem
        }
    }
}