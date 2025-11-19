import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("kotlin-parcelize")

}

android {
    namespace = "com.cookandroid.challengers"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cookandroid.challengers"
        minSdk = 35
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // ✅ 모든 빌드타입에 공통 적용
        buildConfigField(
            "String",
            "BASE_URL",
            "\"https://fitbuddy-server-p6r6.onrender.com/\"" // ★ 끝에 / 꼭 유지!
        )
        //buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:3000/\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // ✅ 여긴 따로 BASE_URL 안 넣어도 돼 (지금은 defaultConfig가 해줌)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true // ✨ BASE_URL 사용을 위한 설정
    }
    kapt {
        correctErrorTypes = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navigation : 75, 76줄 중복
//    implementation("androidx.navigation:navigation-fragment-ktx:2.7.0")
//    implementation("androidx.navigation:navigation-ui-ktx:2.7.0")

    // ✨ 서버 통신 관련 라이브러리 추가
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    implementation ("com.google.code.gson:gson:2.9.0")

    // Room DB (로컬 DB)
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    implementation(libs.androidx.navigation.compose)
    kapt("androidx.room:room-compiler:2.7.1")

    // glide (이미지 불러오기 )
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    //Retrofit에 다음 의존성 추가
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // 캘린더
    implementation("com.github.prolificinteractive:material-calendarview:2.0.1"){
        exclude ("org.threeten", "threetenbp")
    }
    implementation("org.threeten:threetenbp:1.6.8")
    // 체중 그래프 
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // 접속일
    implementation ("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
}