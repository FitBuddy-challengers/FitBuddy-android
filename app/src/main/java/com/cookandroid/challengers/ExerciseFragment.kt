package com.cookandroid.challengers

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cookandroid.challengers.auth.profile.ProfileHealthFragment
import com.cookandroid.challengers.databinding.FragmentExerciseBinding

class ExerciseFragment : Fragment() {

    private var _binding: FragmentExerciseBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExerciseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 앱 최초 진입 or 운동 정보 입력 안 했을 때
        if (savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(R.id.exercise_fragment_container, ProfileHealthFragment())
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}