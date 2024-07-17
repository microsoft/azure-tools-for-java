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
1. ### Storage Upload without Length Check
- **Anti-pattern**: Using Azure Storage upload APIs that don’t take a length parameter, causing the entire data payload to be buffered into memory before uploading.
- **Issue**: This can lead to OutOfMemoryErrors, especially with large files or high-volume uploads.
- **Severity: INFO**
- **Recommendation**: Use APIs that take a length parameter. Please refer to the [Azure SDK for Java documentation](https://learn.microsoft.com/en-us/azure/storage/blobs/storage-blob-upload-java) for additional information.


### 2. Use these encouraged clients instead of their corresponding discouraged clients

#### a. Use **`ServiceBusProcessorClient`** instead of **`ServiceBusReceiverAsyncClient`**
#### b. Use **`EventProcessorClient`** instead of **`EventHubConsumerAsyncClient`**

### Anti-pattern:
- Both `ServiceBusReceiverAsyncClient` and `EventHubConsumerAsyncClient` are low-level APIs. They provide fine-grained control over message/event handling but require a high level of proficiency in Reactive programming.
- Due to their complexity and the need for a deep understanding of Reactive programming, there is a higher risk of these clients being used incorrectly or inefficiently, especially by developers who are not familiar with Reactive paradigms.

### Issue:

#### a. **ServiceBusReceiverAsyncClient**
- **Anti-pattern**: The `ServiceBusReceiverAsyncClient` is considered an anti-pattern because it demands detailed handling of messages, which can be overly complex and unnecessary for most common use cases.
    - **Severity: WARNING**
    - **Recommendation**: Instead of using `ServiceBusReceiverAsyncClient`, it is recommended to use `ServiceBusProcessorClient`. The `ServiceBusProcessorClient` is a higher-level abstraction that simplifies message consumption, making it a more suitable option for most developers and scenarios.
    - Please refer to the [Azure SDK for Java documentation](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/servicebus/azure-messaging-servicebus/README.md#when-to-use-servicebusprocessorclient).

#### b. **EventHubConsumerAsyncClient**
- **Anti-pattern**: The `EventHubConsumerAsyncClient` is considered an anti-pattern due to its low-level nature and the complexity involved in event handling.
    - **Severity: WARNING**
    - **Recommendation**: Instead of using `EventHubConsumerAsyncClient`, it is advised to use `EventProcessorClient`. The `EventProcessorClient` provides a higher-level abstraction that simplifies event processing, making it the preferred choice for most developers.
    - Please refer to the [Azure SDK for Java documentation](https://learn.microsoft.com/en-us/java/api/com.azure.messaging.eventhubs.eventprocessorclient?view=azure-java-stable).
