import gradle.kotlin.dsl.accessors._e3e75d9eed3de87ba322431308ccbb6d.publishing

plugins {
    com.android.library
    io.kotest
}

androidTestDependencies()

android.commonConfig()

android {
    publishing {
        multipleVariants {
            allVariants()
            withSourcesJar()
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("android-lib") {
            afterEvaluate {
                from(components["default"])
            }
        }
    }
}
