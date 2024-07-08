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
- **Telemetry Configuration Panel**: This space allows users to enable or disable telemetry if desired.
- 
## Rules
1. #### Storage Upload without Length Check
- **Anti-Pattern**: Using Azure Storage upload APIs that don’t take a length parameter, causing the entire data payload to be buffered into memory before uploading.
- **Issue**: This can lead to OutOfMemoryErrors, especially with large files or high-volume uploads.
- **Severity**: INFO. 
- **Recommendation**: Use APIs that take a length parameter. [Click here for more details](https://learn.microsoft.com/en-us/azure/storage/blobs/storage-blob-upload-java)


2. #### Hardcoded APIs & Access Tokens Check
- **Anti-pattern**: Hardcoding API keys and tokens in the source code.
- **Issue**: The source code contains hardcoded API keys and tokens, which is a security risk. It exposes sensitive information that could be exploited if the code is publicly accessible or falls into the wrong hands.
- **Severity**: WARNING.
- **Recommendation**: DefaultAzureCredential is recommended for authentication if the service client supports Token Credential (Entra ID Authentication). If not, then use Azure Key Credential for API key based authentication. Please refer to the [Azure SDK for Java documentation](https://learn.microsoft.com/en-us/java/api/com.azure.identity.defaultazurecredential?view=azure-java-stable)
