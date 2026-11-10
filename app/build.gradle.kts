plugins {
    id("com.android.application")
}

android {
    namespace = "com.zhangshang.keyboard"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.zhangshang.keyboard"
        minSdk = 28
        targetSdk = 35
        versionCode = 18
        versionName = "0.6.5"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
}
