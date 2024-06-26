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
1. #### Using Connection Strings to build Azure Service Clients
   Anti-Pattern:Using Connection Strings to build Azure Service Clients
   Issue: While it’s often used in many applications, its usage is not recommended in Azure SDKs for Java due to potential security vulnerabilities
    ##### Severity: WARNING.
   #### Solution: Use DefaultAzureCredential for azure service client authentication instead. [Click here for more details](https://learn.microsoft.com/en-us/java/api/com.azure.identity.defaultazurecredential?view=azure-java-stable)