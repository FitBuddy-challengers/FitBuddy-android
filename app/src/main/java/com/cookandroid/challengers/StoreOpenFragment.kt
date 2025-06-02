package com.cookandroid.challengers

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import androidx.fragment.app.DialogFragment // setStyle 사용을 위해
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class StoreOpenFragment : BottomSheetDialogFragment() {

    // 리스너 인터페이스 정의
    interface StoreBottomSheetDismissListener {
        fun onBottomSheetDismissed()
    }
    private lateinit var storeViewModel: StoreViewModel
    private var dismissListener: StoreBottomSheetDismissListener? = null
    private val categoryIds = listOf("top", "onepiece", "pants", "glasses", "acc", "character")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme_NoDimBottomSheetDialog)

        // ViewModel 인스턴스 가져오기 (Activity 범위로 하여 StoreFragment와 공유 가능)
        // requireActivity()를 사용하면 StoreFragment와 같은 Activity에 속할 때 ViewModel 공유
        storeViewModel = ViewModelProvider(requireActivity()).get(StoreViewModel::class.java)
    }

    fun setStoreBottomSheetDismissListener(listener: StoreBottomSheetDismissListener) {
        this.dismissListener = listener
    }

    // (이하 기존 StoreOpenFragment 코드 - tabLayout, viewPager, tabIcons 등)
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var categoryPagerAdapter: CategoryPagerAdapter


    private val tabIcons = listOf(
        R.drawable.ic_store_top,
        R.drawable.ic_store_onepiece,
        R.drawable.ic_store_pants,
        R.drawable.ic_store_glasses,
        R.drawable.ic_store_acc,
        R.drawable.ic_store_character
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_store_open, container, false)
        tabLayout = view.findViewById(R.id.tab_layout_store)
        viewPager = view.findViewById(R.id.view_pager_store)
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)

        // Dim 효과 프로그래밍 방식으로 제거
        dialog.window?.setDimAmount(0f)

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.background = ContextCompat.getDrawable(dialog.context, R.drawable.bottom_sheet_background)
                val behavior = BottomSheetBehavior.from(it)
                behavior.peekHeight = resources.getDimensionPixelSize(R.dimen.store_peek_height)
                behavior.maxHeight = resources.getDimensionPixelSize(R.dimen.store_peek_height) // 이 부분을 주석 처리하거나 peekHeight보다 큰 값으로 설정 고려
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewPager()
        setupTabLayout()
    }

    private fun setupViewPager() {
        // StoreCategoryFragment 생성 시 categoryId를 명확히 전달
        val fragments = tabIcons.indices.map { index ->
            StoreCategoryFragment.newInstance(categoryIds[index]) // 정의된 categoryId 사용
        }
        categoryPagerAdapter = CategoryPagerAdapter(this, fragments)
        viewPager.adapter = categoryPagerAdapter
    }

    private fun setupTabLayout() {
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.setIcon(tabIcons[position])
        }.attach()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dismissListener?.onBottomSheetDismissed()
    }

    companion object {
        fun newInstance(): StoreOpenFragment {
            return StoreOpenFragment()
        }
    }
}