plugins {
    id("mpv-base")
    id("mpv-android-lib")
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":ffm"))
}

android {
    namespace = "dev.silenium.libs.mpv.android.tests"
    externalNativeBuild {
        cmake {
            path = file("src/androidTest/cpp/CMakeLists.txt")
        }
    }
}
