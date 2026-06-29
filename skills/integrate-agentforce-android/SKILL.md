---
name: integrate-agentforce-android
description: Integrate the Agentforce Mobile SDK into an existing Android app. Walks the consumer through use-case discovery, picks the right auth flow (employee OAuth/JWT vs public service agent vs guest), adds the Maven dependencies, and scaffolds Kotlin files for the credential provider, AgentforceClient holder, Logger, Network, UI delegate, and a Compose chat host. Use when a developer asks to "add Agentforce", "integrate the Agentforce SDK", "set up Agentforce chat", or wire an Android app up to a Salesforce agent.
---

# integrate-agentforce-android

This skill walks a consumer through wiring the **Agentforce Mobile SDK** into their Android app. It is **interactive** — ask the user the questions in each phase before generating code. Don't assume; the wrong auth flow is the most common integration mistake.

## Operating rules

- **Run inside the consumer's project, not inside the SDK repo.** If the working directory contains `agentforce-sdk/` or `agentforce-service/` Gradle modules at the root, refuse and tell the user to `cd` into their app first.
- **Discover before deciding.** Always run Phase 1 (use-case discovery) before recommending an auth flow. Don't ask "which auth flow do you want?" — most consumers don't know.
- **Don't suggest `Guest(url)` or `OrgJWT` by default.** They're only correct in specific situations. Recommend the path that matches the user's described use case.
- **Hold the `AgentforceClient` for the conversation's lifetime.** Stash it in your Application or a long-lived ViewModel; if it's recreated mid-chat the conversation is lost.
- **Use `AskUserQuestion` for branching choices.** Don't free-text prompts — give 2–4 explicit options.
- **Substitute placeholders, don't leave `{{TOKENS}}` in the final files.** Collect values up front; if the user can't provide a value, leave a clearly-marked `// TODO:` comment instead.

## Phase 0 — Detect the target project

Look in the current working directory for:

- `settings.gradle.kts` / `settings.gradle` (root of an Android Gradle project)
- `build.gradle.kts` / `build.gradle` (in app module)
- An `app/` (or similar) module containing `AndroidManifest.xml`

If none is present, ask the user where the Android project root is and `cd` there. If the directory contains `agentforce-sdk/` and `agentforce-service/` modules at the root, refuse — that's this SDK's own repo (or its internal counterpart).

See `references/dep-detection.md` for the full Gradle setup decision tree.

## Phase 1 — Discover the use case (this drives auth)

Ask **first** what they're building, then map to an auth flow:

```
AskUserQuestion: "What kind of agent are you integrating?"
  - Employee agent (signed-in users, internal tools)         → AgentforceMode.FullConfig (employee path)
  - Public service agent (customer-facing, no sign-in)       → AgentforceMode.ServiceAgent
  - Other / not sure                                          → see references/auth-flows.md
```

### Branch A — Employee agent

Ask the follow-up:

```
AskUserQuestion: "How are you obtaining auth credentials?"
  - Salesforce Mobile SDK (UserAccountManager)   → AgentforceAuthCredentials.OAuth(authToken, orgId, userId)
  - Org JWT                                       → AgentforceAuthCredentials.OrgJWT(orgJWT)
```

- **Salesforce Mobile SDK**: scaffold `AppCredentialProvider` from `references/snippets/AppCredentialProvider+OAuth.kt`. The provider's `getAuthCredentials()` reads from `SalesforceSDKManager.getInstance().userAccountManager.currentUser` — or wraps the consumer's existing token-source class if they already have one.
- **Org JWT**: scaffold from `references/snippets/AppCredentialProvider+OrgJWT.kt`. Ask for the source of the JWT (a function reference, encrypted SharedPreferences key, or a backend call) and wire `getAuthCredentials()` to call into it on every invocation. Don't cache.

For both employee paths, use `AgentforceMode.FullConfig(configuration)`. Salesforce currently exposes `EmployeeAgentConfiguration` as well, but the public SDK path most apps follow is `FullConfig` with an `AgentforceConfiguration.builder(...)` — that's what the README shows and it accepts every option (network, navigation, logger, theme).

### Branch B — Public service agent

This is the **simplest** path:

