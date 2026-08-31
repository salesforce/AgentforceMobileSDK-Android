# AgentforceClient setup reference (Android)

## The two constructor shapes consumers actually use

### Employee agent (OAuth or OrgJWT) — `FullConfig`

```kotlin
val configuration = AgentforceConfiguration.builder(
    authCredentialProvider = AppCredentialProvider()
)
    .setApplication(application)
    .setSalesforceDomain("https://mycompany.my.salesforce.com")
    .setNetwork(AppNetwork(okHttpClient))
    .setNavigation(AppNavigation())
    .setLogger(AppLogger())
    .setDelegate(AppAgentforceUIDelegate())
    .build()

val agentforceMode = AgentforceMode.FullConfig(configuration)

val client = AgentforceClient().apply {
    init(
        authCredentialProvider = configuration.authCredentialProvider,
        agentforceMode = agentforceMode,
        application = application
    )
}

val conversation = client.startAgentforceConversation(agentId = "0Xx...")
```

### Public service agent — `ServiceAgent`

```kotlin
val configuration = AgentforceConfiguration.builder(
    authCredentialProvider = AppCredentialProvider() // returns Guest(salesforceDomain)
)
    .setApplication(application)
    .setSalesforceDomain("https://mycompany.my.salesforce.com")
    .setNetwork(AppNetwork(okHttpClient))
    .setNavigation(AppNavigation())
    .setLogger(AppLogger())
    .build()

val serviceConfig = ServiceAgentConfiguration.builder(
    serviceApiURL = "https://api.salesforce.com",
    organizationId = "00D...",
    esDeveloperName = "MyServiceAgent",
    authorizationContext = AuthorizationContext(
        authorizationMethod = AuthorizationMethod.UNVERIFIED
    )
).build()

val agentforceMode = AgentforceMode.ServiceAgent(
    serviceAgentConfiguration = serviceConfig,
    agentforceConfiguration = configuration
)

val client = AgentforceClient().apply {
    init(
        authCredentialProvider = configuration.authCredentialProvider,
        agentforceMode = agentforceMode,
        application = application
    )
}

val conversation = client.startAgentforceServiceConversation(
    esDeveloperName = serviceConfig.esDeveloperName
)
```

## Holder pattern

`AgentforceClient` must outlive any single Activity (rotation, back-stack pop, etc.). Idiomatic ownership:

```kotlin
class AgentforceHolder(application: Application) {
    val client: AgentforceClient = AgentforceClient()
    var conversation: AgentforceConversation? = null
        private set

    init {
        client.init(...)
        conversation = client.startAgentforceConversation()
    }
}
```

…held on `Application` (`MyApp.agentforce`) or as a Hilt `@Singleton`. Don't instantiate `AgentforceClient` inside an Activity's `onCreate` — the conversation will reset on every rotation.

## Starting conversations

```kotlin
// Employee / FullConfig
val conversation = client.startAgentforceConversation(
    agentId = "0Xx...",
    sessionId = null  // or a previously-saved session ID to resume
)

// Service agent (uses ES developer name, NOT agent ID)
val conversation = client.startAgentforceServiceConversation(
    esDeveloperName = "MyServiceAgent",
    sessionId = null
)
```

If you call `startAgentforceConversation()` without an `agentId` on the FullConfig path, the SDK uses the default agent for the org.

## Conversation lifecycle

| Method | Behavior |
|---|---|
| `conversation.sendUtterance(utterance, attachment)` | Send a message. `attachment` is `AgentforceAttachment?`. |
| `conversation.conversationManager.sendMessage(...)` | Lower-level streaming entry point — returns a `ReceiveChannel<AgentforceComponent>`. |
| `conversation.sessionId` | `StateFlow<String>` for observing and persisting the current resumable session ID. |
| `conversation.conversationState` | `StateFlow<AgentforceConversationState>`; includes `ENDED` in current releases. |
| `client.clearStorage(esDeveloperName, clearAuthorization)` suspend | Service-Agent only. Clears MIAW/CoreSDK state for that deployment. |

Subscribe to messages by collecting the channel returned from `conversationManager.sendMessage(...)` (Headless / Option B), or just let `AgentforceConversationContainer` render the streamed UI for you (Option A — what most consumers want).

## Rendering the chat UI

```kotlin
@Composable
fun ChatScreen(holder: AgentforceHolder, onClose: () -> Unit) {
    val conversation = holder.conversation ?: return
    holder.client.AgentforceConversationContainer(
        conversation = conversation,
        onClose = onClose
    )
}
```

The composable manages the top app bar, message list, and input field for you. Only call it inside a Compose scope.
