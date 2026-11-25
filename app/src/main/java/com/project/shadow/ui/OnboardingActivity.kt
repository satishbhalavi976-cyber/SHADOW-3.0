package com.project.shadow.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.project.shadow.R
import com.project.shadow.databinding.ActivityOnboardingBinding
import com.project.shadow.databinding.ItemOnboardingPageBinding

/**
 * SHADOW 3.0 Onboarding Activity
 * Shown on first launch. Black background, minimal white UI.
 */
class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val pages = listOf(
        OnboardingPage(
            "Meet SHADOW 3.0",
            "Your intelligent voice assistant, reimagined with a sleek black & white interface.",
            R.drawable.ic_shadow_logo
        ),
        OnboardingPage(
            "Always Listening",
            "Activate with your voice or the power button. Set SHADOW as your default digital assistant.",
            R.drawable.ic_mic
        ),
        OnboardingPage(
            "Smart Conversations",
            "Chat, ask questions, set reminders, control your phone — all with natural language.",
            R.drawable.ic_chat
        ),
        OnboardingPage(
            "Vision & Camera",
            "Point your camera at anything and ask SHADOW about it. Powered by ML Kit.",
            R.drawable.ic_camera
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = OnboardingAdapter(pages)

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < pages.size - 1) {
                binding.viewPager.currentItem = current + 1
            } else {
                finish()
            }
        }

        binding.btnSkip.setOnClickListener { finish() }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val isLast = position == pages.size - 1
                binding.btnNext.text = if (isLast) getString(R.string.get_started) else getString(R.string.next)
                binding.btnSkip.visibility = if (isLast) View.INVISIBLE else View.VISIBLE
            }
        })
    }

    data class OnboardingPage(val title: String, val desc: String, val iconRes: Int)

    private class OnboardingAdapter(
        private val items: List<OnboardingPage>
    ) : RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val binding = ItemOnboardingPageBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return PageViewHolder(binding)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        class PageViewHolder(private val binding: ItemOnboardingPageBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(page: OnboardingPage) {
                binding.ivIcon.setImageResource(page.iconRes)
                binding.tvTitle.text = page.title
                binding.tvDesc.text = page.desc
            }
        }
    }
}
