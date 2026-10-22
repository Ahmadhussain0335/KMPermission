import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    // Every public declaration must state its visibility, so nothing becomes API by accident.
    explicitApi()

    // The committed api/ dumps describe the public ABI; checkKotlinAbi fails the build on any change
    // to it that has not been recorded with updateKotlinAbi.
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KMPPermissionCore"
            isStatic = true
        }
    }

    android {
        namespace = "com.kmp.permission"
        // The library targets Android 8.0+ regardless of what consuming apps declare.
        minSdk = 26
        compileSdk = libs.versions.android.compileSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            // Flow appears in the public API (PermissionObserver), so this must be `api`, not
            // `implementation` — consumers need it on their compile classpath too.
            api(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            // ComponentActivity appears in the public Android factory function's signature.
            api(libs.androidx.activity)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.core.ktx)
        }
    }
}
