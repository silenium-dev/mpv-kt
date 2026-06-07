import com.android.build.api.dsl.ManagedVirtualDevice

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotest)
    id("mpv-base")
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":ffm"))

    androidTestImplementation(libs.slf4j.android)
    androidTestImplementation(libs.bundles.androidx.test)
    androidTestImplementation(libs.bundles.tests)
    androidTestImplementation(libs.kotest.runner.junit4)
}

android {
    namespace = "dev.silenium.libs.mpv.android.tests"
    compileSdk {
        version = release(ProjectConfig.COMPILE_SDK)
    }
    defaultConfig {
        minSdk = ProjectConfig.MIN_SDK
    }
    packaging {
        resources.pickFirsts += "META-INF/*"
    }
    externalNativeBuild {
        cmake {
            path = file("src/androidTest/cpp/CMakeLists.txt")
        }
    }
    testOptions {
        managedDevices {
            allDevices {
                create<ManagedVirtualDevice>("pixel10X86_64") {
                    device = "Pixel 10 Pro"
                    sdkVersion = 30
                    testedAbi = "x86_64"
                    require64Bit = true
                    systemImageSource = "aosp-atd"
                }
            }
        }
    }
}
