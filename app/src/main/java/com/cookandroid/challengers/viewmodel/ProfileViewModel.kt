package com.cookandroid.challengers.viewmodel


import androidx.lifecycle.ViewModel

class ProfileViewModel : ViewModel() {

    var email: String = ""
    var password: String = ""  // 필요시

    var name: String = ""
    var ageGroup: String = ""
    var gender: String = ""

    var height: Int = 0
    var weight: Int = 0
    var diseases: List<String> = emptyList()

    var workoutLevel: String = ""
    var preferredWorkouts: List<String> = emptyList()
    var equipment: List<String> = emptyList()
}
