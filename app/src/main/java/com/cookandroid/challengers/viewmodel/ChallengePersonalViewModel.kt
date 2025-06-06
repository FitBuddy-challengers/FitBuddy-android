import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
//import com.cookandroid.challengers.data.ChallengePersonal
//import com.cookandroid.challengers.data.ChallengePersonalDao
import kotlinx.coroutines.launch

//class ChallengePersonalViewModel(private val challengePersonalDao: ChallengePersonalDao) : ViewModel() {
//
//    val displayedChallenge: LiveData<ChallengePersonal?> = challengePersonalDao.getFirstChallenge() // LiveData 직접 할당
//
//    fun completeChallenge(completedChallenge: ChallengePersonal) {
//        viewModelScope.launch {
//            // 1. 완료된 챌린지 상태 업데이트
//            val updatedCompletedChallenge = completedChallenge.copy(isAchieved = true)
//            challengePersonalDao.update(updatedCompletedChallenge)
//
//            // 2. 다음 챌린지 조회 (LiveData를 직접 관찰하므로 여기서는 별도 처리 불필요)
//            // _displayedChallenge.value = challengePersonalDao.getNextChallenge(completedChallenge.id).value
//        }
//    }
//
//    // 다음 챌린지를 명시적으로 가져와 업데이트해야 하는 경우 (예: completeChallenge 호출 시 즉시 UI 업데이트)
//    private val _nextChallenge = MutableLiveData<ChallengePersonal?>()
//    val nextChallenge: LiveData<ChallengePersonal?> = _nextChallenge
//
//    fun completeChallengeAndUpdateNext(completedChallenge: ChallengePersonal) {
//        viewModelScope.launch {
//            val updatedCompletedChallenge = completedChallenge.copy(isAchieved = true)
//            challengePersonalDao.update(updatedCompletedChallenge)
//            _nextChallenge.value = challengePersonalDao.getNextChallenge(completedChallenge.id).value
//        }
//    }
//}