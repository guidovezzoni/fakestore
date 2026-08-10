plugins {
    alias(libs.plugins.fakestore.android.library)
}

android {
    namespace = "com.guidovezzoni.fakestore.core"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "BASE_URL", "\"https://fakestoreapi.com\"")
    }
}

dependencies {
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    testImplementation(libs.junit)
}
