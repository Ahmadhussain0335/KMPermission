import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // Every public declaration must state its visibility, so nothing becomes API by accident.
    explicitApi()

    // The committed api/ dumps describe the public ABI; checkKotlinAbi fails the build on any change
    // to it that has not been recorded with updateKotlinAbi.
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation()

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "KMPPermissionCompose"
            isStatic = true
        }
    }

    android {
        namespace = "io.github.ahmadhussain0335.kmppermission.compose"
        minSdk = 26
        compileSdk = libs.versions.android.compileSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.kmppermissionCore)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// Group, version, license, developer and SCM details come from the root gradle.properties.
mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates(artifactId = "kmppermission-compose")
    pom {
        name.set("KMPermission Compose")
        description.set("Compose Multiplatform state holders and effects for KMPermission.")
    }
}
