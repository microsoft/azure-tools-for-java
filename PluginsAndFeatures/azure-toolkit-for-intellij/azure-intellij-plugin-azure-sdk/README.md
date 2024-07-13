# Project: Java Code Quality Analyzer

## Project Description
The Java Code Quality Analyzer, is a plugin designed to improve the quality of Java code. It provides an interactive tool window that offers real-time code suggestions. The following are proposed essential features, including rule set integration, telemetry connectivity, and Azure Toolkit integration.

## Key Features

- **Rule Set Integration**: This feature enables users to import and customize rule sets, tailoring the plugin to their specific needs.
- **Telemetry Integration**: This feature connects the plugin to the backend with Application Insights, allowing for efficient data transmission.
- **Azure Toolkit Integration**: This feature allows the plugin to integrate with the IntelliJ Azure toolkit, providing extended functionality.
- **Editor Integration**: This feature offers continuous analysis and real-time code suggestions, enhancing the coding experience.
- **Quick-Fix Actions**: This feature identifies issues and suggests quick actions within the tool window, facilitating immediate problem resolution.

## User Interface
- **Telemetry Configuration Panel** : This space allows users to enable or disable telemetry if desired.


## Rules
1. #### Storage Upload without Length Check
- **Anti-pattern**: Using Azure Storage upload APIs that don’t take a length parameter, causing the entire data payload to be buffered into memory before uploading.
- **Issue**: This can lead to OutOfMemoryErrors, especially with large files or high-volume uploads.
- **Severity: INFO**
- **Recommendation**: Use APIs that take a length parameter. Please refer to the [Azure SDK for Java documentation](https://learn.microsoft.com/azure/storage/blobs/storage-blob-upload-java) for additional information.

2. #### Use ServiceBusProcessorClient instead of ServiceBusReceiverAsyncClient.
- **Anti-pattern**: The use of the Reactor receiver, specifically the `ServiceBusReceiverAsyncClient`, is an anti-pattern. This is because it's a low-level API that provides fine-grained control over message handling. While this might seem beneficial, it requires a high level of proficiency in Reactive programming and is mainly useful when building a Reactive library or an end-to-end Reactive application.
- **Issue**: The main issue with using `ServiceBusReceiverAsyncClient` is its complexity and the requirement for a deep understanding of Reactive programming. This can make it difficult to use correctly and efficiently, especially for developers who are not familiar with Reactive programming paradigms.
- **Severity: WARNING**
- **Recommendation**: Instead of using the low-level `ServiceBusReceiverAsyncClient`, it's recommended to use the `ServiceBusProcessorClient`. The `ServiceBusProcessorClient` is a higher-level abstraction that simplifies message consumption. It's designed for most common use cases and should be the primary choice for consuming messages. This makes it a more suitable option for most developers and scenarios. 
Please refer to the [Azure SDK for Java documentation](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/servicebus/azure-messaging-servicebus/README.md#when-to-use-servicebusprocessorclient) for additional information.

3. #### Use SyncPoller instead of PollerFlux#getSyncPoller()
- **Anti-pattern**: Using `getSyncPoller()` on a `PollerFlux` instance is an anti-pattern. 
- **Issue**: The main issue with using `getSyncPoller()` is that it introduces additional complexity by converting an asynchronous polling mechanism to a synchronous one, which should be avoided.
- **Severity: WARNING**
- **Recommendation**: Instead of using `getSyncPoller()`, it's recommended to use the `SyncPoller` directly to handle synchronous polling tasks. `SyncPoller` provides a synchronous way to interact with the poller and is the preferred method for synchronous operations.
  Please refer to the [Azure SDK for Java documentation](https://learn.microsoft.com/java/api/com.azure.core.util.polling.syncpoller?view=azure-java-stable) for additional information.