- Use `AgentforceMode.ServiceAgent(serviceAgentConfiguration, agentforceConfiguration)`.
- The SDK still requires an `AgentforceAuthCredentialProvider` — for unauthenticated service agents, scaffold one that returns `AgentforceAuthCredentials.Guest(url = "<your salesforceDomain>")`. See `references/snippets/AppCredentialProvider+Guest.kt`.
- Tell the user they'll need a **Messaging-for-In-App-Web (MIAW) mobile deployment** in their Salesforce org first, and link the docs:
  - https://help.salesforce.com/s/articleView?id=service.miaw_deployment_mobile.htm&type=5
- If they don't have one yet, pause here. The skill can't proceed without `esDeveloperName`, `organizationId`, and `serviceApiURL` from the deployment.

### Branch C — Other / not sure

Walk them through `references/auth-flows.md`. The two extra options to surface here:

- `Guest(url)` — for public agents going through the Agent API behind an Experience Cloud site. Most "public agent" cases should use Branch B's MIAW service-agent flow instead.
- `PassThroughAuth(miawJWT, eventID)` — only for service agents whose MIAW deployment uses `AuthorizationMethod.PASSTHROUGH`. The consumer's backend mints a MIAW JWT and the SDK calls `fetchMIAWJWTForPassthrough(...)` on demand.

## Phase 2 — Pick the chat presentation point

```
AskUserQuestion: "Where should the chat UI live?"
  - Bottom sheet (recommended)                       → ChatHost+BottomSheet.kt
  - Full-screen Activity / route                     → ChatHost+FullScreenActivity.kt
  - Embedded panel in an existing screen             → ChatHost+EmbeddedPanel.kt
  - Dialog / modal popup                             → ChatHost+Dialog.kt
```

Each option corresponds to one snippet in `references/snippets/`. `AgentforceClient.AgentforceConversationContainer(conversation, onClose)` is a `@Composable` — drop it inside any composable scope.

See `references/chat-presentation.md` for the patterns and how to remember `showChat` state across configuration changes (`rememberSaveable`).

## Phase 3 — Collect config values

Based on the chosen branch:

| Branch | Required values |
|---|---|
| Employee + Mobile SDK | `salesforceDomain` (instance URL, e.g. `https://mycompany.my.salesforce.com`); `agentId` |
| Employee + Org JWT | Same as above, plus the JWT source (function/closure, secure storage key, or backend endpoint) |
| Public Service Agent | `esDeveloperName`, `organizationId`, `serviceApiURL`, plus a `salesforceDomain` for the `Guest` URL |
| Guest (via "Other") | `salesforceDomain`, `agentId` |

Ask one question per missing value. If the user gives "I don't know" for a Service Agent value, point them back at the MIAW deployment link and stop.

## Phase 4 — Add the dependencies

Edit two Gradle files. See `references/dep-detection.md` for full details and KTS/Groovy variants.

### `settings.gradle.kts` — add Maven repos

```kotlin
dependencyResolutionManagement {
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

### App-module `build.gradle.kts` — plugins and dependencies

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
    id("kotlinx-serialization")
}

android {
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    api("com.salesforce.android.agentforcesdk:agentforce-sdk:15.0.2")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Optional: voice support
    // api("com.salesforce.android.agentforcesdk:agentforce-sdk-voice:15.0.2")
}
```

