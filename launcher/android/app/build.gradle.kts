import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val launcherWebRoot = rootProject.projectDir.parentFile
val launcherWebOutput = layout.buildDirectory.dir("generated/launcher-web")

val syncLauncherWeb by tasks.registering(Copy::class) {
    from(launcherWebRoot) {
        include("index.html", "css/**", "js/**")
        into("launcher")
    }
    into(launcherWebOutput)
}

android {
    namespace = "com.rex.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rex.launcher"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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

    sourceSets["main"].assets.srcDir(launcherWebOutput.get().asFile)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }.configureEach {
    dependsOn(syncLauncherWeb)
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("androidx.webkit:webkit:1.12.1")
}
