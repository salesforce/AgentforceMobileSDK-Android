# Agentforce Mobile SDK - Integration Guide

## Getting Started with Agentforce

The Agentforce Mobile SDK empowers you to integrate Salesforce's feature-rich, trusted AI platform directly into your native iOS and Android mobile applications. By leveraging the Agentforce Mobile SDK, you can deliver cutting-edge, intelligent, and conversational AI experiences to your mobile users, enhancing engagement and providing seamless access to information and actions.

### What is Agentforce?
Agentforce is Salesforce's platform for building and deploying trusted AI agents. These agents can understand user requests, access relevant data, perform actions across Salesforce and external systems, and generate helpful, conversational responses. The Agentforce Mobile SDK acts as the bridge, allowing your mobile app to communicate directly with these sophisticated agents, bringing the power of generative AI and automated workflows to your users' fingertips.

### Why Use the Agentforce Mobile SDK?
Integrating the Agentforce Mobile SDK into your app offers several key advantages. It allows you to deliver advanced AI experiences that go beyond simple chatbots, providing users with truly agentic capabilities for complex task completion, personalized interactions, and access to generative AI features within your app's context.

The SDK offers flexible integration options to suit your needs:

* **Full UI Experience:** Get up and running quickly by using the SDK's pre-built, customizable chat interface. This provides a rich, out-of-the-box conversational experience supporting text, voice, and multimodal inputs with minimal coding effort.
* **Headless Integration:** For ultimate control, use the headless SDK to power your own custom user interface or to drive automated, agent-led processes in the background. You manage the conversational state and UI, while Agentforce handles the complex AI logic. For more information go to [AgentforceService](https://github.com/salesforce/AgentforceMobileSDK-Android?tab=readme-ov-file#option-b-headless-with-agentforceservice)

We suggest starting with the Full UI Experience unless you need more granular control over the interface.

### Core Features

* **Networking:** A robust networking layer for handling both communication with the Agentforce platform and streaming responses.
* **Conversational Intelligence:** Sophisticated Natural Language Understanding (NLU) to process user requests via text or voice.
* **Salesforce Actions:** Execute actions within Salesforce, such as creating records, updating fields, and running Apex classes, directly from the conversation.
* **Customizable UI:** When using the Full UI experience, you can customize the appearance of the chat interface to match your app's branding.
* **Extensible Service Protocols:** The SDK defines a set of protocols for core services like navigation, caching, and logging, which you implement using your app's native infrastructure.

## **Before You Begin**

Before you begin integrating the Agentforce Mobile SDK,  you must first configure an agent within your Salesforce organization.

* **Create and Configure an Agent:** You will need to build and configure an agent using the tools available in your Salesforce org. This includes defining its capabilities, persona, and connecting it to any necessary Salesforce data or actions.
* **Obtain Agent and Org IDs:** Once configured, you will need to retrieve the `Agent ID` and the `Salesforce Org ID`. These unique identifiers are required to initialize the SDK and route requests to the correct agent in the correct organization.
   * You can find the Agent ID in the URL of the Agent Details page. When you select your agent from Setup, use the 18-character ID at the end of the URL. For example, when viewing this URL, `https://mydomain.salesforce.com/lightning/setup/EinsteinCopilot/0XxSB000000IPCr0AO/edit`, the agent ID is `0XxSB000000IPCr0AO`.
   * You can find the Org ID from the Company Information page in Setup.


More documentation on configuring your orgs for Agentforce are available at [developer.salesforce.com](https://developer.salesforce.com)

#### Implementing Core Service Interfaces
The Agentforce Mobile SDK is designed for flexibility and delegates several core responsibilities to the host application. This is achieved through a set of protocols (interfaces) that your application must implement. This approach allows the SDK to remain lean and lets you use your existing application architecture for handling common tasks.

You will need to create concrete implementations for the following interfaces:

**AgentforceSDK Interfaces:**

* **AgentforceAuthCredentialProviding:** Supplies the SDK with the authentication token (e.g., OAuth 2.0 access token) required to communicate securely with Salesforce APIs.
* **AgentforceInstrumentation:** A handler for the SDK to emit instrumentation and telemetry data into your app's analytics system.

**Salesforce Mobile Interfaces:**

These are described in more detail in [Salesforce Mobile Interfaces](https://github.com/forcedotcom/SalesforceMobileInterfaces-Android)

* **SalesforceNetwork.Network:** Provides the SDK with the ability to make authenticated network calls to Salesforce. Your implementation will handle the underlying networking logic, leveraging your app's existing network stack, and will be responsible for attaching the necessary authentication tokens to each request.
* **SalesforceNavigation.Navigation:** Handles navigation requests from the agent. For example, if the agent provides a link to a specific record, this interface allows your app to intercept that request and navigate the user to the appropriate screen within your native application.
* **SalesforceLogging.Logger:** Allows the SDK to pipe its internal logs into your application's logging system for easier debugging and monitoring.


By implementing these interfaces, you provide the "scaffolding" that the Agentforce SDK builds upon, ensuring seamless integration with your app's existing ecosystem.

## Android Integration Guidelines
This guide provides the steps to integrate the Agentforce Mobile SDK into your native Android application using Jetpack Compose.

### Prerequisites

* **Android Studio:** Android Studio Meerkat 2024.3.1 or newer.
* **Android Gradle Plugin:** Version 8.9.1.
* **Kotlin:** Version 1.9.22 or higher.
* **Minimum SDK Version:** API level 29 (Android 10) or higher.
* **Jetpack Compose:** The SDK's UI components are built with Compose. Your project must be configured to use it.

### Dependencies

#### Add the Maven Repository
In your top-level `settings.gradle.kts` file, add the Salesforce Maven URL:

```kotlin
 // settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://opensource.salesforce.com/AgentforceMobileSDK-Android/agentforce-sdk-repository") }
        maven { url = uri("https://s3.amazonaws.com/salesforce-async-messaging-experimental/public/android") }
    }
}
```

#### Add SDK Dependencies
In your app-level `build.gradle.kts` file, add the dependencies for the integration path you choose. The `agentforcesdk` artifact is for the Full UI experience, while `agentforce-service` is for the Headless approach.
#### Required Plugin
```kotlin
plugins {
   id("com.android.application")
   id("kotlin-android")
   id("kotlin-kapt")
   id("kotlinx-serialization")
}
```

#### Dependencies
```kotlin
// app/build.gradle.kts
dependencies {
   // Agentforce SDK Dependencies
   api("com.salesforce.android.agentforcesdk:agentforce-sdk:14.0.0")
}
```
After adding the dependencies, sync your project with the Gradle files.

### Implement Core Service Interfaces
The Agentforce Mobile SDK is designed for flexibility and delegates several core responsibilities to the host application. This is achieved through a set of protocols that your application must implement. This approach allows the SDK to remain lean and lets you use your existing application architecture for handling common tasks.
You will need to create concrete implementations for the following:

#### AgentforceSDK Interfaces
-AgentforceAuthCredentialProviding: Supplies the SDK with the authentication token (e.g., OAuth 2.0 access token) required to communicate securely with Salesforce APIs.
-AgentforceInstrumentation: A handler for the SDK to emit instrumentation and telemetry data into your app's analytics system.

#### AgentforceNetwork Implementation:
```kotlin
import com.salesforce.android.mobile.interfaces.network.Network
import com.salesforce.android.mobile.interfaces.network.NetworkRequest
import com.salesforce.android.mobile.interfaces.network.NetworkResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class MyAppNetwork(private val okHttpClient: OkHttpClient) : Network {
   override suspend fun perform(request: NetworkRequest): NetworkResponse {
      return withContext(Dispatchers.IO) {
         val okHttpRequest = Request.Builder().apply {
            url(request.path)
            request.headers.forEach { (key, value) -> addHeader(key, value) }

            when (request.method) {
               NetworkRequest.Method.POST -> {
                  val mediaType = request.contentType?.toMediaType() ?: "application/json".toMediaType()
                  post(request.body?.toRequestBody(mediaType))
               }
               NetworkRequest.Method.GET -> get()
               NetworkRequest.Method.PUT -> {
                  val mediaType = request.contentType?.toMediaType() ?: "application/json".toMediaType()
                  put(request.body?.toRequestBody(mediaType))
               }
               NetworkRequest.Method.DELETE -> delete()
               else -> throw IllegalArgumentException("Unsupported HTTP method: ${request.method}")
            }
         }.build()

         val response = okHttpClient.newCall(okHttpRequest).execute()

         response.use { // Properly close the response
            NetworkResponse(
               body = it.body?.string()?.toByteArray(),
               statusCode = it.code,
               headers = it.headers.toMap()
            )
         }
      }
   }
}
```

#### AgentforceAuthCredentialProvider Implementation:
```kotlin
import com.salesforce.android.agentforceservice.AgentforceAuthCredentialProvider
import com.salesforce.android.agentforceservice.AgentforceAuthCredentials
import com.salesforce.android.mobile.interfaces.logging.Logger

class MyAppCredentialProvider(
   private val userSession: UserSession,
   private val logger: Logger? = null
) : AgentforceAuthCredentialProvider {
   override fun getAuthCredentials(): AgentforceAuthCredentials {
      return try {
         val token = userSession.getAuthToken()
         if (token.isBlank()) {
            logger?.e("Auth token is empty")
            throw IllegalStateException("Authentication token is empty")
         }
         AgentforceAuthCredentials(
            authToken = token,
            orgId = userSession.getOrgId(),
            userId = userSession.getUserId()
         )
      } catch (e: Exception) {
         logger?.e("Failed to fetch auth credentials", e)
         throw IllegalStateException("Failed to fetch authentication credentials", e)
      }
   }
}
```

### Option A: Full UI (with `AgentforceClient`)
This approach is the fastest way to get a complete, out-of-the-box chat interface running in your app. The `AgentforceClient` manages the session and provides a `AgentforceLauncherContainer` Composable that presents the chat UI.

#### 1. Create an `AgentforceConfiguration` Instance
The `AgentforceConfiguration` object holds the essential settings for connecting to your agent. You'll need to use the builder pattern to create the configuration.

```kotlin
import com.salesforce.android.agentforcesdkimpl.configuration.AgentforceConfiguration
import com.salesforce.android.agentforceservice.AgentforceAuthCredentialProvider
import com.salesforce.android.agentforceservice.AgentforceInstrumentationHandler
import com.salesforce.android.mobile.interfaces.logging.Logger
import com.salesforce.android.mobile.interfaces.network.Network
import com.salesforce.android.mobile.interfaces.navigation.Navigation

// First, create your implementations of the required interfaces
val authCredentialProvider = MyAppCredentialProvider(userSession)
val network = MyAppNetwork(okHttpClient)
val logger = MyAppLogger() // Your implementation of Logger
val navigation = MyAppNavigation() // Your implementation of Navigation

// Then create the configuration using the builder
val agentforceConfig = AgentforceConfiguration.builder(authCredentialProvider)
   .setSalesforceDomain("https://your-domain.my.salesforce.com")
   .setAgentId("YOUR_AGENT_ID")
   .setNetwork(network)
   .setLogger(logger)
   .setNavigation(navigation)
   // Optional configurations
   .setInstrumentationHandler(myInstrumentationHandler) // Optional: for analytics
   .setViewProvider(myViewProvider) // Optional: for custom UI components
   .build()
```

The configuration requires several mandatory components:
- `authCredentialProvider`: Your implementation of `AgentforceAuthCredentialProvider`
- `salesforceDomain`: Your Salesforce instance URL
- `agentId`: The ID of your configured agent
- `network`: Your implementation of the Network interface
- `logger`: Your implementation of the Logger interface
- `navigation`: Your implementation of the Navigation interface

Optional components include:
- `instrumentationHandler`: For analytics and telemetry
- `viewProvider`: For customizing UI components
- `featureFlagSettings`: For enabling/disabling specific features
- `isVoiceModeEnabled`: For voice interaction capabilities
- `cameraUriProvider`: For handling image uploads
- `onboardingManager`: For managing user onboarding
- `voiceManaging`: For voice interaction management
- `readbackManaging`: For text-to-speech capabilities
- `delegate`: For UI customization
- `connectionInfo`: For connection settings
- `dataProvider`: For custom data sources
- `user`: For user information
- `permission`: For permission handling

#### 2. Initialize the SDK and Build the View
Instantiate `AgentforceClient` and add the `AgentforceLauncherContainer` to your Composable UI.

##### Instantiate `AgentforceClient`
Create and retain an instance of `AgentforceClient`. It's best to hold this in a lifecycle-aware component, like a `ViewModel`, to ensure the conversation state persists across configuration changes.

```kotlin
import androidx.lifecycle.ViewModel
import com.salesforce.android.agentforcesdkimpl.AgentforceClient
import com.salesforce.android.agentforceservice.AgentforceServiceProvider

class MyViewModel : ViewModel() {
   // Retain the client in a ViewModel
   val agentforceClient: AgentforceClient

   init {
      // Assume you have access to your config and interface implementations
      val serviceProvider = AgentforceServiceProvider(
         network = MyAppNetwork(okHttpClient), // Your implementation
         credentialProvider = MyAppCredentialProvider(userSession) // Your implementation 
         // Pass optional implementations here (logger, delegate, etc.)
      )

      agentforceClient = AgentforceClient(
         configuration = agentforceConfig, // Your config from the previous step
         serviceProvider = serviceProvider
      )
   }
}
```

##### Build and Present the View
Use the `AgentforceLauncherContainer` Composable in your screen. It displays a Floating Action Button (FAB) that, when tapped, presents the full chat container.

```kotlin
import androidx.compose.runtime.Composable
import com.salesforce.android.agentforcesdk.ui.AgentforceLauncherContainer
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun MyScreen(viewModel: MyViewModel = viewModel()) {
   // ... Your existing screen content (e.g., inside a Scaffold)

   // Add the Agentforce Launcher
   AgentforceLauncherContainer(client = viewModel.agentforceClient)
}
```

##### Starting a Conversation

Once you have initialized the `AgentforceClient`, you can start a conversation with an agent using the `startAgentforceConversation` method.

```kotlin
// Start the conversation
agentforceClient.startAgentforceConversation()

// Option 1: Get the conversation session using fetchAgentforceSession
val session = agentforceClient.fetchAgentforceSession(YOUR_AGENT_ID)

// Option 2: Create a new AgentforceConversation object
val conversation = AgentforceConversation(
   configuration = configuration,
   conversationService = agentforceClient.conversationService
)
```

Both approaches start a conversation and provide access to an `AgentforceConversation` object, which represents a single conversation with an agent. You can use this session/conversation to interact with the agent and manage the conversation state.

##### Displaying the Chat UI

To display the chat UI, you can use the `AgentforceConversationContainer` Composable. This Composable provides the complete chat interface that you can integrate into your app.

First, create an `AgentforceConversation` object:

```kotlin
// Create the conversation object
val conversation = AgentforceConversation(
   configuration = configuration,
   conversationService = agentforceClient.conversationService
)
```

Then use the `AgentforceConversationContainer` Composable:

```kotlin
import androidx.compose.runtime.Composable
import com.salesforce.android.agentforcesdkimpl.AgentforceConversation

@Composable
fun MyChatScreen(conversation: AgentforceConversation, agentforceClient: AgentforceClient) {
   agentforceClient.AgentforceConversationContainer(
      conversation = conversation,
      onClose = {
         // Handle chat view close
      }
   )
}
```

The `AgentforceConversationContainer` Composable takes the following parameters:

-   `conversation`: The `AgentforceConversation` object that you created
-   `onClose`: A lambda that will be called when the user closes the chat view

You can also use the `AgentforceLauncherContainer` for a floating action button approach:

```kotlin
@Composable
fun MyScreen() {
   // Your existing screen content

   // Add the Agentforce Launcher (FAB)
   AgentforceLauncherContainer(client = agentforceClient)
}
```

## Basic Use Cases

### Sending a Message
To send a message to the agent programmatically, you can use a method like `sendUtterance` on your `AgentforceConversation` object:

```kotlin
// Example: Sending a message with an optional attachment
conversation.sendUtterance(
   utterance = "Hello, world!",
   attachment = null // or provide an AgentforceAttachment object
)
```

### Receiving Messages
To receive messages from the agent, you can use the `conversationManager` property on the `AgentforceConversation` object and its `sendMessage` method which returns a `ReceiveChannel<AgentforceComponent>`:

```kotlin
// Example: Using conversationManager to send and receive messages
val responseChannel = conversation.conversationManager.sendMessage(
   inputRepresentation = ConversationInputRepresentation("Hello, world!"),
   agentforceAttachment = null, // Optional attachment
   file = null // Optional file
)
```

### Handling UI Events

To handle UI events, you can implement the `AgentforceUIDelegate` interface. This interface has the following methods:

-   `modifyUtteranceBeforeSending(utterance: AgentforceUtterance)`: This method is called before an utterance is sent to the agent. It allows you to modify the utterance before it is sent.
-   `didSendUtterance(utterance: AgentforceUtterance)`: This method is called after an utterance has been sent to the agent.
-   `userDidSwitchAgents(newConversation: AgentforceConversation)`: This method is called when the user switches to a different agent.

```kotlin
class YourActivity : AgentforceUIDelegate {
   override suspend fun modifyUtteranceBeforeSending(utterance: AgentforceUtterance): AgentforceUtterance {
      // Modify the utterance if needed
      return utterance
   }

   override fun didSendUtterance(utterance: AgentforceUtterance) {
      // Handle sent utterance
   }

   override fun userDidSwitchAgents(newConversation: AgentforceConversation) {
      // Handle agent switch
   }
}
```

### Option B: Headless (with `AgentforceService`)
This approach is for developers who want to build a completely custom UI or run agent interactions in the background. The `AgentforceService` handles the communication and state, emitting events via Kotlin Flows that your app uses to update its own UI or trigger logic.

#### 1. Instantiate `AgentforceService`
Use the `AgentforceServiceProvider` to create an `AgentforceService` instance for a specific agent.

```kotlin
import com.salesforce.android.agentforceservice.AgentforceServiceProvider
import com.salesforce.android.agentforceservice.AgentforceService
import com.salesforce.android.agentforceservice.AgentforceServerSentEvents
import com.salesforce.android.mobile.interfaces.logging.Logger
import com.salesforce.android.mobile.interfaces.network.Network
import java.util.Locale

// Create the service provider with required dependencies
val serviceProvider = AgentforceServiceProvider(
   configurationLocale = Locale.getDefault(), // Optional: Your app's locale
   domain = "https://your-domain.my.salesforce.com", // Your Salesforce domain
   network = MyAppNetwork(okHttpClient), // Your Network implementation
   credentialProvider = MyAppCredentialProvider(userSession), // Your credential provider
   instrumentationHandler = myInstrumentationHandler, // Optional: For analytics
   logger = myLogger // Optional: For logging
)

// Get the service for your specific agent
val agentId = "YOUR_AGENT_ID"
val agentforceService: AgentforceService = serviceProvider.getAgentforceService(agentId)
```

#### 2. Start a Session and Handle Events
Start a session by calling `startSession()` and then use `sendMessageAndStartStreaming()` to send messages and receive streaming responses. You'll need to handle the streaming responses in your UI.

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.salesforce.android.agentforceservice.AgentforceService
import com.salesforce.android.agentforceservice.conversationservice.data.AgentforceResponse
import com.salesforce.android.agentforceservice.conversationservice.data.StreamingCapabilities
import com.salesforce.android.agentforceservice.conversationservice.data.StreamingRequest
import com.salesforce.android.agentforceservice.conversationservice.data.ChunkType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel(private val agentforceService: AgentforceService) : ViewModel() {
   private var currentSessionId: String? = null
   private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
   val messages = _messages.asStateFlow()

   fun startSession() {
      viewModelScope.launch {
         try {
            // Configure streaming capabilities
            val streamingCapabilities = StreamingCapabilities(
               chunkTypes = mutableListOf(ChunkType.TEXT.type).apply {
                  if (AgentforceClient.agentforceFeatureFlagSettings.enableLightningTypeStreaming) {
                     add(ChunkType.LIGHTNING.type)
                  }
               }
            )

            // Start the session
            val session = agentforceService.startSession(streamingCapabilities)
            currentSessionId = session.sessionId

            // Handle initial messages if any
            session.messages.forEach { message ->
               processMessage(message)
            }
         } catch (e: Exception) {
            // Handle session start error
         }
      }
   }

   fun sendMessage(text: String) {
      val sessionId = currentSessionId ?: return
      viewModelScope.launch {
         try {
            // Create streaming request
            val streamingRequest = StreamingRequest(
               message = text,
               // Add any additional request parameters
            )

            // Send message and start streaming
            val responseChannel = agentforceService.sendMessageAndStartStreaming(
               sessionId = sessionId,
               inputRepresentation = streamingRequest
            )

            // Process streaming responses
            for (response in responseChannel) {
               processMessage(response.message)
            }
         } catch (e: Exception) {
            // Handle message sending error
         }
      }
   }

   private fun processMessage(message: AgentforceMessage) {
      // Process different message types
      when (message.type) {
         "progressindicator" -> {
            // Handle progress indicator
         }
         "inform" -> {
            // Handle informational message
         }
         "inquire" -> {
            // Handle inquiry message
         }
         // Handle other message types...
      }
   }

   override fun onCleared() {
      super.onCleared()
      // Cancel any ongoing streaming
      agentforceService.cancelCurrentStreaming()
   }
}
```

The session flow works as follows:
1. Call `startSession()` to initialize a new session with the agent
2. The session response includes a `sessionId` and any initial messages
3. Use `sendMessageAndStartStreaming()` to send messages and receive streaming responses
4. Process the streaming responses in your UI, handling different message types:
   - `progressindicator`: Shows that the agent is processing
   - `inform`: Contains information or responses from the agent
   - `inquire`: Contains questions or requests from the agent
5. Use `cancelCurrentStreaming()` to stop any ongoing streaming when needed

## Advanced Topics

### Custom View Providers

The `AgentforceViewProvider` interface allows you to provide your own custom views for the different components that are displayed in the chat interface. This is useful if you want to replace the default implementation of a component with your own.

To provide your own views, you must create a class that implements the `AgentforceViewProvider` interface and implement the following methods:

-   `canHandle(definition: String)`: This method should return `true` if you want to provide a custom view for the given component definition, and `false` otherwise.
-   `GetView(modifier: Modifier, view: AgentforceComponent)`: This method should return your custom Composable for the given component. The `view` parameter contains the component data and definition.

Here is an example of how you can provide a custom view for the `AF_RICH_TEXT` component:

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.salesforce.android.agentforcesdk.components.models.AgentforceComponent
import com.salesforce.android.agentforcesdk.components.models.AgentforceViewProvider

class CustomViewProvider : AgentforceViewProvider {
   override fun canHandle(definition: String): Boolean {
      return definition == "AF_RICH_TEXT"
   }

   @Composable
   override fun GetView(modifier: Modifier, view: AgentforceComponent) {
      // Access component data from the view parameter
      val value = view.data["value"] as? String ?: ""
      androidx.compose.material3.Text(
         text = value,
         fontSize = 16.sp,
         fontWeight = FontWeight.Normal,
         modifier = modifier
      )
   }
}
```

Once you have created your custom view provider, you can register it with a `ViewProviderService` and pass it to the `AgentforceConfiguration` when you initialize it:

```kotlin
// Create and register your view provider
val viewProviderService = DemoViewProviderServiceImpl()
viewProviderService.register(CustomViewProvider())

val agentforceConfig = AgentforceConfiguration.builder(authCredentialProvider)
   .setSalesforceDomain("https://your-domain.my.salesforce.com")
   .setAgentId("YOUR_AGENT_ID")
   .setNetwork(network)
   .setLogger(logger)
   .setNavigation(navigation)
   .setViewProvider(viewProviderService) // Add your view provider service
   .build()
```

### Instrumentation

The Agentforce SDK provides a detailed instrumentation framework for monitoring performance and usage. To receive instrumentation events, you can provide an implementation of the `AgentforceInstrumentationHandler` interface in your `AgentforceConfiguration`.

```kotlin
import com.salesforce.android.agentforceservice.AgentforceInstrumentationHandler
import com.salesforce.android.agentforceservice.AgentforceInstrumentationEvent
import com.salesforce.android.mobile.interfaces.logging.Logger

class MyInstrumentationHandler(private val logger: Logger? = null) : AgentforceInstrumentationHandler {
   override fun recordEvent(event: AgentforceInstrumentationEvent) {
      // Handle instrumentation events
      logger?.d("Instrumentation event: $event")

      // You can send events to your analytics service here
      // For example, using Firebase Analytics, Mixpanel, etc.
      when (event) {
         is AgentforceInstrumentationEvent.MessageSent -> {
            // Track message sent events
            logger?.d("Message sent: ${event.messageType}")
         }
         is AgentforceInstrumentationEvent.MessageReceived -> {
            // Track message received events
            logger?.d("Message received: ${event.messageType}")
         }
         is AgentforceInstrumentationEvent.SessionStarted -> {
            // Track session start events
            logger?.d("Session started: ${event.sessionId}")
         }
         is AgentforceInstrumentationEvent.SessionEnded -> {
            // Track session end events
            logger?.d("Session ended: ${event.sessionId}")
         }
         is AgentforceInstrumentationEvent.Error -> {
            // Track error events
            logger?.e("Agentforce error: ${event.error}")
         }
         else -> {
            // Handle other event types
            logger?.d("Other event: $event")
         }
      }
   }
}
```

Include the instrumentation handler in your configuration:

```kotlin
val agentforceConfig = AgentforceConfiguration.builder(authCredentialProvider)
   .setSalesforceDomain("https://your-domain.my.salesforce.com")
   .setAgentId("YOUR_AGENT_ID")
   .setNetwork(network)
   .setLogger(logger)
   .setNavigation(navigation)
   .setInstrumentationHandler(MyInstrumentationHandler(logger)) // Add your instrumentation handler
   .build()
```
## Support and Resources

For additional documentation, examples, and support:

- [Salesforce Developer Documentation](https://developer.salesforce.com)
- [Salesforce Mobile Interfaces](https://github.com/forcedotcom/SalesforceMobileInterfaces-Android)


## Contributing

See `CONTRIBUTING.md`

## License

This project is licensed under the terms specified in the `LICENSE.txt` file.
