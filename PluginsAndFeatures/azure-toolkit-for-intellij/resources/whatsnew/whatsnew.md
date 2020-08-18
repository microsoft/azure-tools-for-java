<!-- Version: 3.40.0 -->
# What's new in Azure Toolkit for IntelliJ

## 3.41.0

### Changed
- Changed default tomcat version for app service to tomcat 9.0
- Scaling Spring Cloud deployment before deploy artifacts

### Fixed
- [#4490](https://github.com/microsoft/azure-tools-for-java/issues/4490) Fix plugin initialization exceptions while parsing configuration
- [#4511](https://github.com/microsoft/azure-tools-for-java/issues/4511) Fix `AuthMethodManager` initialization issues
- [#4532](https://github.com/microsoft/azure-tools-for-java/issues/4532) Fix NPE in FunctionRunState and add validation for function run time
- [#4534](https://github.com/microsoft/azure-tools-for-java/pull/4534) Create temp folder as function run/deploy staging folder in case staging folder conflict
- [#4552](https://github.com/microsoft/azure-tools-for-java/issues/4552) Fix thread issues while prompt tooltips for deployment slot 
- [#4553](https://github.com/microsoft/azure-tools-for-java/pull/4553) Fix deployment target selection issue after create new webapp

## 3.40.0

### Added
- Support IntelliJ 2020.2

### Changed
- Show non-anonymous HTTP trigger urls after function deployment

## 3.39.0

[Azure CLI](https://docs.microsoft.com/cli/azure/) is supported as a sign in option. You can now sign in with single click if you have already signed in with Azure CLI.

![azure-cli-auth](https://user-images.githubusercontent.com/12445236/86110888-b1734a00-baf8-11ea-9d12-6bffef0d6823.gif)

Besides, Azure toolkit add new support on [Azure Synapse](https://docs.microsoft.com/en-us/azure/synapse-analytics/spark/intellij-tool-synapse) now! The following features are waiting to be discovered.
 - Monitor Spark batch job on Spark history server UI and Spark job details UI

 ![monitor-spark-batch-job](https://user-images.githubusercontent.com/32627233/86082318-0ba8e680-baca-11ea-97db-bfdb21c33310.gif)

 - Run Apache Spark Livy interactive console on Azure Synapse

![spark-interactive-console](https://user-images.githubusercontent.com/32627233/84374160-319c4300-ac10-11ea-902a-f98dec9d6f99.gif)

### Added
- Support Azure Functions with Java 11 runtime(Preview)
- Support authentication with Azure CLI credentials

### Changed
- Show Apache Spark on Cosmos node by default no matter whether there are SoC clusters under user's subscription or not
- Remove Docker Host in Azure Explorer

### Fixed
- Fix Spark history server link broken for Azure Synapse issue
- [#3712](https://github.com/microsoft/azure-tools-for-java/issues/3712) Fixes NPE while refreshing Azure node
- [#4449](https://github.com/microsoft/azure-tools-for-java/issues/4449) Fixes NPE while parsing Function bindings
- [#2226](https://github.com/microsoft/azure-tools-for-java/issues/2226) Fixes AuthException for no subscrition account
- [#4102](https://github.com/microsoft/azure-tools-for-java/issues/4102) Fixes Exception when app service run process is terminated
- [#4389](https://github.com/microsoft/azure-tools-for-java/issues/4389) Fixes check box UI issue when create function project
- [#4307](https://github.com/microsoft/azure-tools-for-java/issues/4307) Selecting wrong module automatically when adding function run configuration for gradle function project


## 3.38.0
We are proud to announce the support for [Azure Synapse](https://docs.microsoft.com/en-us/azure/synapse-analytics/spark/intellij-tool-synapse) in Azure toolkit! The following features are waiting to be discovered.
 - List Azure Synapse workspaces and Apache Spark pools

![list-workspaces-pools-blur](https://user-images.githubusercontent.com/32627233/84378742-3dd7ce80-ac17-11ea-871f-d85e9bedf99f.gif)

 - Submit Apache Spark batch jobs to Apache Spark pools

![synapse-submit-middle-quick](https://user-images.githubusercontent.com/32627233/84487331-2f4ded80-acd1-11ea-8fe1-b6cc15b9371b.gif)

### Added
- Support create application insights connection while creating new function app

### Changed
- Deprecate Docker Host(will be removed in v3.39.0)

### Fixed
- [#4423](https://github.com/microsoft/azure-tools-for-java/issues/4423) Spark local run mockfs issue with Hive support enabled
- [#4410](https://github.com/microsoft/azure-tools-for-java/issues/4410) The context menu <code>Submit Spark Application</code> action regression issue at IDEA 2020.1
- [#4419](https://github.com/microsoft/azure-tools-for-java/issues/4419) The run configuration Spark config table changes didn't take effects regression
- [#4413](https://github.com/microsoft/azure-tools-for-java/issues/4413) The regression issue of Spark local console with Scala plugin 2020.1.36
- [#4422](https://github.com/microsoft/azure-tools-for-java/issues/4422) Fixes <code>ConcurrentModificationException</code> while refreshing spring cloud clusters
- [#4438](https://github.com/microsoft/azure-tools-for-java/issues/4438) Fixes modality state issue when open what's new document

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

## Summary

The plugin allows Java developers to easily develop, configure, test, and deploy highly available and scalable Java web apps. It also supports Azure Synapse data engineers, Azure HDInsight developers and Apache Spark on SQL Server users to create, test and submit Apache Spark/Hadoop jobs to Azure from IntelliJ on all supported platforms.

#### Features
- Azure Web App Workflow: Run your web applications on Azure Web App and view logs.
- Azure Functions Workflow: Scaffold, run, debug your Functions App locally and deploy it on Azure.
- Azure Spring Cloud Workflow: Run your Spring microservices applications on Azure Spring CLoud and- view logs.
- Azure Container Workflow: You can dockerize and run your web application on Azure Web App (Linux)- via Azure Container Registry.
- Azure Explorer: View and manage your cloud resources on Azure with embedded Azure Explorer.
- Azure Resource Management template: Create and update your Azure resource deployments with ARM- template support.
- Azure Synapse: List workspaces and Apache Spark Pools, compose an Apache Spark project, author and submit Apache Spark jobs to Azure Synapse Spark pools.
- Azure HDInsight: Create an Apache Spark project, author and submit Apache Spark jobs to HDInsight cluster; Monitor and debug Apache Spark jobs easily; Support HDInsight ESP cluster MFA Authentication.
- Link to SQL Server Big Data Cluster; Create an Apache Spark project, author and submit Apache Spark jobs to cluster; Monitor and debug Apache Spark jobs easily.
