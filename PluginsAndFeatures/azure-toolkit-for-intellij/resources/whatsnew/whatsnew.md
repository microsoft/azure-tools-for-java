<!-- Version: 3.37.0 -->
# What's new in Azure Toolkit for IntelliJ

> Scroll down to checkout our newly added support for **Azure Functions**, **Azure Spring Cloud** and
 **log streaming for Azure App Service**.

## 3.38.0(Current Release)
We are proud to announce the support for [Azure Synapse](https://docs.microsoft.com/en-us/azure/synapse-analytics/spark/intellij-tool-synapse) in Azure toolkit! You would have following experiences around Azure Synapse feature in IntelliJ.
 - List Azure Synapse workspaces and Apache Spark pools
 ![list-workspaces-pools](https://user-images.githubusercontent.com/32627233/84374093-18939200-ac10-11ea-9b09-901c7c2bd03f.gif)
 - Submit Apache Spark batch jobs to Apache Spark pools
 ![add-config-submit-batch-job](https://user-images.githubusercontent.com/32627233/84374124-2517ea80-ac10-11ea-8866-b7a6fc0daad6.gif)
 - Run Apache Spark Livy interactive console on Azure Synapse
 ![spark-interactive-console](https://user-images.githubusercontent.com/32627233/84374160-319c4300-ac10-11ea-902a-f98dec9d6f99.gif)

## 3.37.0

### Added
- Add what's new document in Azure menu
- Filter unsupported regions when creating new app service plan

### Changed
- Sort Spark on Cosmos Serverless jobs in descending order by job submission time

### Fixed
- Fixed Spark batch job submission skipped after uploading artifact to SQL Server big data cluster issue
- Fixed no permission issue after submitting Spark batch job to ESP HDInsight cluster with ADLS Gen2 as default storage account type
- [#4370](https://github.com/microsoft/azure-tools-for-java/issues/4370) Fixes NPE while loading Function deployment panel
- [#4347](https://github.com/microsoft/azure-tools-for-java/issues/4347) Fixes NPE while getting action status
- [#4380](https://github.com/microsoft/azure-tools-for-java/pull/4380) Fixes validation may freeze UI in spring cloud deployment panel
- [#4350](https://github.com/microsoft/azure-tools-for-java/issues/4350) Fixes null value in spring cloud property view

## 3.36.0
Azure Toolkit support Log Streaming for App Service now! You could get the live log with log streaming in explorer with simple click.

![logstreaming](https://user-images.githubusercontent.com/12445236/82419555-2b9fc000-9ab1-11ea-8165-0c7fa1b97933.gif)

### Added
- Support log streaming for webapp
- Support open portal Live Metrics Stream for linux function app 
- Validate Azure dependencies version before deploying
- Tag log line with log source(azuretool, livy, driver.stderr) for Spark batch job logs and interactive session logs

### Changed
- Remove version of Azure Spring Cloud dependencies when it is not necessary

### Fixed
- [#4179](https://github.com/microsoft/azure-tools-for-java/issues/4179) Fix NPE caused job submission failure issue
- [#4204](https://github.com/microsoft/azure-tools-for-java/issues/4204) Deploy Azure Spring Cloud App dialog default value is apply
- [#4231](https://github.com/microsoft/azure-tools-for-java/issues/4231) Cannot use Auth file for spring cloud authentication

## 3.35.0

We are proud to announce the support for [Azure Spring Cloud](https://docs.microsoft.com/en-us/azure/spring-cloud/) in Azure Toolkit!

You could have full Azure Spring Cloud experience in IntelliJ Toolkit, which cludes resolve project dependencies
, create/deploy and troubleshooting spring cloud apps. You may try the azure spring cloud in intellij toolkit with the [quick start](https://docs.microsoft.com/en-us/azure/spring-cloud/spring-cloud-tutorial-intellij-deploy-apps?source=intellijwhatsnew).

![Azure Spring Cloud](https://user-images.githubusercontent.com/12445236/82417195-c4344100-9aad-11ea-9791-fd0f33f446cd.png)

### Added
- Add Azure Spring Cloud support in Azure Toolkits
  - Manage Azure Spring Cloud project dependencies
  - Manage Azure Spring Cloud apps in Azure Explorer
    * Create/Delete/Start/Stop/Restart
    * Assign/un-assign public endpoint
    * Update environment variables
    * Update JVM options
    * View app properties
  - Deploying apps from current project
  - Monitoring and troubleshooting apps
    * Getting public url
    * Getting test endpoint
    * Instance status(shown in app properties view)
- Support trigger function with timer trigger
- Support log streaming for Windows functions

### Fixed
- [#4157](https://github.com/microsoft/azure-tools-for-java/issues/4157) Can't trigger function/admin http function when click 'Trigger Function' button
- [#4160](https://github.com/microsoft/azure-tools-for-java/issues/4160) Nothing shown in function run mark
- [#4179](https://github.com/microsoft/azure-tools-for-java/issues/4179) Fixed NPE caused Spark job submission failure in 201EAP
- [#4213](https://github.com/microsoft/azure-tools-for-java/issues/4213) Unhandled error when creating function app
- [#4215](https://github.com/microsoft/azure-tools-for-java/issues/4215) App settings not loaded when openning the deploy wizard

## 3.34.0

We are proud to announce the support for [Azure Functions](https://azure.microsoft.com/en-us/services/functions/) in Azure Toolkit!

Full Azure Function experience are enabled in IntelliJ Toolkit now, which includes create Function projects, running function app locally and deploy your function app to Azure. Please refer  [quick start](https://docs.microsoft.com/azure/developer/java/toolkit-for-intellij/quickstart-functions?source=intellijwhatsnew) to try Azure Functions with IntelliJ.

![Azure Functions](https://user-images.githubusercontent.com/12445236/82418602-c26b7d00-9aaf-11ea-8864-af8d43be713d.jpg)

### Added
- Add Azure Function support in Azure Toolkits
    * Scaffold functions project
    * Create new functions class by trigger type
    * Local run/debug functions
    * Create/deploy Function apps on Azure
    * List/view existing Function apps on Azure
    * Stop/start/restart Function apps on Azure
    * Trigger azure functions
- Support project artifact dependencies in Spark interactive console
- Add more debug log when creating Spark Livy interactive console

### Changed
- Enable Spark on Synapse feature by default

## Summary

These plugins allow Java developers, Azure HDInsight developers and Apache Spark on SQL Server users to easily create, develop, configure, test, and deploy highly available and scalable Java web apps and Apache Spark/Hadoop jobs to Azure from IntelliJ on all supported platforms.

#### Features
- Azure Web App Workflow: Run your web applications on Azure Web App and view logs.
- Azure Functions Workflow: Scaffold, run, debug your Functions App locally and deploy it on Azure.
- Azure Spring Cloud Workflow: Run your Spring microservices applications on Azure Spring CLoud and- view logs.
- Azure Container Workflow: You can dockerize and run your web application on Azure Web App (Linux)- via Azure Container Registry.
- Azure Explorer: View and manage your cloud resources on Azure with embedded Azure Explorer.
- Azure Resource Management template: Create and update your Azure resource deployments with ARM- template support.
- Azure HDInsight: Create an Apache Spark project, author and submit Apache Spark jobs to HDInsight cluster; Monitor and debug Apache Spark jobs easily; Support HDInsight ESP cluster MFA Authentication.
- Link to SQL Server Big Data Cluster; Create an Apache Spark project, author and submit Apache Spark jobs to cluster; Monitor and debug Apache Spark jobs easily.
