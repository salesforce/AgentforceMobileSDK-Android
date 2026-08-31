# Logger and Network setup reference (Android)

The SDK delegates two infrastructure concerns to the consumer:

- `com.salesforce.android.mobile.interfaces.logging.Logger` — write SDK loglines somewhere visible.
- `com.salesforce.android.mobile.interfaces.network.Network` — perform authenticated HTTP requests.

## Logger

The default scaffold uses `android.util.Log` under the tag `AgentforceSDK`. Visible in Logcat with filter `tag:AgentforceSDK`.

The interface only defines `e`, `i`, and `w` (no `d` / `v`):

```kotlin
interface Logger {
    fun e(message: String)
    fun e(message: String, exception: Throwable)
    fun i(message: String)
    fun i(message: String, exception: Throwable)
    fun w(message: String)
    fun w(message: String, exception: Throwable)
}
```

If the consumer already has a logging framework (Timber, SLF4J-android, etc.), wrap their existing logger inside `AppLogger` rather than introducing a duplicate.

## Network

The `Network` interface lets the SDK delegate HTTP to the consumer's existing stack. The default scaffold uses OkHttp because it's the most common Android choice; if the consumer already configures interceptors (auth refresh, telemetry, certificate pinning) on a shared `OkHttpClient`, pass that instance through.

The interface:

```kotlin
interface Network {
    suspend fun perform(request: NetworkRequest): NetworkResponse
}
```

Where `NetworkRequest` carries `path`, `method`, `headers`, `body`, and `contentType`, and `NetworkResponse` carries `body: ByteArray?`, `statusCode`, `headers`. See `snippets/AppNetwork.kt`.

## Wire-up

Both go through `AgentforceConfiguration.builder`:

```kotlin
AgentforceConfiguration.builder(authCredentialProvider)
    .setApplication(application)
    .setSalesforceDomain("https://mycompany.my.salesforce.com")
    .setNetwork(AppNetwork(okHttpClient))   // required
    .setLogger(AppLogger())                 // optional but strongly recommended
    .setNavigation(AppNavigation())         // required
    .build()
```

`Network` and `Navigation` are required; `Logger` is technically optional but you'll have no visibility into SDK behavior without one.

Current `Navigation` also includes `goto(destination, target, replace)`. Implement it even if the app initially delegates to `goto(destination, replace)`; otherwise the scaffold does not satisfy the published interface.

## Viewing logs

In Android Studio's **Logcat** tab, filter on:

```
tag:AgentforceSDK
```

In a terminal:

```bash
adb logcat -s AgentforceSDK
```
