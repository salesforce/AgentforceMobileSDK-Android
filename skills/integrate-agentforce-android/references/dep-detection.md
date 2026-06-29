# Gradle dependency detection (Android)

## Decision tree

1. If `settings.gradle.kts` exists at the project root → **Kotlin DSL project**. Edit it.
2. Else if `settings.gradle` exists → **Groovy project**. Edit it (the syntax is slightly different).
3. Else if there's no Gradle settings file at all, ask the user where the Android project root is and `cd` there.

When **both** `.kts` and `.gradle` exist, ask the user — they probably mean to migrate but it's not the skill's job to do that.

## Refusing to run inside the SDK repo

If the working directory contains `agentforce-sdk/`, `agentforce-service/`, or `agentforce-test-harness/` Gradle modules at the root, refuse and tell the user to `cd` into their consuming app. Running the skill against the SDK repo would try to add the SDK as a dependency to itself.

## `settings.gradle.kts` (Kotlin DSL — recommended)

Add the four Salesforce-related repos under `dependencyResolutionManagement.repositories`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://opensource.salesforce.com/AgentforceMobileSDK-Android/agentforce-sdk-repository") }
        maven { url = uri("https://s3.amazonaws.com/inapp.salesforce.com/public/android") }
        maven { url = uri("https://s3.amazonaws.com/salesforce-async-messaging-experimental/public/android") }
    }
}
```

If `repositoriesMode` is `FAIL_ON_PROJECT_REPOS`, double-check no app-level `build.gradle.kts` is also declaring repositories — that's a Gradle error.

## `settings.gradle` (Groovy)

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
        maven { url 'https://opensource.salesforce.com/AgentforceMobileSDK-Android/agentforce-sdk-repository' }
        maven { url 'https://s3.amazonaws.com/inapp.salesforce.com/public/android' }
        maven { url 'https://s3.amazonaws.com/salesforce-async-messaging-experimental/public/android' }
    }
}
```

## App-module `build.gradle.kts`

On Kotlin 2.0+, the legacy `composeOptions.kotlinCompilerExtensionVersion`
setting is obsolete — the new compiler model is shipped as a dedicated
Gradle plugin (`org.jetbrains.kotlin.plugin.compose`) that replaces it.
Apply that plugin instead; AGP errors out if `composeOptions` is also set.

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
    id("kotlinx-serialization")
}

android {
    compileSdk = 35
    defaultConfig {
        minSdk = 29
        targetSdk = 35
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    api("com.salesforce.android.agentforcesdk:agentforce-sdk:15.0.2")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Optional: voice support
    // api("com.salesforce.android.agentforcesdk:agentforce-sdk-voice:15.0.2")
}
```

Make sure the root `build.gradle.kts` pins `kotlin-gradle-plugin`,
`kotlin-serialization`, and `compose-compiler-gradle-plugin` to the same
Kotlin version (see floors below) so kapt doesn't blow up with a metadata
mismatch.

## Compose enablement check

The Agentforce SDK's UI is `@Composable`. If the consumer's app module does not have `buildFeatures.compose = true`, the chat container won't render. Surface this and ask whether to add Compose to the existing module before scaffolding `AgentforceChatHost.kt`.

## Min SDK / Kotlin / AGP requirements

- **Min SDK:** 29 (Android 10).
- **Kotlin:** **2.1 or higher** (recommend 2.2.0). `agentforce-sdk:15.0.2` is published with Kotlin metadata version 2.1.0/2.2.0; consumers on Kotlin 1.9.x fail kapt with `Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.1.0/2.2.0, expected version is 1.9.0`. Bump `kotlin-gradle-plugin`, `kotlin-serialization`, and `compose-compiler-gradle-plugin` together.
- **Android Gradle Plugin:** 8.9.1+ (mostly for desugar_jdk_libs:2.1.5 support).
- **Android Studio:** Meerkat 2024.3.1 or newer.

## Optional voice module

If the consumer wants voice input, add:

```kotlin
api("com.salesforce.android.agentforcesdk:agentforce-sdk-voice:15.0.2")
```

…and configure the voice module on the configuration builder:

```kotlin
.setVoiceModule(
    uiProvider = AgentforceVoiceUIProvider(),
    employeeAgentFactory = AgentforceVoiceProviderFactory(),
    serviceAgentConfig = null  // or a ServiceAgentVoiceConfig if you also want voice on MIAW
)
```

Voice requires additional Android permissions (`RECORD_AUDIO`) that the consumer must request at runtime.
