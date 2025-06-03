package com.cookandroid.challengers // 실제 패키지명으로 변경해주세요

import android.Manifest
import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.cookandroid.challengers.databinding.FragmentUploadPhotoBinding // 생성된 ViewBinding 클래스명
// import com.cookandroid.challengers.api.RetrofitClient // 실제 API 호출 시 필요
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ChallengeUploadPhotoFragment : Fragment() {

    private var _binding: FragmentUploadPhotoBinding? = null
    private val binding get() = _binding!!

    private var completionTimeMillis: Long = 0L
    private var totalDurationMillis: Long = 0L

    private var cameraImageUri: Uri? = null
    private var galleryImageUri: Uri? = null
    private var finalSelectedImageUri: Uri? = null

    companion object {
        private const val TAG = "UploadPhotoFragment"
        private const val ARG_COMPLETION_TIME_MILLIS = "completion_time_millis"
        private const val ARG_TOTAL_DURATION_MILLIS = "total_duration_millis"

        // Constants for Fragment Result API
        const val REQUEST_KEY_UPLOAD_PHOTO = "upload_photo_request_key"
        const val RESULT_KEY_PHOTO_ACTION_DONE = "photo_action_done_result"


        fun newInstance(completionTimeMillis: Long, totalDurationMillis: Long): ChallengeUploadPhotoFragment {
            val fragment = ChallengeUploadPhotoFragment()
            val args = Bundle().apply {
                putLong(ARG_COMPLETION_TIME_MILLIS, completionTimeMillis)
                putLong(ARG_TOTAL_DURATION_MILLIS, totalDurationMillis)
            }
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var requestCameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickImageLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            completionTimeMillis = it.getLong(ARG_COMPLETION_TIME_MILLIS)
            totalDurationMillis = it.getLong(ARG_TOTAL_DURATION_MILLIS)
        }
        Log.d(TAG, "onCreate: completionTimeMillis=$completionTimeMillis, totalDurationMillis=$totalDurationMillis")
        setupActivityResultLaunchers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUploadPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated")
        setupInitialUI()
        setupClickListeners()
    }

    private fun setupActivityResultLaunchers() {
        requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d(TAG, "Camera permission granted")
                launchCamera()
            } else {
                Log.w(TAG, "Camera permission denied")
                Toast.makeText(requireContext(), "카메라 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                cameraImageUri?.let { uri ->
                    Log.d(TAG, "Picture taken successfully: $uri")
                    finalSelectedImageUri = uri
                    galleryImageUri = null
                    displaySelectedImage(uri, isCamera = true)
                }
            } else {
                Log.d(TAG, "Picture taking cancelled or failed")
                cameraImageUri = null
            }
        }

        pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                Log.d(TAG, "Image picked from gallery: $it")
                galleryImageUri = it
                finalSelectedImageUri = it
                cameraImageUri = null
                displaySelectedImage(it, isCamera = false)
            } ?: run {
                Log.d(TAG, "No image picked from gallery")
            }
        }
    }

    private fun setupInitialUI() {
        if (completionTimeMillis > 0) {
            val dateFormat = SimpleDateFormat("yyyy.MM.dd. a hh:mm", Locale.getDefault())
            binding.setUploadTime.text = dateFormat.format(Date(completionTimeMillis))
        } else {
            binding.setUploadTime.text = "시간 정보 없음"
        }

        if (totalDurationMillis >= 0) {
            val hours = TimeUnit.MILLISECONDS.toHours(totalDurationMillis)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(totalDurationMillis) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(totalDurationMillis) % 60
            binding.setExerciseHour.setText(hours.toString())
            binding.setExerciseMin.setText(minutes.toString())
            binding.setExerciseSec.setText(seconds.toString())
        } else {
            binding.setExerciseHour.setText("0")
            binding.setExerciseMin.setText("0")
            binding.setExerciseSec.setText("0")
        }
    }

    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            Log.d(TAG, "Back button clicked. Setting result and navigating to Home.")
            // ★ 부모 프래그먼트(ExerciseFragment)에 사진 업로드 단계가 처리되었음을 알림
            setFragmentResult(REQUEST_KEY_UPLOAD_PHOTO, bundleOf(RESULT_KEY_PHOTO_ACTION_DONE to true))

            try {
                // TODO: 'action_challengeUploadPhotoFragment_to_homeFragment'를 실제 네비게이션 액션 ID로 변경.
                // 홈으로 이동 시 이전 스택(운동화면들)을 제거하고 싶다면 popUpTo 옵션 사용.
                findNavController().navigate(R.id.action_challengeUploadPhotoFragment_to_homeFragment) // 실제 액션 ID로 변경 필요
            } catch (e: Exception) {
                Log.e(TAG, "Navigation to home failed. Action ID might be incorrect or destination not found.", e)
                Toast.makeText(requireContext(), "홈 화면으로 이동 중 오류 발생", Toast.LENGTH_SHORT).show()
                // Fallback: 홈으로 가는 액션이 없다면, 이전 화면으로 돌아감 (이 경우 ExerciseDoingFragment일 수 있음)
                // findNavController().popBackStack()
            }
        }

        binding.cameraButtonContainer.setOnClickListener {
            Log.d(TAG, "Camera button container clicked")
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.galleryButtonContainer.setOnClickListener {
            Log.d(TAG, "Gallery button container clicked")
            pickImageLauncher.launch("image/*")
        }

        // TODO: XML에 "인증 완료" 또는 "건너뛰기" 버튼을 추가하고 해당 ID로 리스너 설정 필요
        // 예시:
        // binding.buttonCompleteUpload.setOnClickListener {
        //     handleUpload() // 이 함수 내부에서 업로드 성공 후 setFragmentResult 호출 및 네비게이션
        // }
        // binding.buttonSkipUpload.setOnClickListener {
        //     Log.d(TAG, "Skip button clicked. Setting result and navigating to Home.")
        //     setFragmentResult(REQUEST_KEY_UPLOAD_PHOTO, bundleOf(RESULT_KEY_PHOTO_ACTION_DONE to true))
        //     navigateToHome() // 홈으로 이동하는 공통 함수 호출
        // }
        Log.d(TAG, "TODO: Add an 'Upload/Done/Skip' button and its click listener to call handleUpload() or navigateToHome() with setFragmentResult.")
    }

    private fun launchCamera() {
        try {
            val photoFile: File? = createImageFile(requireContext())
            if (photoFile != null) {
                val providerUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    photoFile
                )
                cameraImageUri = providerUri
                Log.d(TAG, "Launching camera with URI: $cameraImageUri")
                cameraImageUri?.let { uri ->
                    takePictureLauncher.launch(uri)
                } ?: Log.e(TAG, "cameraImageUri is null after FileProvider.getUriForFile")
            } else {
                Log.e(TAG, "Error: Could not create image file.")
                Toast.makeText(requireContext(), "사진 파일을 생성할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } catch (ex: Exception) {
            Log.e(TAG, "Error creating image file or launching camera", ex)
            Toast.makeText(requireContext(), "카메라 실행 중 오류 발생: ${ex.message}", Toast.LENGTH_SHORT).show()
            cameraImageUri = null
        }
    }

    @Throws(java.io.IOException::class)
    private fun createImageFile(context: Context): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (storageDir != null && !storageDir.exists()) {
            storageDir.mkdirs()
        }
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    private fun displaySelectedImage(uri: Uri, isCamera: Boolean) {
        if (!isAdded) return
        Log.d(TAG, "Displaying selected image: $uri, isCamera: $isCamera")
        if (isCamera) {
            binding.ivSelectedCameraImage.visibility = View.VISIBLE
            binding.placeholderCameraLayout.visibility = View.GONE
            Glide.with(this).load(uri).into(binding.ivSelectedCameraImage)
            binding.ivSelectedGalleryImage.visibility = View.GONE
            binding.placeholderGalleryLayout.visibility = View.VISIBLE
            binding.ivSelectedGalleryImage.setImageDrawable(null)
        } else {
            binding.ivSelectedGalleryImage.visibility = View.VISIBLE
            binding.placeholderGalleryLayout.visibility = View.GONE
            Glide.with(this).load(uri).into(binding.ivSelectedGalleryImage)
            binding.ivSelectedCameraImage.visibility = View.GONE
            binding.placeholderCameraLayout.visibility = View.VISIBLE
            binding.ivSelectedCameraImage.setImageDrawable(null)
        }
    }

    private fun handleUpload() {
        if (finalSelectedImageUri == null) {
            Toast.makeText(requireContext(), "인증할 사진을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        // ... (기존 업로드 준비 로직) ...
        Log.i(TAG, "Preparing to upload: Image URI: $finalSelectedImageUri")
        Toast.makeText(requireContext(), "업로드 시작 (구현 필요)", Toast.LENGTH_SHORT).show()

        // TODO: 실제 서버 업로드 로직 구현
        // 업로드 성공 시:
        // setFragmentResult(REQUEST_KEY_UPLOAD_PHOTO, bundleOf(RESULT_KEY_PHOTO_ACTION_DONE to true))
        // navigateToHome() 또는 다른 적절한 화면으로 이동

        // 임시로, handleUpload가 호출되면 바로 완료된 것으로 간주하고 결과 설정 및 홈으로 이동
        Log.d(TAG, "handleUpload: Simulating upload completion.")
        setFragmentResult(REQUEST_KEY_UPLOAD_PHOTO, bundleOf(RESULT_KEY_PHOTO_ACTION_DONE to true))
        navigateToHome()
    }

    private fun navigateToHome() {
        if (!isAdded) return
        try {
            // TODO: 'action_challengeUploadPhotoFragment_to_homeFragment'를 실제 네비게이션 액션 ID로 변경.
            findNavController().navigate(R.id.action_challengeUploadPhotoFragment_to_homeFragment)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation to home failed.", e)
            Toast.makeText(requireContext(), "홈 화면으로 이동 중 오류 발생", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }
}