On Kotlin 2.0+, `composeOptions.kotlinCompilerExtensionVersion` is replaced
by the `org.jetbrains.kotlin.plugin.compose` Gradle plugin. Apply that
plugin (matched to the project's Kotlin version) and remove any leftover
`composeOptions { ... }` block from the consuming module — AGP errors when
both are present.

The consumer needs:

- **Min SDK ≥ 29** (Android 10).
- **Compose enabled** in their app module — the chat UI is `@Composable`.
- **Kotlin ≥ 2.1** (recommend 2.2.0), AGP **8.9.1+**, **Android Studio Meerkat 2024.3.1+**. `agentforce-sdk:15.0.2` is published with Kotlin metadata 2.1.0/2.2.0; consumers on Kotlin 1.9.x fail kapt with a metadata-version mismatch.

If their app is not Compose-based, surface this and ask whether they want to add Compose to the existing module. The SDK does not ship a View-based chat surface.

## Phase 5 — Scaffold Kotlin files

Create the package `com.<their.package>.agentforce` and write the following, substituting placeholders with values from Phase 3:

| File | When | Source snippet |
|---|---|---|
| `AppCredentialProvider.kt` | Always | `snippets/AppCredentialProvider+OAuth.kt`, `+OrgJWT.kt`, or `+Guest.kt` based on Phase 1 |
| `AppNetwork.kt` | Always | `snippets/AppNetwork.kt` (OkHttp-backed `Network` impl) |
| `AppLogger.kt` | Always | `snippets/AppLogger.kt` (`android.util.Log`-backed `Logger` impl) |
| `AppNavigation.kt` | Always | `snippets/AppNavigation.kt` (no-op `Navigation` to start) |
| `AgentforceHolder.kt` | Always | `snippets/AgentforceHolder.kt` (initializes the client; lives on `Application`) |
| `AppAgentforceUIDelegate.kt` | Always | `snippets/AppAgentforceUIDelegate.kt` |
| `AgentforceChatHost.kt` | Always | one of `snippets/ChatHost+*.kt` based on Phase 2 |

`AgentforceHolder.kt` is parameterized by mode — pass the right `AgentforceMode` and the right conversation-starter call (`startAgentforceConversation()` for employee/full-config, `startAgentforceServiceConversation(esDeveloperName = ...)` for service agents).

The logger conforms to `com.salesforce.android.mobile.interfaces.logging.Logger` (methods `e/i/w`, no `d`). Wire it via `.setLogger(AppLogger())` on the configuration builder.

## Phase 6 — Wire it into Application

`AgentforceClient` should outlive any single Activity. Patch the consumer's `Application` subclass:

```kotlin
class MyApp : Application() {
    lateinit var agentforce: AgentforceHolder
        private set

    override fun onCreate() {
        super.onCreate()
        agentforce = AgentforceHolder(application = this)
    }
}
```

…and register the `Application` class in `AndroidManifest.xml`:

```xml
<application
    android:name=".MyApp"
    ... >
```

If the consumer already uses Hilt or another DI framework, surface that instead — provide `AgentforceHolder` as a `@Singleton` rather than putting it on `Application` directly.

## Phase 7 — Verify

Tell the user:

1. **Sync Gradle** (`./gradlew :app:dependencies` or via Android Studio). Expect a clean sync.
2. **Build**: `./gradlew :app:assembleDebug`. If it fails on `BUILD_LIBRARY_FOR_DISTRIBUTION`-equivalent or duplicate-class errors, check the Maven repo order in `settings.gradle.kts` (Salesforce repos must come before `mavenCentral()` if you hit conflicts).
3. **Holder lifetime**: confirm `AgentforceHolder` is owned at the `Application` level (or as a Hilt singleton). If it's instantiated inside an Activity, the conversation will reset on rotation.
4. **Run on device/emulator**, navigate to the chat surface, send a test utterance, watch for streamed response.
5. **Logs**: in Logcat, filter on tag `AgentforceSDK` (the default for the scaffolded `AppLogger`) to see SDK loglines.
6. **Service Agent only**: if `AuthorizationMethod.USERVERIFIED` or `PASSTHROUGH` was chosen, remind the user to implement `fetchMIAWJWTForPassthrough(...)` and `getIdentityToken()` on their `AgentforceAuthCredentialProvider`.

If the build fails, common causes:

- Missing `kotlin-kapt` or `kotlinx-serialization` plugins.
- Missing core library desugaring on Android Gradle Plugin <8.x.
- Compose not enabled in the consuming module.
- Salesforce Maven repo not added to `settings.gradle.kts`.

## References

- `references/auth-flows.md` — full credential-flow decision tree, including `Guest`, `OrgJWT`, and `PassThroughAuth` edge cases.
- `references/client-setup.md` — `AgentforceClient.init(...)`, mode selection, holder pattern, conversation lifecycle.
- `references/logger-setup.md` — `Logger` and `Network` interface conformance.
- `references/chat-presentation.md` — bottom sheet / full-screen / embedded / dialog Compose patterns.
- `references/dep-detection.md` — Gradle KTS/Groovy variants and Compose enablement check.
- `references/snippets/*.kt` — file templates with `{{PLACEHOLDERS}}` to substitute.
