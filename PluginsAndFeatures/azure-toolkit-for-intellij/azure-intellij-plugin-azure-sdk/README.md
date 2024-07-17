# Project: Java Code Quality Analyzer

## Project Description

The Java Code Quality Analyzer is a plugin designed to improve the quality of Java code. It provides an interactive tool
window that offers real-time code suggestions. The following are proposed essential features, including rule set
integration, telemetry connectivity, and Azure Toolkit integration.

## Key Features

- **Rule Set Integration**: This feature enables users to import and customize rule sets, tailoring the plugin to their
  specific needs.
- **Telemetry Integration**: This feature connects the plugin to the backend with Application Insights, allowing for
  efficient data transmission.
- **Azure Toolkit for IntelliJ Integration**: This feature allows the plugin to integrate with the Azure Toolkit for
  IntelliJ, providing extended functionality
- **Editor Integration**: This feature offers continuous analysis and real-time code suggestions, enhancing the coding
  experience.
- **Quick-Fix Actions**: This feature identifies issues and suggests quick actions within the tool window, facilitating
  immediate problem resolution.

## User Interface

- **Telemetry Configuration Panel** : This space allows users to enable or disable telemetry if desired.

## Rules

1. #### Storage Upload without Length Check

- **Anti-pattern**: Using Azure Storage upload APIs that don’t take a length parameter, causing the entire data payload
  to be buffered into memory before uploading.
