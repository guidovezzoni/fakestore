plugins {
    alias(libs.plugins.fakestore.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.guidovezzoni.fakestore.data"
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
