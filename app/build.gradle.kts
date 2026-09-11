plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.plantrace.jzkrwq"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
      applicationIdSuffix = ".dev"
      versionNameSuffix = "-foundation-dev"
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Keep the shipped app local and deterministic. Unused template services are not dependencies.
dependencies {
  // Local, deterministic topology/derived coping; pinned and licensed in assets/licenses.
  implementation("org.locationtech.jts:jts-core:1.20.0")
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}

// Audit the resolved graphs, including transitives, instead of trusting declared dependencies.
// This explicitly resolves two configurations, so it runs separately from cached app builds.
tasks.register("reportRuntimeDependencies") {
  group = "verification"
  description = "Write debug/release runtime coordinates for the local-runtime policy check"
  notCompatibleWithConfigurationCache("Resolves runtime dependency graphs for an audit report")
  doLast {
    val report = layout.buildDirectory.dir("reports/runtime-policy").get().asFile
    report.mkdirs()
    for (variant in listOf("debug", "release")) {
      val graph = configurations.getByName("${variant}RuntimeClasspath").incoming.resolutionResult
      check(graph.allDependencies.none { it is org.gradle.api.artifacts.result.UnresolvedDependencyResult }) {
        "The $variant runtime graph contains unresolved dependencies"
      }
      val modules = graph.allComponents.mapNotNull { component ->
          (component.id as? org.gradle.api.artifacts.component.ModuleComponentIdentifier)?.let {
            "${it.group}:${it.module}:${it.version}"
          }
        }.sorted()
      check(modules.isNotEmpty()) { "No $variant runtime components were resolved" }
      report.resolve("$variant-runtime.txt").writeText(modules.joinToString("\n", postfix = "\n"))
    }
  }
}
