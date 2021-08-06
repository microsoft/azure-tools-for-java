<!-- Version: 3.55.0 -->
# What's new in Azure Toolkit for IntelliJ

## 3.55.0
### Added
- New Azure Resource Connector explorer for connection management
  - List all resource connections connected to project
  - Create new connections between Azure resources and module in project
  - Edit/Delete existing connections
  - Navigate to resource properties view of an existing connection

  <img src="https://user-images.githubusercontent.com/19339116/127592005-f7c9dd33-40de-4031-bba0-2a96e8e2fbd2.gif" />
- Support native proxy settings in IntelliJ

  <img src="https://user-images.githubusercontent.com/12445236/127419581-c3f8b3f6-891e-46e2-bf34-3f69676bd03b.png" />
- Add unified `Open In Portal` support for Web App/VM/Resource Group in Azure explorer

  <img src="https://user-images.githubusercontent.com/12445236/127419961-5e918811-96c6-41f7-b438-af97bc959abe.png" />

### Changed
- Enhance toolkit setting panel with more configuration

  <img src="https://user-images.githubusercontent.com/12445236/127420059-46bcc63e-525c-413b-9bba-b7e3de93502d.png" />
- Enhance resource loading performance in Azure explorer
- Support turn off Azure SDK deprecation notification
- Support create Azure Spring Cloud app in Azure explorer
- Update Azure icons to new style

