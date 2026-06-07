plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.example.myandroidapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.myandroidapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // NewsAPI Key: 在 local.properties 中设置 news.api.key=YOUR_KEY
        val newsApiKey = project.findProperty("news.api.key") as? String ?: ""
        buildConfigField("String", "NEWS_API_KEY", "\"$newsApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Material (for Activity theme)
    implementation("com.google.android.material:material:1.11.0")

    // SplashScreen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.1")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-android-compiler:2.48.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Timber
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Coil — 图片加载
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Chrome CustomTabs — 原文链接
    implementation("androidx.browser:browser:1.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Room Testing — in-memory database
    testImplementation("androidx.room:room-testing:2.6.1")

    // Robolectric — JVM 端 Android 单元测试（无需模拟器）
    testImplementation("org.robolectric:robolectric:4.14.1")

    // Compose UI Testing — createComposeRule / onNodeWithText / assertIsDisplayed 等
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // AndroidX Test — ActivityScenario / ApplicationProvider
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Turbine — StateFlow 测试
    testImplementation("app.cash.turbine:turbine:1.0.0")
    // Hilt Testing
    testImplementation("com.google.dagger:hilt-android-testing:2.48.1")
    kaptTest("com.google.dagger:hilt-android-compiler:2.48.1")
}

// Compose UI 测试仅在 Debug 变体可运行（Release merged manifest 不含 ComponentActivity）
// B3-P1-2 修复：排除所有 Compose 测试（Release merged manifest 不含 ComponentActivity）
afterEvaluate {
    tasks.named<Test>("testReleaseUnitTest").configure {
        filter.excludeTestsMatching("*ComposeTest*")
    }
}
