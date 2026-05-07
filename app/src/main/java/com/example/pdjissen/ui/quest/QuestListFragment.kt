package com.example.pdjissen.ui.quest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.pdjissen.QuestTypes
import com.example.pdjissen.databinding.FragmentQuestListBinding
import com.example.pdjissen.ui.quest.QuestListViewModel

class QuestListFragment : Fragment() {

    private var _binding: FragmentQuestListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuestListViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentQuestListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val questAdapter = QuestAdapter()
        binding.questRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = questAdapter
        }

        // タブごとのクエスト種別を取得して該当データを監視
        val type = arguments?.getSerializable(ARG_QUEST_TYPE) as? QuestTypes
        if (type != null) {
            viewModel.getQuestsForType(type).observe(viewLifecycleOwner) { quests ->
                questAdapter.submitList(quests)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_QUEST_TYPE = "quest_type_argument"
        fun newInstance(questType: QuestTypes): QuestListFragment {
            return QuestListFragment().apply {
                arguments = bundleOf(ARG_QUEST_TYPE to questType)
            }
        }
    }
}
