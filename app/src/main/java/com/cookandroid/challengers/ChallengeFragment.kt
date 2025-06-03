package com.cookandroid.challengers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class ChallengeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_challenge, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = view.findViewById<TabLayout>(R.id.challengeTabLayout)
        val viewPager = view.findViewById<ViewPager2>(R.id.challengeViewPager)
        val challengeTextView = view.findViewById<TextView>(R.id.challengeTextView)

        // 스와이프 전환 비활성화
        viewPager.isUserInputEnabled = false

        // ViewPager 어댑터 설정 (내부 클래스 사용)
        val adapter = ChallengePagerAdapter(this)
        viewPager.adapter = adapter

        // TabLayout과 ViewPager 연결
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "개인" else "사진"
        }.attach()

        // "챌린지" TextView 관련 로직
    }

    inner class ChallengePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = 2 // 개인 및 그룹 챌린지

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ChallengePersonalFragment() // 개인 챌린지 Fragment
                1 -> ChallengeGroupFragment()    // 그룹 챌린지 Fragment
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}