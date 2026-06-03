plugins {
    alias(libs.plugins.nix.natives) apply false
    alias(libs.plugins.android.kotlin) apply false

    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.atomicfu) apply false
}
