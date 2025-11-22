package com.cookandroid.challengers

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.cookandroid.challengers.databinding.FragmentAiRecommendationBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AiRecommendationBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentAiRecommendationBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "AiRecommendationBottomSheet"
        private const val ARG_CONTENT = "content"

        fun newInstance(content: String): AiRecommendationBottomSheet {
            return AiRecommendationBottomSheet().apply {
                arguments = bundleOf(ARG_CONTENT to content)
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheet = (dialogInterface as BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                it.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottom_sheet_background)
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAiRecommendationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 전달받은 텍스트
        val content = arguments?.getString(ARG_CONTENT) ?: "내용을 불러올 수 없습니다."
        binding.contentTextView.text = content
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}