### Fixed
- [#5439](https://github.com/microsoft/azure-tools-for-java/issues/5439) Fix project already disposed excpetion while loading azure sdk reference book meta data
- [PR#5437](https://github.com/microsoft/azure-tools-for-java/pull/5437) Fix exception while edit json in service principal dialog
- [PR#5476](https://github.com/microsoft/azure-tools-for-java/pull/5476) Fix url render issue for toolkit notification
- [PR#5535](https://github.com/microsoft/azure-tools-for-java/pull/5535) Fix evaluate effective pom will break app service/spring cloud deployment
- [PR#5563](https://github.com/microsoft/azure-tools-for-java/pull/5563) Fix exception: type HTTP is not compatible with address null 
- [PR#5579](https://github.com/microsoft/azure-tools-for-java/pull/5579) Fix reporting error in azure explorer before sign in

## 3.54.0
### Added
- User would be reminded if deprecated Azure SDK libs are used in project.
- Development workflow for SQL Server on Azure: user can now connect SQL Server to local project from Azure Explorer, project, module or application.properties file.

### Changed
- Services are grouped by category in Azure SDK reference book so that user can quickly locate the libs they want.
- Error messages are unified.

## 3.53.0
### Added
- Management workflow for Azure SQL Server
- New login ui for service principal authentication

### Changed
- Deprecated file based service principal authentication

### Fixed
- [PR #5228](https://github.com/microsoft/azure-tools-for-java/pull/5228) Fix OAuth/Device login could not be cancelled

## 3.52.0
### Added
- Support OAuth for authentication
- Add support for management/client sdk in Azure SDK reference book

### Changed
- Improve UI for azure service connector

### Fixed
- [#5121](https://github.com/microsoft/azure-tools-for-java/issues/5121) Fix project disposed exception for workspace tagging
- [PR #5163](https://github.com/microsoft/azure-tools-for-java/pull/5163) Fix enable local access may not work for Azure MySQL

## 3.51.0
### Added
<img src="https://user-images.githubusercontent.com/69189193/113823126-da73ea00-97b0-11eb-9b69-958f5d5a2b00.gif" width="840" height="525" />

- Add support for IntelliJ 2021.1 EAP
- Add Azure SDK reference book for Spring

### Changed
- Improve resource list performance with cache and preload
- Update Azure related run configuration icons
- Continue with warning for multi-tenant issues while getting subscriptions
- Remove preview label for function and spring cloud

### Fixed
- [#5002](https://github.com/microsoft/azure-tools-for-java/issues/5002) Failed to run Spark application with filled-in default Azure Blob storage account credential
- [#5008](https://github.com/microsoft/azure-tools-for-java/issues/5008) IndexOutOfBoundsException while create MySQL connection
- [PR #4987](https://github.com/microsoft/azure-tools-for-java/pull/4987) InvalidParameterException occurs when close a streaming log
- [PR #4987](https://github.com/microsoft/azure-tools-for-java/pull/4987) Failed when select file to deploy to Azure Web App 
- [PR #4998](https://github.com/microsoft/azure-tools-for-java/pull/4998) Fix IDEA203 regression of Spark failure debug in local
- [PR #5006](https://github.com/microsoft/azure-tools-for-java/pull/5006) Fix NPE of exploring ADLS Gen2 FS in Spark job conf
- [PR #5009](https://github.com/microsoft/azure-tools-for-java/pull/5009) Fix bundle build JCEF issue
- [PR #5014](https://github.com/microsoft/azure-tools-for-java/pull/5014) Failed to create MySQL instance as resource provider is not registered 
- [PR #5055](https://github.com/microsoft/azure-tools-for-java/pull/5055) Can't deploy to Azure Web App when there is "Connect Azure Resource" in before launch

## 3.50.0

### Added
<img src="https://user-images.githubusercontent.com/19339116/109937625-11666400-7d0a-11eb-9850-82a62d65f3fa.gif" width="840" height="525" />

- Development workflow for Azure Database for MySQL
  - Connect Azure Database for MySQL Server to local project from Azure Explorer or application.properties file
  - Automatically inject datasource connection properties into runtime environment for local run
  - Publish Azure Web App with datasource connection properties in application settings

## 3.49.0

### Changed
- Collect performance metrics data via telemetry for future performance tuning.
- Update the status text on progress indicator.
- Update context menu icons in Azure Explorer.

## 3.48.0

### Changed
- Update icons in Azure toolkits
- Update Tomcat base images
- Using non-blocking UI to replace blocking progress indicator
- Remove non-functional "cancel" buttons in foreground operations

## 3.47.0

### Added
<img src="https://user-images.githubusercontent.com/19339116/102885897-15b30d00-448f-11eb-9733-ecbf77ee9760.gif" width="840" height="525" />

- Add Azure Database for MySQL support in Azure Toolkits
  - Manage Azure Database for MySQL instance (create/start/stop/restart/configure/show properties)
  - Configure Azure Database for MySQL to allow access it from azure services and local PC
  - Show sample of JDBC connection strings on Azure Database for MySQL
  - Open and connect to Azure Database for MySQL server by Intellij database tools
- Add Stacktrace filter in Spark console
- Enable speed search in subscription table
- Enable speed search in Azure explorer tree

### Changed
- Upgrade Azure Blob batch SDK to 12.7.0
- Enhance App Service file explorer in Azure explorer

### Fixed
- [#4801](https://github.com/microsoft/azure-tools-for-java/issues/4801) Spark tools library serializer potential issues
- [#4808](https://github.com/microsoft/azure-tools-for-java/issues/4808) Fixes unable to attach function host while running functions
- [#4814](https://github.com/microsoft/azure-tools-for-java/issues/4814) Spark livy console staring being blocked by artifacts uploading failure
- [#4823](https://github.com/microsoft/azure-tools-for-java/issues/4823) Compiling warnings of ConfigurationFactory.getId being deprecated
- [#4827](https://github.com/microsoft/azure-tools-for-java/issues/4827) Fix HDInsight cluster can't link non-cluster-default Azure Blob storage account issue
- [#4829](https://github.com/microsoft/azure-tools-for-java/issues/4829) UI hang issue with changing Spark Synapse run configuration ADLS Gen2 storage key settings

## 3.46.0

### Added
- Support IntelliJ 2020.3 RC

### Changed
- Refactor error handling, unify the error notifications

### Fixed
- [#4764](https://github.com/microsoft/azure-tools-for-java/pull/4764) Fixes HDInsights clusters of all subscriptions(instead of the selected subscription) is listed 
- [#4766](https://github.com/microsoft/azure-tools-for-java/pull/4766) Fixes duplicate before run task for Spring Cloud deployment run configuration
- [#4784](https://github.com/microsoft/azure-tools-for-java/pull/4784) Fixes failed to auth with Azure CLI with multi Azure environment enabled


## 3.45.1
### Fixed
- [#4765](https://github.com/microsoft/azure-tools-for-java/pull/4765) Fixes no before run tasks when deploy Spring Cloud app in project menu


## 3.45.0
Flight recorder support was added in the latest Azure Toolkit for IntelliJ v3.45.0! You may monitor your online app
 service in Azure explorer with single click and analyze the report with [Zulu Mission Control](https://www.azul.com/products/zulu-mission-control/) or IntelliJ (requires 2020.2 above Ultimate).

<img src="https://user-images.githubusercontent.com/12445236/98444079-b0a79000-214a-11eb-8015-51cd2a05a1c1.gif" width="800" height="600" />

Besides, file explorer is also added to Azure explorer, you could view and manage your files and logs in app service
 with this new feature.

![File Explorer](https://user-images.githubusercontent.com/12445236/98444253-93bf8c80-214b-11eb-8759-8176b520d515.png)

For all the updates and fixes in this release, please refer the release notes below.

### Added
- Add file explorer for Web App and Function App in Azure explorer
- Support flight recorder for Web App

### Changed
- New creation wizard for Function App with basic and advanced mode
- More monitoring configuration in Web App/Function App creation wizard
- Update template for function project

### Fixed
- [#4703](https://github.com/microsoft/azure-tools-for-java/pull/4703) Fix NPE issue in Function creation/deployment
- [#4707](https://github.com/microsoft/azure-tools-for-java/pull/4707) Enhace error handling for azure cli token expires
- [#4710](https://github.com/microsoft/azure-tools-for-java/pull/4710) Register service provider for insights before get insights client

## 3.44.0
We are proud to announce the support for JBoss in IntelliJ Toolkit! You can now create JBoss Linux app service with totally new resource creation experience. You may create Web App more easily in the simple mode, while you could still set all the parameters in advance mode if necessary.

<img src="https://user-images.githubusercontent.com/12445236/96878853-e2172f00-14ad-11eb-99d2-05b2b61b0bb1.gif" width="800" height="600" />

Besides, file deploy is also added to Web App support, you could deploy your artifact directly with new entrance in Azure explorer.   

<img src="https://user-images.githubusercontent.com/12445236/96880775-29062400-14b0-11eb-8fee-523fe164c333.png" width="700" height="600" />

For all the updates and fixes in this release, please refer the release notes below.
 
### Added
- Support new runtime JBOSS 7.2 for Linux Web App
- Support Gradle projects for Web App and Spring Cloud
- Support file deploy for Web App

### Changed
- New creation wizard for Web App with basic and advanced mode

### Fixed
- [#2975](https://github.com/microsoft/azure-tools-for-java/issues/2975) ,[#4600](https://github.com/microsoft/azure-tools-for-java/issues/4600) ,[#4605](https://github.com/microsoft/azure-tools-for-java/issues/4605), [#4544](https://github.com/microsoft/azure-tools-for-java/issues/4544) Enhance error handling for network issues
- [#4545](https://github.com/microsoft/azure-tools-for-java/issues/4545), [#4566](https://github.com/microsoft/azure-tools-for-java/issues/4566) Unhandled ProcessCanceledException while start up
- [#4530](https://github.com/microsoft/azure-tools-for-java/issues/4530) Unhandled exception in whats new document
- [#4591](https://github.com/microsoft/azure-tools-for-java/issues/4591) ,[#4599](https://github.com/microsoft/azure-tools-for-java/issues/4599) Fix Spring Cloud deployment error handling
- [#4558](https://github.com/microsoft/azure-tools-for-java/pull/4604) Unhandled exception in device login


## 3.43.0

### Added
- Support SSH into Linux web app

### Changed
- Update Spring Cloud dependency constraint rule for spring-cloud-starter-azure-spring-cloud-client

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
