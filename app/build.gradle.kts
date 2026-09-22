import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

/**
 * Where debug builds look for the EventFinder REST API.
 *
 * Resolution order:
 *  1. `api.base.url` in local.properties — machine-specific and gitignored, so
 *     this is the place to put your laptop's LAN IP when testing on a phone.
 *  2. `-PAPI_BASE_URL=...` on the Gradle command line, or gradle.properties.
 *  3. 10.0.2.2, which is the host machine as seen from the Android emulator.
 *
 * Must end with a trailing slash — Retrofit requires it on a base URL.
 */
val debugApiBaseUrl: String = run {
    val fromLocalProperties = rootProject.file("local.properties")
        .takeIf { it.exists() }
        ?.let { file ->
            Properties().apply { file.inputStream().use { load(it) } }
                .getProperty("api.base.url")
        }

    val configured = fromLocalProperties
        ?: project.findProperty("API_BASE_URL") as? String

    val resolved = configured?.trim()?.takeIf { it.isNotEmpty() } ?: "http://10.0.2.2:5217/"

    if (resolved.endsWith("/")) resolved else "$resolved/"
}

/**
 * Where release builds look for the API — the deployed instance. Set
 * `API_BASE_URL_RELEASE` in gradle.properties, which is committed so the whole
 * team builds against the same deployment.
 */
val releaseApiBaseUrl: String = run {
    val configured = (project.findProperty("API_BASE_URL_RELEASE") as? String)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "https://eventfinder-api.fly.dev/"

    if (configured.endsWith("/")) configured else "$configured/"
}

android {
    namespace = "com.eventfinder.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.eventfinder.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Google OAuth web client ID used for Google sign-in (SSO). Set it in
        // gradle.properties; when blank the Login screen explains that sign-in
        // is not configured instead of failing at runtime.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${project.findProperty("GOOGLE_WEB_CLIENT_ID") as? String ?: ""}\""
        )
    }

    buildTypes {
        debug {
            // Set `api.base.url` in local.properties to point at your laptop's
            // LAN IP when testing on a physical phone. See debugApiBaseUrl above.
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
            // The local API is plain HTTP; release builds stay HTTPS-only.
            isDebuggable = true
        }
        release {
            // The deployed API. Set API_BASE_URL_RELEASE in gradle.properties.
            buildConfigField("String", "API_BASE_URL", "\"$releaseApiBaseUrl\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        // Exposes API_BASE_URL to Kotlin via BuildConfig.
        buildConfig = true
    }
    // Compose compiler 1.5.8 is paired with Kotlin 1.9.22 (see official compatibility map).
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        // Android framework stubs (e.g. android.util.Log) are no-op in local unit tests
        unitTests {
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // ---- Jetpack Compose (BOM keeps UI library versions in sync) ----
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // ---- AndroidX core & lifecycle ----
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ---- Persistence: DataStore (preferences) + Room (offline cache) ----
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ---- Security SDK: AndroidX Biometric (fingerprint / face unlock) ----
    implementation("androidx.biometric:biometric:1.1.0")

    // ---- Google sign-in (SSO) via Credential Manager ----
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // FragmentActivity host required by the BiometricPrompt SDK.
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // ---- Networking: Retrofit + OkHttp for the free REST APIs ----
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ---- osmdroid: the free OpenStreetMap SDK (100% royalty-free mapping) ----
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // ---- Image loading ----
    implementation("io.coil-kt:coil-compose:2.6.0")

    // ---- Unit testing (run on JVM / GitHub Actions) ----
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("net.sf.kxml:kxml2:2.3.0")

    // ---- Instrumented testing ----
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // ---- Compose tooling ----
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}