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
1. #### Kusto Queries Having a Time Interval in the Query String

Writing KQL queries with hard-coded time intervals directly in the query string is an anti-pattern. 
Time intervals include using ago(1d) or between(datetime(2023-01-01), datetime(2023-01-02)). 
This approach makes queries less flexible and harder to troubleshoot.

Consider using the `QueryTimeInterval` parameter in the client method parameters to specify the time interval for the query. 
By passing the time range as an argument in the method call, you make it easier to troubleshoot and understand the context of an API call. 
This approach enhances the flexibility and readability of your Kusto queries.

Please refer to the [Azure SDK for Java documentation](https://learn.microsoft.com/en-us/java/api/com.azure.monitor.query.models.querytimeinterval?view=azure-java-stable) for additional information.