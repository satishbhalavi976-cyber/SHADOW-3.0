package com.project.shadow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.project.shadow.data.Conversation
import com.project.shadow.data.ShadowRepository
import com.project.shadow.databinding.ActivityHistoryBinding
import com.project.shadow.databinding.ItemHistoryBinding

/**
 * SHADOW 3.0 - Conversation History Activity
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val conversations = ShadowRepository.getAllConversations()
        if (conversations.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvHistory.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvHistory.visibility = View.VISIBLE
            binding.rvHistory.layoutManager = LinearLayoutManager(this)
            binding.rvHistory.adapter = HistoryAdapter(conversations) { selected ->
                ShadowRepository.loadConversation(selected)
                finish()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private class HistoryAdapter(
        private val items: List<Conversation>,
        private val onClick: (Conversation) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding = ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(items[position], onClick)
        }

        override fun getItemCount(): Int = items.size

        class ViewHolder(private val binding: ItemHistoryBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(conversation: Conversation, onClick: (Conversation) -> Unit) {
                binding.tvTitle.text = conversation.title
                binding.tvPreview.text = conversation.preview()
                binding.tvDate.text = conversation.formattedDate()
                binding.root.setOnClickListener { onClick(conversation) }
            }
        }
    }
}
