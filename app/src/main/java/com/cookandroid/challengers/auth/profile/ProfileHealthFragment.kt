package com.cookandroid.challengers.auth.profile
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.cookandroid.challengers.R
import com.cookandroid.challengers.databinding.FragmentProfileHealthBinding
import com.cookandroid.challengers.viewmodel.ProfileViewModel

class ProfileHealthFragment : Fragment() {

    private var _binding: FragmentProfileHealthBinding? = null
    private val binding get() = _binding!!

    private lateinit var disorderButtons: List<Button>
    private val viewModel: ProfileViewModel by activityViewModels() // ✅ ViewModel 연결

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileHealthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 지병 버튼 수집
        disorderButtons = binding.disorder.children
            .filterIsInstance<LinearLayout>()
            .flatMap { it.children.toList() }
            .filterIsInstance<Button>()
            .toList()

        // 뒤로가기
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        // 입력 감지
        binding.tilProfileHeight.editText?.addTextChangedListener(heightWeightWatcher)
        binding.tilProfileWeight.editText?.addTextChangedListener(heightWeightWatcher)

        // 지병 버튼 다중 선택
        disorderButtons.forEach { button ->
            button.setOnClickListener {
                button.isSelected = !button.isSelected
            }
        }

        // 다음 버튼
        binding.btnNext.setOnClickListener {
            val height = binding.tilProfileHeight.editText?.text.toString().toIntOrNull() ?: 0
            val weight = binding.tilProfileWeight.editText?.text.toString().toIntOrNull() ?: 0
            val diseases = disorderButtons.filter { it.isSelected }.map { it.text.toString() }

            if (diseases.isEmpty()) {
                Toast.makeText(requireContext(), "지병을 하나 이상 선택해주세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ✅ ViewModel에 저장
            viewModel.height = height
            viewModel.weight = weight
            viewModel.diseases = diseases

            findNavController().navigate(R.id.action_profileHealth_to_profileWorkout)
        }

        setNextButtonEnabled(false)
    }

    private val heightWeightWatcher = object : TextWatcher {
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val heightNotEmpty = !binding.tilProfileHeight.editText?.text.isNullOrBlank()
            val weightNotEmpty = !binding.tilProfileWeight.editText?.text.isNullOrBlank()

            if (heightNotEmpty && weightNotEmpty) {
                binding.disorder.visibility = View.VISIBLE
            }

            setNextButtonEnabled(heightNotEmpty && weightNotEmpty)
        }

        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(s: Editable?) {}
    }

    private fun setNextButtonEnabled(enabled: Boolean) {
        binding.btnNext.isEnabled = enabled
        val background = if (enabled) R.drawable.btn_next_blue else R.drawable.btn_next_gray
        binding.btnNext.setBackgroundResource(background)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}