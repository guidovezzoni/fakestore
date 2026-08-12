plugins {
    alias(libs.plugins.fakestore.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.guidovezzoni.fakestore.data"
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
