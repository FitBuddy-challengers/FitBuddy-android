package com.cookandroid.challengers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.cookandroid.challengers.ui.record.RecordWeightFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class RecordFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_record, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tabLayout = view.findViewById<TabLayout>(R.id.RecordTabLayout)
        val viewPager = view.findViewById<ViewPager2>(R.id.RecordViewPager)
        val RecordTextView = view.findViewById<TextView>(R.id.recordTextView)

        // ViewPager 어댑터 설정 (내부 클래스 사용)
        val adapter = RecordPagerAdapter(this)
        viewPager.adapter = adapter

        // TabLayout과 ViewPager 연결
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = if (position == 0) "운동 기록" else "체중 기록"
        }.attach()

        // "챌린지" TextView 관련 로직
    }

    inner class RecordPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

        override fun getItemCount(): Int = 2 // 운동기록 및 체중기록

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> RecordDateFragment() // 운동기록 Fragment
                1 -> RecordWeightFragment() // 체중기록 Fragment
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}