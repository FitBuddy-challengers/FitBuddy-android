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
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.databinding.FragmentUploadPhotoBinding
import com.cookandroid.challengers.util.UserPreference
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Date
import java.util.Locale
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
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


        fun newInstance(
            completionTimeMillis: Long,
            totalDurationMillis: Long
        ): ChallengeUploadPhotoFragment {
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
        Log.d(
            TAG,
            "onCreate: completionTimeMillis=$completionTimeMillis, totalDurationMillis=$totalDurationMillis"
        )
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
        requestCameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    Log.d(TAG, "Camera permission granted")
                    launchCamera()
                } else {
                    Log.w(TAG, "Camera permission denied")
                    Toast.makeText(requireContext(), "카메라 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
                }
            }

        takePictureLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
                if (success) {
                    cameraImageUri?.let { uri ->
                        Log.d(TAG, "Picture taken successfully: $uri")
                        finalSelectedImageUri = uri
                        galleryImageUri = null
                        displaySelectedImage(uri, isCamera = true)
                        // 사진 촬영 성공 후 바로 업로드 핸들러 호출
                        handleUpload()
                    }
                } else {
                    Log.d(TAG, "Picture taking cancelled or failed")
                    cameraImageUri = null
                }
            }

        pickImageLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                uri?.let {
                    Log.d(TAG, "Image picked from gallery: $it")
                    galleryImageUri = it
                    finalSelectedImageUri = it
                    cameraImageUri = null
                    displaySelectedImage(it, isCamera = false)
                    // 갤러리 선택 성공 후 바로 업로드 핸들러 호출
                    handleUpload()
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
            // 부모 프래그먼트(ExerciseFragment)에 사진 업로드 단계가 처리되었음을 알림
            setFragmentResult(
                REQUEST_KEY_UPLOAD_PHOTO,
                bundleOf(RESULT_KEY_PHOTO_ACTION_DONE to true)
            )

            try {
                findNavController().navigate(R.id.action_challengeUploadPhotoFragment_to_homeFragment)
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Navigation to home failed. Action ID might be incorrect or destination not found.",
                    e
                )
                Toast.makeText(requireContext(), "홈 화면으로 이동 중 오류 발생", Toast.LENGTH_SHORT).show()
            }
        }

        binding.cameraButtonContainer.setOnClickListener {
            Log.d(TAG, "Camera button container clicked")
            // 유효성 검사 추가
            if (completionTimeMillis <= 0) {
                Toast.makeText(requireContext(), "운동한 날짜를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // totalDurationMillis는 0초일 수 있으므로 0 이상인지 확인
            if (totalDurationMillis < 0) {
                Toast.makeText(requireContext(), "운동한 시간을 작성해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.galleryButtonContainer.setOnClickListener {
            Log.d(TAG, "Gallery button container clicked")
            // 유효성 검사 추가
            if (completionTimeMillis <= 0) {
                Toast.makeText(requireContext(), "운동한 날짜를 선택해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (totalDurationMillis < 0) {
                Toast.makeText(requireContext(), "운동한 시간을 작성해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pickImageLauncher.launch("image/*")
        }
        Log.d(
            TAG,
            "TODO: Add an 'Upload/Done' button and its click listener to call handleUpload()"
        )
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
            Toast.makeText(requireContext(), "카메라 실행 중 오류 발생: ${ex.message}", Toast.LENGTH_SHORT)
                .show()
            cameraImageUri = null
        }
    }

    @Throws(java.io.IOException::class)
    private fun createImageFile(context: Context): File {
        val timeStamp: String =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
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
        val imageUri = finalSelectedImageUri
        if (imageUri == null) {
            Toast.makeText(requireContext(), "인증할 사진을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = UserPreference(requireContext()).getUserId()
        if (userId == -1) {
            Toast.makeText(requireContext(), "로그인 정보가 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 운동 완료 시간을 "YYYY-MM-DD" 형식으로 변환
        val dateString = Instant.ofEpochMilli(completionTimeMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Uri를 실제 파일로 변환
        val imageFile = getFileFromUri(requireContext(), imageUri)
        if (imageFile == null) {
            Toast.makeText(requireContext(), "사진 파일을 처리할 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 파일 및 다른 데이터 파트 생성
        val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val photoPart = MultipartBody.Part.createFormData("photo", imageFile.name, requestFile)
        val userIdPart = userId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val datePart = dateString.toRequestBody("text/plain".toMediaTypeOrNull())

//        binding.uploadProgressBar.visibility = View.VISIBLE // 프로그레스바 표시

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.challengeApi.uploadPhoto(photoPart, userIdPart, datePart)

                if (response.isSuccessful && response.body()?.success == true) {
                    navigateToChallengeScreen()
                } else {
                    val errorMsg = response.body()?.message ?: "업로드 실패: ${response.code()}"
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "업로드 중 오류 발생", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Upload failed", e)
            } finally {
//                if(isAdded) binding.uploadProgressBar.visibility = View.GONE
            }
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File? {
        val fileName = "upload_temp_${System.currentTimeMillis()}.jpg"
        val tempFile = File(context.cacheDir, fileName)
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file from URI", e)
            null
        }
    }

    private fun navigateToChallengeScreen() {
        if (!isAdded) return
        try {
            Toast.makeText(requireContext(), "인증 완료! 챌린지 화면으로 이동합니다.", Toast.LENGTH_SHORT).show()

            val args = Bundle().apply {
                putString("initialTab", "photo")
            }
            val actionId = R.id.action_challengeUploadPhotoFragment_to_challengeFragment
            findNavController().navigate(actionId, args)

        } catch (e: Exception) {
            Log.e(TAG, "Navigation to Challenge Screen failed.", e)
            Toast.makeText(requireContext(), "화면 이동 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
        _binding = null
    }
}
