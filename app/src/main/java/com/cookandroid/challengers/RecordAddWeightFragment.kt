package com.cookandroid.challengers

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.cookandroid.challengers.data.WeightRecord
import com.cookandroid.challengers.data.db.AppDatabase
import com.cookandroid.challengers.databinding.FragmentRecordAddWeightBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RecordAddWeightFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentRecordAddWeightBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: AppDatabase
    private var selectedDate: LocalDate = LocalDate.now()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecordAddWeightBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogTheme) // Apply style here.  Important!
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) // Use super's onCreateDialog
        if (dialog is BottomSheetDialog) {
            dialog.behavior.apply {
                isDraggable = true // Make it draggable
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
        }
        dialog.setOnShowListener { dialogInterface ->
            if (dialogInterface is BottomSheetDialog) {
                val bottomSheet = dialogInterface.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                bottomSheet?.let {
                    it.background =
                        ContextCompat.getDrawable(dialogInterface.context, R.drawable.bottom_sheet_background) //Use dialogInterface.context
                    val behavior = BottomSheetBehavior.from(it)
                    behavior.peekHeight =
                        resources.getDimensionPixelSize(R.dimen.exercise_set_peek_height) // Corrected resource name.
                    behavior.maxHeight =
                        resources.getDimensionPixelSize(R.dimen.exercise_set_peek_height) //Corrected resource name
                    behavior.state = BottomSheetBehavior.STATE_EXPANDED
                }
            }
        }
        return dialog;
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = AppDatabase.getDatabase(requireContext(), viewLifecycleOwner.lifecycleScope)

        // Initial date setup
        binding.dateEditText.hint = selectedDate.format(dateFormatter)

        // Date selection listener
        binding.dateEditText.setOnClickListener {
            DatePickerFragment { date ->
                selectedDate = date
                binding.dateEditText.hint = date.format(dateFormatter)
            }.show(childFragmentManager, "datePicker")
        }

        // Save button listener
        binding.saveButton.setOnClickListener {
            val weight = binding.weightEditText.text.toString().toFloatOrNull()
            val bodyFat = binding.bodyFatEditText.text.toString().toFloatOrNull()
            val muscle = binding.skeletalMuscleMassEditText.text.toString().toFloatOrNull()

            if (weight == null) {
                binding.weightEditText.error = "체중을 입력해주세요"
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                val record = WeightRecord(
                    date = selectedDate,
                    weight = weight.toDouble(),
                    bodyFatPercentage = bodyFat?.toDouble(),
                    skeletalMuscleMass = muscle?.toDouble()
                )
                db.weightRecordDao().insert(record)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "저장되었습니다.", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
        }
    }
    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as? MainActivity)?.showBottomNav()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class DatePickerFragment(val onDateSelected: (LocalDate) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Use current date as default
        val c = java.util.Calendar.getInstance()
        val year = c.get(java.util.Calendar.YEAR)
        val month = c.get(java.util.Calendar.MONTH)
        val day = c.get(java.util.Calendar.DAY_OF_MONTH)

        // Create and return DatePickerDialog
        return android.app.DatePickerDialog(requireContext(), { _, year, month, dayOfMonth -> // Changed to dayOfMonth
            val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            onDateSelected(selectedDate)
        }, year, month, day)
    }
}

