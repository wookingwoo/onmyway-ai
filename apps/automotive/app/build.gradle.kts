plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "ai.onmyway.car"
    compileSdk = 35
    defaultConfig {
        applicationId = "ai.onmyway.car"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        buildConfigField("String", "DEFAULT_SERVER_URL", "\"http://10.0.2.2:8000\"")
    }
    flavorDimensions += "mode"
    productFlavors {
        create("demo") { dimension = "mode"; applicationIdSuffix = ".demo"; buildConfigField("boolean", "DEMO", "true") }
        create("live") { dimension = "mode"; buildConfigField("boolean", "DEMO", "false") }
    }
    buildFeatures { buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
