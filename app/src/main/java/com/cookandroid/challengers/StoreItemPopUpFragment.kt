package com.cookandroid.challengers

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.cookandroid.challengers.api.RetrofitClient
import com.cookandroid.challengers.api.RetrofitClient.PurchaseRequest
import com.cookandroid.challengers.databinding.FragmentStoreItemPopUpBinding
import com.cookandroid.challengers.util.UserPreference
import kotlinx.coroutines.launch

class StoreItemPopUpFragment : DialogFragment() {

    private var _binding: FragmentStoreItemPopUpBinding? = null
    private val binding get() = _binding!!

    private lateinit var storeViewModel: StoreViewModel
    private lateinit var userPreference: UserPreference
    private var productItem: ProductItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            // Android 13 (API 33) 이상과 하위 버전 호환성을 위한 getParcelable 처리
            productItem = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_PRODUCT_ITEM, ProductItem::class.java)
            } else {
                @Suppress("DEPRECATION")
                it.getParcelable(ARG_PRODUCT_ITEM)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoreItemPopUpBinding.inflate(inflater, container, false)
        storeViewModel = ViewModelProvider(requireActivity()).get(StoreViewModel::class.java)
        userPreference = UserPreference(requireContext())

        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        productItem?.let {
            binding.itemPriceTextView.text = it.price.toString()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnBuy.setOnClickListener {
            handlePurchase()
        }
    }

    private fun handlePurchase() {
        val item = productItem ?: return
        val userId = userPreference.getUserId()

        if (userId == -1) {
            Toast.makeText(requireContext(), "로그인 정보가 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnBuy.isEnabled = false

        lifecycleScope.launch {
            try {
                val request = PurchaseRequest(userId, item.id)
                Log.d(TAG, "Sending Purchase Request: userId=$userId, itemId=${item.id}")

                val response = RetrofitClient.storeApi.purchaseItem(request)

                if (response.isSuccessful && response.body()?.success == true) {
                    val purchaseResponse = response.body()!!

                    val msg = purchaseResponse.message ?: "구매 완료"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

                    storeViewModel.onItemPurchased(item, purchaseResponse.updatedCoin)
                    setFragmentResult(REQUEST_KEY_PURCHASE, bundleOf(RESULT_KEY_PURCHASE_SUCCESS to true))

                    dismiss()
                } else {
                    val errorMsg = response.body()?.message ?: "구매 실패 (${response.code()})"
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()

                    Log.e(TAG, "Purchase failed: ${response.code()} - ${response.errorBody()?.string()}")
                    if (isAdded) binding.btnBuy.isEnabled = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Purchase exception", e)
                Toast.makeText(requireContext(), "구매 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                if (isAdded) binding.btnBuy.isEnabled = true
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            val desiredMarginDp = 16
            val displayMetrics = requireContext().resources.displayMetrics
            val dialogWidth = displayMetrics.widthPixels - (2 * (desiredMarginDp * displayMetrics.density).toInt())
            setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT)
            setDimAmount(0.6f)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "StoreItemPopUpFragment"
        private const val ARG_PRODUCT_ITEM = "product_item"

        const val REQUEST_KEY_PURCHASE = "purchase_request"
        const val RESULT_KEY_PURCHASE_SUCCESS = "purchase_success"

        fun newInstance(item: ProductItem): StoreItemPopUpFragment {
            return StoreItemPopUpFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PRODUCT_ITEM, item)
                }
            }
        }
    }
}
