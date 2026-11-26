plugins {
  alias(libs.plugins.android.application)
}

android {
  namespace = "com.college.shadowcrypt.android"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.college.shadowcrypt"
    minSdk = 24
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    aidl = false
    buildConfig = false
    shaders = false
  }

  sourceSets {
    getByName("main") {
      java.directories.add("src/shared/java")
    }
  }
}

val syncSharedCore by tasks.registering(Sync::class) {
  from("../../src/main/java")
  include(
    "com/college/shadowcrypt/model/**",
    "com/college/shadowcrypt/engine/**",
    "com/college/shadowcrypt/persistence/**"
  )
  exclude("**/SoundEngine.java")
  into(layout.projectDirectory.dir("src/shared/java"))
}

tasks.named("preBuild") {
  dependsOn(syncSharedCore)
}
