package com.cookandroid.challengers.auth.profile

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.R
import com.cookandroid.challengers.databinding.FragmentProfileHealthBinding

class ProfileHealthFragment : Fragment() {

    private var _binding: FragmentProfileHealthBinding? = null
    private val binding get() = _binding!!

    private lateinit var disorderButtons: List<Button>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileHealthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 지병 버튼 리스트
        disorderButtons = binding.disorder.children
            .filterIsInstance<LinearLayout>()
            .flatMap { it.children.toList() }
            .filterIsInstance<Button>().toList()

        // 뒤로
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // 신장
        binding.tilProfileHeight.editText?.addTextChangedListener(heightWeightWatcher)
        // 체중
        binding.tilProfileWeight.editText?.addTextChangedListener(heightWeightWatcher)

        // 지병 버튼 다중 선택
        disorderButtons.forEach { button ->
            button.setOnClickListener {
                button.isSelected = !button.isSelected
            }
        }

        binding.btnNext.setOnClickListener {
            // ✅ Navigation으로 이동
            findNavController().navigate(R.id.action_profileHealth_to_profileWorkout)
        }

        // 초기 상태 설정
        setNextButtonEnabled(false)
    }

    // 신장/체중 TextWatcher
    private val heightWeightWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val isHeightEntered = !binding.tilProfileHeight.editText?.text.isNullOrBlank()
            val isWeightEntered = !binding.tilProfileWeight.editText?.text.isNullOrBlank()

            // 지병 레이아웃
            if (isHeightEntered && isWeightEntered) {
                binding.disorder.visibility = View.VISIBLE
            }

            setNextButtonEnabled(isHeightEntered && isWeightEntered)
        }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(s: Editable?) {}
    }

    // 다음 버튼 활성화
    private fun setNextButtonEnabled(enabled: Boolean) {
        binding.btnNext.isEnabled = enabled
        val backgroundRes = if (enabled) R.drawable.btn_next_blue else R.drawable.btn_next_gray
        binding.btnNext.setBackgroundResource(backgroundRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}