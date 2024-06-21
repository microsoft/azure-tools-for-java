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
1. #### Use ServiceBusProcessorClient instead of ServiceBusReceiverAsyncClient.
The use of the Reactor receiver, specifically the `ServiceBusReceiverAsyncClient`, is discouraged due to its low-level API. 
This API provides fine-grained control over message handling, making it suitable for scenarios where full control over message processing is required. 
However, it necessitates a proficiency in Reactive programming and is primarily beneficial when constructing a Reactive library or an end-to-end Reactive application.

As a recommendation, consider using the `ServiceBusProcessorClient` instead of the low-level `ServiceBusReceiverAsyncClient`. 
The ServiceBusProcessorClient is a higher-level abstraction that simplifies message consumption and is tailored for most common use cases. 
It should be the primary choice for consuming messages.

Please refer to the [Azure SDK for Java documentation](https://github.com/Azure/azure-sdk-for-java/blob/main/sdk/servicebus/azure-messaging-servicebus/README.md#when-to-use-servicebusprocessorclient) for additional information.