- **Issue**: This can lead to OutOfMemoryErrors, especially with large files or high-volume uploads.
- **Severity: INFO**
- **Recommendation**: Use APIs that take a length parameter. Please refer to
  the [Azure SDK for Java documentation](https://learn.microsoft.com/en-us/azure/storage/blobs/storage-blob-upload-java)
  for additional information.

2. #### Use ServiceBusProcessorClient instead of ServiceBusReceiverAsyncClient.

- **Anti-pattern**: The use of the Reactor receiver, specifically the `ServiceBusReceiverAsyncClient`, is an
  anti-pattern. This is because it's a low-level API that provides fine-grained control over message handling. While
  this might seem beneficial, it requires a high level of proficiency in Reactive programming and is mainly useful when
  building a Reactive library or an end-to-end Reactive application.

- **Issue**: The main issue with using `ServiceBusReceiverAsyncClient` is its complexity and the requirement for a deep
  understanding of Reactive programming. This can make it difficult to use correctly and efficiently, especially for
  developers who are not familiar with Reactive programming paradigms.
- **Severity: WARNING**
- **Recommendation**: Instead of using the low-level `ServiceBusReceiverAsyncClient`, it's recommended to use
  the `ServiceBusProcessorClient`. The `ServiceBusProcessorClient` is a higher-level abstraction that simplifies message
  consumption. It's designed for most common use cases and should be the primary choice for consuming messages. This
  makes it a more suitable option for most developers and scenarios.
  Please refer to
  the [Azure SDK for Java documentation](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/servicebus/azure-messaging-servicebus/README.md#when-to-use-servicebusprocessorclient)
  for additional information.

3. #### Disable Auto-complete when using ServiceBusReceiver or Processor clients

- **Anti-pattern**: When using ServiceBusReceiver or Processor clients, auto-complete is enabled by default, but this
  behavior is not explicitly verified or disabled when necessary.
- **Issue**: Auto-complete being enabled by default might lead to messages being marked as completed even if the message
  processing fails or encounters an error.
  Errors in message processing might not be noticed since the message is automatically completed regardless of success
  or failure, making it harder to identify and handle issues.
- **Severity: WARNING**
- **Recommendation**: Explicitly Disable Auto-Complete: When creating ServiceBusReceiver or Processor clients,
  explicitly use the
  disableAutoComplete() method call to prevent automatic message completion.
  Please refer to
  the [Azure SDK for Java documentation](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.servicebus.servicebusclientbuilder.servicebusreceiverclientbuilder?view=azure-java-stable#com-azure-messaging-servicebus-servicebusclientbuilder-servicebusreceiverclientbuilder-disableautocomplete())
  for additional information.

4. #### Dynamic Client Creation is wasteful

- **Anti-pattern**: Dynamic client creation refers to creating a new client instance for each operation, without reusing
  existing instances.
- **Issue**: This process can be resource-intensive and slow, especially if repeated frequently. It can lead to
  performance issues, increased memory usage, and unnecessary overhead.
- **Severity: WARNING**
- **Recommendation**: Instead of creating a new client instance for each operation, consider reusing existing client
  instances.
  It's recommended to create client instances once and reuse them throughout the application's lifecycle.
  This approach can lead to better performance and efficiency.
  Please refer to
  the [Azure SDK for Java documentation](https://learn.microsoft.com/en-us/azure/developer/java/sdk/overview#connect-to-and-use-azure-resources-with-client-libraries)
  for additional information.

5. #### Hardcoded APIs & Access Tokens Check

- **Anti-pattern**: Hardcoding API keys and tokens in the source code.
- **Issue**: The source code contains hardcoded API keys and tokens, which is a security risk. It exposes sensitive
  information that could be exploited if the code is publicly accessible or falls into the wrong hands.
- **Severity: WARNING**
- **Recommendation**: DefaultAzureCredential is recommended for authentication if the service client supports Token
  Credential (Entra ID Authentication). If not, then use Azure Key Credential for API key based authentication. Please
  refer to
  the [Azure SDK for Java documentation](https://learn.microsoft.com/en-us/java/api/com.azure.identity.defaultazurecredential?view=azure-java-stable)
  for additional information.

6. #### Use SyncPoller instead of PollerFlux#getSyncPoller()

- **Anti-pattern**: Using `getSyncPoller()` on a `PollerFlux` instance is an anti-pattern.
- **Issue**: The main issue with using `getSyncPoller()` is that it introduces additional complexity by converting an
  asynchronous polling mechanism to a synchronous one, which should be avoided.
- **Severity: WARNING**
- **Recommendation**: Instead of using `getSyncPoller()`, it's recommended to use the `SyncPoller` directly to handle
  synchronous polling tasks. `SyncPoller` provides a synchronous way to interact with the poller and is the preferred
  method for synchronous operations.
  Please refer to
  the [Azure SDK for Java documentation](https://learn.microsoft.com/java/api/com.azure.core.util.polling.syncpoller?view=azure-java-stable)
  for additional information.

7. #### Managing Receive Mode and Prefetch Value in Azure Service Bus

- **Anti-pattern**: Setting the receive mode as PEEK_LOCK with a high prefetch value (e.g., 50 or 100) in Azure Service
  Bus.
- **Severity: WARNING**
- **Issue**:
    1. **Suboptimal Performance:** A high prefetch value in PEEK_LOCK mode can result in suboptimal performance, as one
       client
       locks all prefetched messages, potentially leading to processing bottlenecks.
    2. **Message Lock Expiry:** Messages in the prefetch queue do not have their locks renewed automatically.
       Consequently,
       the
       message lock may expire by the time they are processed.
    3. **Dead-Letter Queue:** Expired message locks can result in messages being inadvertently sent to the dead-letter
       queue,
       causing potential data loss or requiring additional handling to recover these messages.
- **Recommendation**: Optimize Prefetch Value - Set a prefetch value that balances between efficient message
  retrieval and the ability for multiple clients to process messages concurrently. Please refer to
  the [Azure SDK for Java documentation](https://learn.microsoft.com/en-us/azure/service-bus-messaging/service-bus-prefetch?tabs=dotnet#why-is-prefetch-not-the-default-option)
  for additional information.

8. #### Use These Encouraged Alternatives Instead of Their Corresponding Discouraged APIs

   #### a. Azure service client authentication instead of Connection Strings to build Azure Service Clients

    - **Anti-pattern**: Using Connection Strings for Authenticating Azure SDK Clients.
    - **Issue**: Connection strings authentication is not recommended in Azure SDKs for Java due to potential
      security
      vulnerabilities.
    - **Severity: WARNING**
    - **Recommendation**: Azure service client authentication is recommended if the service client supports Token
      Credential (Entra ID Authentication). If not, then use Azure Key Credential or Connection Strings based
      authentication. Please refer to
      the [Azure SDK for Java documentation](https://learn.microsoft.com/java/api/com.azure.identity.defaultazurecredential?view=azure-java-stable)
      for additional information.

   #### b. Use Azure OpenAI's `getChatCompletions` for Chat Applications instead of `getCompletions` API

    - **Anti-pattern**: Using the getCompletions API
    - **Issue**: Issue: Functionality Mismatch - The `getCompletions` API is designed for general-purpose completion
      tasks.
      whereas `getChatCompletions` is specifically optimized for conversational contexts.
    - **Severity: WARNING**
    - **Recommendation**: Use `getChatCompletions` for Chat Applications: Specifically use `getChatCompletions` API
      when
      generating responses for chatbot or conversational AI applications.
    - Please refer to
      the [Azure OpenAI client library for Java](https://learn.microsoft.com/java/api/overview/azure/ai-openai-readme?view=azure-java-preview)
      for additional information.
