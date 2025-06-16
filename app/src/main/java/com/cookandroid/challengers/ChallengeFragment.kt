package com.cookandroid.challengers

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.cookandroid.challengers.databinding.FragmentChallengeBinding
import com.google.android.material.tabs.TabLayoutMediator

class ChallengeFragment : Fragment() {

    companion object {
        private const val TAG = "ChallengeFragment"
    }

    private var _binding: FragmentChallengeBinding? = null
    private val binding get() = _binding!!

    private val tabTitles = listOf("개인", "사진")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChallengeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뷰가 생성된 후에 ViewPager와 TabLayout 설정을 시작합니다.
        setupViewPager()
        setupTabLayout()
        handleInitialTabSelection()
    }

    private fun setupViewPager() {
        // isUserInputEnabled는 XML에서 설정하거나 여기서 설정할 수 있습니다.
        binding.challengeViewPager.isUserInputEnabled = false
        binding.challengeViewPager.adapter = ChallengePagerAdapter(this)
    }

    private fun setupTabLayout() {
        TabLayoutMediator(binding.challengeTabLayout, binding.challengeViewPager) { tab, position ->
            tab.text = tabTitles[position]
        }.attach()
    }

    private fun handleInitialTabSelection() {
        arguments?.getString("initialTab")?.let { tab ->
            Log.d(TAG, "Received initialTab argument: $tab")
            if (tab == "photo") {
                val photoTabIndex = tabTitles.indexOf("사진")
                if (photoTabIndex != -1) {
                    // 이제 binding.challengeViewPager가 초기화된 것이 보장되므로 안전하게 접근 가능
                    binding.challengeViewPager.setCurrentItem(photoTabIndex, false)
                    Log.i(TAG, "Switched to photo tab at index $photoTabIndex.")
                }
            }
            // 인자를 한 번 사용한 후에는 제거하여, 화면 회전 등에서 중복 실행되는 것을 방지합니다.
            arguments?.remove("initialTab")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }

    // ViewPager2 어댑터
    inner class ChallengePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2 // 개인, 사진 2개의 탭

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ChallengePersonalFragment()
                1 -> ChallengePhotoFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }
}
