<!-- Version: 3.40.0 -->
# What's new in Azure Toolkit for IntelliJ

## 3.43.0

### Added
- Support SSH into Linux web app

### Fixed
- [#4555](https://github.com/microsoft/azure-tools-for-java/issues/4555) Azure CLI authentication does not show subscriptions for all tenants
- [#4558](https://github.com/microsoft/azure-tools-for-java/issues/4558) Unhandled exception in device login
- [#4560](https://github.com/microsoft/azure-tools-for-java/issues/4560) Unhandled exception while create application insights
- [#4595](https://github.com/microsoft/azure-tools-for-java/pull/4595) Unhandled exception in Docker Run/Run on Web App for Containers
- [#4601](https://github.com/microsoft/azure-tools-for-java/issues/4601) Fixed customized configuration are wrongly cleared after blob storage is slected for Synapse batch job issue
- [#4607](https://github.com/microsoft/azure-tools-for-java/pull/4607) Fixed regression in service principal authentication

## 3.42.0

### Added
- Support Custom Binding for Azure Functions

### Fixed
- [#1110](https://github.com/microsoft/azure-maven-plugins/issues/1110) Fixes XSS issue in authentication 

## 3.41.1

### Fixed
- [#4576](https://github.com/microsoft/azure-tools-for-java/issues/4576) Can not list webapps in web app deployment panel

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
