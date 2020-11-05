# Change Log

All notable changes to "Azure Toolkit for IntelliJ IDEA" will be documented in this file.

- [Change Log](#change-log)
  - [3.45.0](#3450)
  - [3.44.0](#3440)
  - [3.43.0](#3430)
  - [3.42.0](#3420)
  - [3.41.1](#3411)
  - [3.41.0](#3410)
  - [3.40.0](#3400)
  - [3.39.0](#3390)
  - [3.38.0](#3380)
  - [3.37.0](#3370)
  - [3.36.0](#3360)
  - [3.35.0](#3350)
  - [3.34.0](#3340)
  - [3.33.1](#3331)
  - [3.33.0](#3330)
  - [3.32.0](#3320)
  - [3.31.0](#3310)
  - [3.30.0](#3300)
  - [3.29.0](#3290)
  - [3.28.0](#3280)
  - [3.27.0](#3270)
  - [3.26.0](#3260)
  - [3.25.0](#3250)
  - [3.24.0](#3240)
  - [3.23.0](#3230)
  - [3.22.0](#3220)
  - [3.21.1](#3211)
  - [3.21.0](#3210)
  - [3.20.0](#3200)
  - [3.19.0](#3190)
  - [3.18.0](#3180)
  - [3.17.0](#3170)
  - [3.16.0](#3160)
  - [3.15.0](#3150)
  - [3.14.0](#3140)
  - [3.13.0](#3130)
  - [3.12.0](#3120)
  - [3.11.0](#3110)
  - [3.10.0](#3100)
  - [3.9.0](#390)
  - [3.8.0](#380)
  - [3.7.0](#370)
  - [3.6.0](#360)
  - [3.5.0](#350)
  - [3.4.0](#340)
  - [3.3.0](#330)
  - [3.2.0](#320)
  - [3.1.0](#310)
  - [3.0.12](#3012)
  - [3.0.11](#3011)
  - [3.0.10](#3010)
  - [3.0.9](#309)
  - [3.0.8](#308)
  - [3.0.7](#307)
  - [3.0.6](#306)

## 3.45.0
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
### Added
- Support new runtime JBOSS 7.2 for Linux Web App
- Support Gradle projects for Web App and Spring Cloud
- Support file deploy for Web App

### Changed
- New creation wizard for Web App with basic and advanced mode

### Fixed
- [#2975](https://github.com/microsoft/azure-tools-for-java/issues/2975),[#4600](https://github.com/microsoft/azure-tools-for-java/issues/4600),[#4605](https://github.com/microsoft/azure-tools-for-java/issues/4605),[#4544](https://github.com/microsoft/azure-tools-for-java/issues/4544) Enhance error handling for network issues
- [#4545](https://github.com/microsoft/azure-tools-for-java/issues/4545),[#4566](https://github.com/microsoft/azure-tools-for-java/issues/4566) Unhandled ProcessCanceledException while start up
- [#4530](https://github.com/microsoft/azure-tools-for-java/issues/4530) Unhandled exception in whats new document
- [#4591](https://github.com/microsoft/azure-tools-for-java/issues/4591),[#4599](https://github.com/microsoft/azure-tools-for-java/issues/4599) Fix Spring Cloud deployment error handling
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

## 3.39.0

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

### Added
- Support create application insights connection while creating new function app

### Changed
- Deprecate Docker Host(will be removed in v3.39.0)

### Fixed
- [#4423](https://github.com/microsoft/azure-tools-for-java/issues/4423) Spark local run mockfs issue with Hive support enabled
- [#4410](https://github.com/microsoft/azure-tools-for-java/issues/4410) the context menu `Submit Spark Application` action regression issue at IDEA 2020.1
- [#4419](https://github.com/microsoft/azure-tools-for-java/issues/4419) the run configuration Spark config table changes didn't take effects regression
- [#4413](https://github.com/microsoft/azure-tools-for-java/issues/4413) the regression issue of Spark local console with Scala plugin 2020.1.36 
- [#4422](https://github.com/microsoft/azure-tools-for-java/issues/4422) Fixes `ConcurrentModificationException` while refreshing spring cloud clusters
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

## 3.33.1

### Fixed
- [#4061](https://github.com/microsoft/azure-tools-for-java/issues/4061) The error of Spark job remote debugging
- [#4079](https://github.com/microsoft/azure-tools-for-java/issues/4079) The regression of Spark console can not start

## 3.33.0

### Added
 - Support upload artifact to ADLS Gen1 storage for Spark on Cosmos Spark Pool
 - Detect authentication type automatically when user types cluster name and lost focus when link an HDInsight cluster
 - Fetch more Livy logs when submit Spark job to HDInsight cluster failed
 - Add background task indicator to improve user experience
 - Support virtual file system on ADLS Gen2 storage for HDInsight Spark cluster and Synapse Spark pool

### Changed
 - Seperator for multiple referenced jars and referenced files is changed from semicolon to space in Spark batch job configuration
 - "Continue Anyway" is changed to "Cancel submit" in "Change configuration settings" dialog when validation check failed for spark batch job
 - The behavior of "Rerun" button action for Spark batch job is changed from re-run with current selected configuration to re-run with previous job configuration

### Fixed
 - [#3935](https://github.com/microsoft/azure-tools-for-java/pull/3935) Clean up HDInsight clusters from cache when user signs out
 - [#3887](https://github.com/microsoft/azure-tools-for-java/issues/3887), [#4023](https://github.com/microsoft/azure-tools-for-java/pull/4023) Fix uncaught StackOverflowError reported by user
 - [#4045](https://github.com/microsoft/azure-tools-for-java/issues/4045) Fix uncaught NullPointerException reported by user

## 3.32.0

### Added

- Support Synapse default ADLS Gen2 storage uploading artifacts
- Support Synapse default ADLS Gen2 storage explorer for reference files/jars
- Synapse Spark batch job detail page link after submission
- Support HIB cluster default ADLS Gen2 storage explorer for reference files/jars
- Support Spark Job remote debugging for HIB cluster
- Support Authentication type detection when linking HIB cluster

### Changed

- Mute warning messages when refreshing HDInsight cluster node in Azure explorer

### Fixed

- [#3899](https://github.com/microsoft/azure-tools-for-java/issues/3899) ADLS Gen2 Virtual File System explorer special characters in path issue
- Linked HDInsight cluster persistent issue
- [#3802](https://github.com/microsoft/azure-tools-for-java/issues/3802) HIB linked cluster logout issue
- [#3887](https://github.com/microsoft/azure-tools-for-java/issues/3887) Stack Overflow issue of SparkBatchJobDebugExecutor

## 3.31.0

### Added
- Support for IntelliJ 2019.3
- Support link an HDInsight HIB cluster for no ARM permission users(Supported by smiles-a-lot girl Yi Zhou [@lcadzy](https://github.com/lcadzy))

### Changed
- List only Synapse workspaces rather than mixed Arcadia and Synapse workspaces
- Remove Storage Accounts explorer

### Fixed
- [#3831](https://github.com/microsoft/azure-tools-for-java/issues/3831) Fix ClassCastException when link an SQL Server big data cluster
- [#3806](https://github.com/microsoft/azure-tools-for-java/issues/3806) Fix showing two 'scala>' when run Spark local console issue
- [#3864](https://github.com/microsoft/azure-tools-for-java/issues/3864), [#3869](https://github.com/microsoft/azure-tools-for-java/issues/3869) Fix scala plugin version breaking change
- [#3823](https://github.com/microsoft/azure-tools-for-java/issues/3823) Fix uncaught StackOverflowError when calling SparkBatchJobDebugExecutor.equals() issue

## 3.30.0

### Added
- Add shorcut ctrl+shift+alt+F2 for disconnect spark application action
- Integrate with HDInsight Identity Broker (HIB) for HDInsight ESP cluster MFA Authentication, cluster navigation, job submission, and interactive query.

### Changed
- Rename brand name from Arcadia to Synapse
- Deprecate Storage Accounts(will be removed in v3.31.0)
- Upload path changes to abfs scheme for default ADLS GEN2 storage type

### Fixed
- [#2891](https://github.com/microsoft/azure-tools-for-java/issues/2891) Hidden Toolkit directory in user home
- [#3765](https://github.com/microsoft/azure-tools-for-java/issues/3765) Fix upload path shows null for spark serverless
- [#3676](https://github.com/microsoft/azure-tools-for-java/issues/3676),[#3728](https://github.com/microsoft/azure-tools-for-java/issues/3728) Fix job view panel show failure
- [#3700](https://github.com/microsoft/azure-tools-for-java/issues/3700),[#3710](https://github.com/microsoft/azure-tools-for-java/issues/3710) Fix Spark configuration name shorten issue in 193EAP
- Fix Spark job submission dialog accessibility issues of Eclipse plugin

## 3.29.0

### Added
- Support IntelliJ 2019.3 EAP
- Add support for Windows Java SE web apps

### Fixed
- Improving the accessibility of IntelliJ plugin

## 3.28.0

### Changed
- HDInsight emulator function is removed
- Upgrade Azure SDK dependencies to most new versions

### Fixed
- [#3534](https://github.com/microsoft/azure-tools-for-java/issues/3534) Fix errors when starting Spark interactive console
- [#3552](https://github.com/microsoft/azure-tools-for-java/issues/3552) Fix Spark remote debugging regresion
- [#3641](https://github.com/microsoft/azure-tools-for-java/issues/3641) Fix NPE error in customer survey dialog
- [#3642](https://github.com/microsoft/azure-tools-for-java/issues/3642) Fix Not Found error when HDInsight refreshing
- [#3643](https://github.com/microsoft/azure-tools-for-java/issues/3643) Fix errors when create service principals

## 3.27.0

### Fixed
- [#3316](https://github.com/microsoft/azure-tools-for-java/issues/3316), [#3322](https://github.com/microsoft/azure-tools-for-java/issues/3322), [#3334](https://github.com/microsoft/azure-tools-for-java/issues/3334), [#3337](https://github.com/microsoft/azure-tools-for-java/issues/3337), [#3339](https://github.com/microsoft/azure-tools-for-java/issues/3339), [#3346](https://github.com/microsoft/azure-tools-for-java/issues/3346), [#3385](https://github.com/microsoft/azure-tools-for-java/issues/3385), [#3387](https://github.com/microsoft/azure-tools-for-java/issues/3387) Fix Accessibility issues

## 3.26.0

### Added
- Support spark 2.4 template projects
- Introduce Spark console view message bars

### Changed
- Refine important message show in the error report 
- Provide Spark Submission panel minimum size to help form building

### Fixed
- [#3308](https://github.com/microsoft/azure-tools-for-java/issues/3308) Fix Scala plugin 2019.2.15 regression
- [#3440](https://github.com/microsoft/azure-tools-for-java/issues/3440) Fix can't open Yarn UI for Aris cluster issue
- [#2414](https://github.com/microsoft/azure-tools-for-java/issues/2414) Fix NPE error when open multi IntelliJ window and sign in/out.
- [#3058](https://github.com/microsoft/azure-tools-for-java/issues/3058) Remove duplicated error notification when auth with no subscription account
- [#3454](https://github.com/microsoft/azure-tools-for-java/issues/3454) Fix ArrayIndexOutOfBoundsException when pop up customer survey window

## 3.25.0

### Added

- Support IntelliJ 2019.2

### Changed

- Move customer survey to qualtrics and refactor survey ui.

### Fixed

- [#3297](https://github.com/microsoft/azure-tools-for-java/issues/3297) Fix NPE error when submit job to Spark on cosmos cluster 

## 3.24.0

### Added

- Support EAP 2019.2
- Support parameter file for Azure Resource Manager
- Integrate intelliJ virtual file system with ADLS Gen2 storage on reference text field in HDI configuration
- Show Yarn log for jobs submitted to Spark on SQL Server cluster

### Changed

- Change app service deploy method to war/zip deploy.
- Given more cluster detail when refreshing Azure explorer encounters exceptions on report dialog
- Better format JSON text of Spark serverless job detail

### Fixed
- [#3230](https://github.com/microsoft/azure-tools-for-java/issues/3230),[#3159](https://github.com/microsoft/azure-tools-for-java/issues/3159) Fix related issues for upload path refresh is not ready scenario
- [#3223](https://github.com/microsoft/azure-tools-for-java/issues/3223),[#3256](https://github.com/microsoft/azure-tools-for-java/issues/3256) Fix main class and cluster info missing on Aris configuration after reopen
- [#3190](https://github.com/microsoft/azure-tools-for-java/issues/3190),[#3234](https://github.com/microsoft/azure-tools-for-java/issues/3234) Fix Spark on Cosmos node disappear after sign in account of dogfood environment
- [#3198](https://github.com/microsoft/azure-tools-for-java/issues/3198) Fix misclassified service exception

## 3.23.0

### Added

- Support Azure Resource Manager, you can deploy and manage azure resource template with toolkit
- Support choosing remote reference jars through folder browser button for HDI cluster with ADLS Gen2 account

### Changed

- Optimize refreshing HDInsight clusters performance
- Handle access related exceptions for linked reader role cluster

### Fixed
- [#3104](https://github.com/microsoft/azure-tools-for-java/issues/3104) Fix linked role reader cluster issue
- [#2895](https://github.com/microsoft/azure-tools-for-java/issues/2895) Fix unnecessarily killing finalizing or ended state job for serverless job

## 3.22.0

### Added

- Automaticly fill in Azure Blob account name or ADLS Gen1/Gen2 root path for linked HDInsight Reader role cluster in run configuration dialog

### Changed

- Improve app service data loading performance
- Restrict upload storage type to cluster default storage type and spark interactive session storage type for linked HDInsight Reader role cluster

### Fixed
- [#3094](https://github.com/microsoft/azure-tools-for-java/issues/3094), [#3096](https://github.com/microsoft/azure-tools-for-java/issues/3096) Fix warning message spelling issue


## 3.21.1

### Fixed

- Fix telemetry shares same installation id

## 3.21.0

### Added

- Support Java 11 App Service
- Add failure task debug feature for HDInsight cluster with Spark 2.3.2
- Support linking cluster with ADLS GEN2 storage account
- Add default storage type for cluster with ADLS GEN2 account

### Changed

- **Breaking change**: Users with cluster ‘**Reader**’ only role can no longer submit job to the HDInsight cluster nor access to the cluster storage. Please request the cluster owner or user access administrator to upgrade your role to **HDInsight Cluster Operator** or **Contributor** in the [Azure Portal](https://ms.portal.azure.com). Click [here](https://docs.microsoft.com/en-us/azure/role-based-access-control/built-in-roles#contributor) for more information. 
- AadProvider.json file is no longer needed for Spark on Cosmos Serverless feature

### Fixed

- [#2866](https://github.com/Microsoft/azure-tools-for-java/issues/2866) Fix uncaught exception when remote debug in HDI 4.0
- [#2958](https://github.com/Microsoft/azure-tools-for-java/issues/2958) Fix deleted cluster re-appeared issue for Spark on Cosmos cluster
- [#2988](https://github.com/Microsoft/azure-tools-for-java/issues/2988) Fix toolkit installation failure with version incompatibility issue
- [#2977](https://github.com/Microsoft/azure-tools-for-java/issues/2977) Fix "Report to Microsoft" button been disabled issue

## 3.20.0

### Added

- Support Failure Task Local Reproduce for Spark 2.3 on Cosmos
- Support mock file system in Spark local console
- Support ADLS Gen2 storage type to submit job to HDInsight cluster
- Introduce extended properties field when provision a Spark on Cosmos cluster or submit a Spark on Cosmos Serverless job

### Changed

- Use device login as the default login method.
- Change icons for HDInsight cluster and related configuration

### Fixed

- [#2805](https://github.com/Microsoft/azure-tools-for-java/issues/2805) Save password with SecureStore.
- [#2888](https://github.com/Microsoft/azure-tools-for-java/issues/2888), [#2894](https://github.com/Microsoft/azure-tools-for-java/issues/2894), [#2921](https://github.com/Microsoft/azure-tools-for-java/issues/2921) Fix Spark on Cosmos Serverless job run failed related issues
- [#2912](https://github.com/Microsoft/azure-tools-for-java/issues/2912) Check invalid access key for submitting with ADLS Gen2 account
- [#2844](https://github.com/Microsoft/azure-tools-for-java/issues/2844) Refine WebHDFS and ADLS input path hints
- [#2848](https://github.com/Microsoft/azure-tools-for-java/issues/2848) Reset background color for not empty ADLS path input
- [#2749](https://github.com/Microsoft/azure-tools-for-java/issues/2749), [#2936](https://github.com/Microsoft/azure-tools-for-java/issues/2936) Fix Spark run configuration cast issues and classified exception message factory NPE issues

## 3.19.0

### Added

- Support open browser after Web App deployment.
- Support to link SQL Server Big Data cluster and submit Spark jobs.
- Support WebHDFS storage type to submit job to HDInsight cluster with ADLS Gen 1 storage account.

### Changed

- Update UI of Web App creation and deployment
- Subscription ID need to be specified for ADLS Gen 1 storage type

### Fixed

- [#2840](https://github.com/Microsoft/azure-tools-for-java/issues/2840) Submit successfully with invalid path for WebHDFS storage type issue.
- [#2747](https://github.com/Microsoft/azure-tools-for-java/issues/2747),[#2801](https://github.com/Microsoft/azure-tools-for-java/issues/2801) Error loadig HDInsight node issue.
- [#2714](https://github.com/Microsoft/azure-tools-for-java/issues/2714),[#2688](https://github.com/Microsoft/azure-tools-for-java/issues/2688),[#2669](https://github.com/Microsoft/azure-tools-for-java/issues/2669),[#2728](https://github.com/Microsoft/azure-tools-for-java/issues/2728),[#2807](https://github.com/Microsoft/azure-tools-for-java/issues/2807),[#2808](https://github.com/Microsoft/azure-tools-for-java/issues/2808),[#2811](https://github.com/Microsoft/azure-tools-for-java/issues/2811),[#2831](https://github.com/Microsoft/azure-tools-for-java/issues/2831)Spark Run Configuration validation issues.
- [#2810](https://github.com/Microsoft/azure-tools-for-java/issues/2810),[#2760](https://github.com/Microsoft/azure-tools-for-java/issues/2760) Spark Run Configuration issues when created from context menu.

## 3.18.0

### Added

- Supports Cosmos Serverless Spark submission and jobs list.
- Accepts SSL certificates automatically if the bypass option is enabled.

### Changed

- Wording of HDInsight and Spark UX.
- Enhanced Spark Run Configuration validation.

### Fixed

- [#2368](https://github.com/Microsoft/azure-tools-for-java/issues/2368) Device login will write useless error log.
- [#2675](https://github.com/Microsoft/azure-tools-for-java/issues/2675) Error message pops up when refresh HDInsight.

## 3.17.0

### Added

- The menu option for default Spark type to create Run Configuration.
- The menu option for bypassing SSL certificate validation for Spark Cluster.
- The progress bar for Spark cluster refreshing.
- The progress bar for Spark interactive consoles.

### Changed

- SQL Big Data Cluster node of Azure Explorer is changed into a first level root node.
- Link a SQL Big Data Cluster UI is aligned with Azure Data Studio UX.
- Spark for ADL job submission pops up Spark master UI page at the end.

### Fixed

- [#2307](https://github.com/Microsoft/azure-tools-for-java/issues/2307) Spark Run Configuration storage info for artifacts deployment issues
- [#2267](https://github.com/Microsoft/azure-tools-for-java/issues/2267) Spark Run Configuration remote run/debug actions overwrite non-spark codes Line Mark actions issue
- [#2500](https://github.com/Microsoft/azure-tools-for-java/issues/2500),[#2492](https://github.com/Microsoft/azure-tools-for-java/issues/2492),[#2451](https://github.com/Microsoft/azure-tools-for-java/issues/2451),[#2254](https://github.com/Microsoft/azure-tools-for-java/issues/2254) SQL Big Data Cluster link issues
- [#2485](https://github.com/Microsoft/azure-tools-for-java/issues/2485),[#2484](https://github.com/Microsoft/azure-tools-for-java/issues/2484),[#2483](https://github.com/Microsoft/azure-tools-for-java/issues/2483),[#2481](https://github.com/Microsoft/azure-tools-for-java/issues/2481),[#2427](https://github.com/Microsoft/azure-tools-for-java/issues/2427),[#2423](https://github.com/Microsoft/azure-tools-for-java/issues/2423),[#2417](https://github.com/Microsoft/azure-tools-for-java/issues/2417),[#2462](https://github.com/Microsoft/azure-tools-for-java/issues/2462) Spark Run Configuration validation issues
- [#2418](https://github.com/Microsoft/azure-tools-for-java/issues/2418) Spark for ADL provision UX issues
- [#2392](https://github.com/Microsoft/azure-tools-for-java/issues/2392) Azure Explorer HDInsight Spark cluster refreshing errors
- [#2488](https://github.com/Microsoft/azure-tools-for-java/issues/2488) Spark remote debugging SSH password saving regression

## 3.16.0

### Added

- Support both dedicated Azure explorer node and run configuration for Aris linked clusters.
- Support Spark local run classpath modules selection.

### Changed

- Use P1V2 as the default pricing tier for App Service.
- Spark run configuration validate checking is moved from before saving to before running.

### Fixed

- [#2468](https://github.com/Microsoft/azure-tools-for-java/issues/2468) Spark Livy interactive console regression of IDEA183 win process
- [#2424](https://github.com/Microsoft/azure-tools-for-java/issues/2424) Spark Livy interactive console blocking UI issue
- [#2318](https://github.com/Microsoft/azure-tools-for-java/issues/2318), [#2283](https://github.com/Microsoft/azure-tools-for-java/issues/2283) Cosmos Spark provision dialog AU warning issue
- [#2420](https://github.com/Microsoft/azure-tools-for-java/issues/2420) Spark cluster name duplicated issue in the run configuration
- [#2478](https://github.com/Microsoft/azure-tools-for-java/pull/2478) Cosmos Spark submit action can't find the right run configuration issue
- [#2419](https://github.com/Microsoft/azure-tools-for-java/issues/2419) The user can submit Spark job to unstable Cosmos Spark cluster issue
- [#2484](https://github.com/Microsoft/azure-tools-for-java/issues/2484), [#2316](https://github.com/Microsoft/azure-tools-for-java/issues/2316) The uploading storage config issues of Spark run configuration*
- [#2341](https://github.com/Microsoft/azure-tools-for-java/issues/2341) Authentication regression of `InvalidAuthenticationTokenAudience`

## 3.15.0

### Added

- Support new runtime WildFly 14 for Web App on Linux.
- Support to connect Spark Cosmos resource pool with Spark Interactive Console.
- Support to deploy Spark Application JAR artifacts by WebHDFS service (only support Basic authentication method).

### Fixed

- [#2381](https://github.com/Microsoft/azure-tools-for-java/issues/2381) Spark local interactive console jline dependence auto-fix dialog always popped up issue.
- [#2326](https://github.com/Microsoft/azure-tools-for-java/issues/2326) The Spark Run Configuration dialog always popped up issue for correct config.
- [#2116](https://github.com/Microsoft/azure-tools-for-java/issues/2116) [#2345](https://github.com/Microsoft/azure-tools-for-java/issues/2345) [#2339](https://github.com/Microsoft/azure-tools-for-java/issues/2339) User feedback issues.

## 3.14.0

### Added

- Support to show application settings of Deployment Slot.
- Support to delete a Deployment Slot in Azure Explorer.
- Support to config ADLS Gen1 Storage settings for Spark Run Configuration (only for HDInsight ADLS Gen 1 clusters and the interactive sign in mode).
- Support to auto fix Spark local REPL console related dependency.
- Support to classify Spark remotely application running error and provide more clear error messages.
- Support to start a Spark local console without a run configuration.

### Changed

- Change the Deployment Slot area in "Run on Web App" to be hideable.
- Use Azul Zulu JDK in Dockerfile of Web App for Containers.
- Spark linked cluster storage blob access key is saved to the secure store.

### Fixed

- [#2215](https://github.com/Microsoft/azure-tools-for-java/issues/2215) The prompt warning message on deleting web app is not correct issue.
- [#2310](https://github.com/Microsoft/azure-tools-for-java/issues/2310) Discarding of changes on Web App application settings is too slow issue.
- [#2286](https://github.com/Microsoft/azure-tools-for-java/issues/2286) [#2285](https://github.com/Microsoft/azure-tools-for-java/issues/2285) [#2120](https://github.com/Microsoft/azure-tools-for-java/issues/2120) [#2119](https://github.com/Microsoft/azure-tools-for-java/issues/2119) [#2117](https://github.com/Microsoft/azure-tools-for-java/issues/2117) Spark Console related issues.
- [#2203](https://github.com/Microsoft/azure-tools-for-java/issues/2203) Spark Remote Debug SSH password wasn't saved issue.
- [#2288](https://github.com/Microsoft/azure-tools-for-java/issues/2288) [#2287](https://github.com/Microsoft/azure-tools-for-java/issues/2287) HDInsight related icons size issue.
- [#2296](https://github.com/Microsoft/azure-tools-for-java/issues/2296) UI hang issue caused by Spark storage information validation.
- [#2295](https://github.com/Microsoft/azure-tools-for-java/issues/2295) [#2314](https://github.com/Microsoft/azure-tools-for-java/issues/2314) Spark Resource Pool issues.
- [#2303](https://github.com/Microsoft/azure-tools-for-java/issues/2303) [#2272](https://github.com/Microsoft/azure-tools-for-java/issues/2272) [#2200](https://github.com/Microsoft/azure-tools-for-java/issues/2200) [#2198](https://github.com/Microsoft/azure-tools-for-java/issues/2198) [#2161](https://github.com/Microsoft/azure-tools-for-java/issues/2161) [#2151](https://github.com/Microsoft/azure-tools-for-java/issues/2151) [#2109](https://github.com/Microsoft/azure-tools-for-java/issues/2109) [#2087](https://github.com/Microsoft/azure-tools-for-java/issues/2087) [#2058](https://github.com/Microsoft/azure-tools-for-java/issues/2058) Spark Job submission issues.
- [#2158](https://github.com/Microsoft/azure-tools-for-java/issues/2158) [#2085](https://github.com/Microsoft/azure-tools-for-java/issues/2085) HDInsight 4.0 regression issues.

## 3.13.0

### Added

- Support to deploy an application to Deployment Slot.
- Support to show and operate Deployment Slots of a Web App in Azure Explorer.
- Support to link an independent Livy server for Spark cluster.
- Add Spark Local interactive console.
- Add Spark HDInsight cluster interactive console (Only for 2018.2, Scala plugin is needed).

### Changed

- Change the Spark Job context menu submission dialog, to unify with IntelliJ Run Configuration Setting dialog.
- Move the storage information of HDInsight/Livy cluster to linked into Run Configuration settings.

### Fixed

- [#2143](https://github.com/Microsoft/azure-tools-for-java/issues/2143) The element "filter-mapping" is not removed when disabling telemetry with Application Insights.

## 3.12.0

### Added

- Support to deploy applications to Web App (Linux).
- Support to show the Azure Data Lake Spark resource pool provision log outputs.

### Changed

- List Web Apps on both Windows and Linux in Azure Explorer.
- List all app service plans of the selected subscription when creating a new Web App.
- Always upload the web.config file together with the .jar artifact when deploying to Web App (Windows).

### Fixed

- [#1968](https://github.com/Microsoft/azure-tools-for-java/issues/1968) Runtime information is not clear enough for Azure Web Apps
- [#1779](https://github.com/Microsoft/azure-tools-for-java/issues/1779) [#1920](https://github.com/Microsoft/azure-tools-for-java/issues/1920) The issue of Azure Data Lake Spark resource pool `Update` dialog pop up multi times.

## 3.11.0

- Added the main class hint when users choose to submit a Spark job using a local artifact file.
- Added Spark cluster GUID for Spark cluster provision failure investigation.
- Added the "AU not enough" warning message in Azure Data Lake Spark resource pool provision.
- Added the job queue query to check AU consumption in Azure Data Lake Spark resource pool provision.
- Fixed cluster total AU by using systemMaxAU instead of maxAU.
- Refresh node automatically when node is clicked in Azure explorer.
- Updated the Azure SDK to 1.14.0.
- Fixed some bugs.

## 3.10.0

- Supported to fix Spark job configuration in run configuration before Spark job submission.
- Updated Application Insights library to v2.1.2.
- Fixed some bugs.

## 3.9.0

- Added Spark 2.3 support.
- Spark in Azure Data Lake private preview refresh and bug fix.
- Fixed some bugs.

## 3.8.0

- Supported to run Spark jobs in Azure Data Lake cluster (in private preview).
- Fixed some bugs.

## 3.7.0

- Users do not need to login again in interactive login mode, if Azure refresh token is still validated.
- Updated ApplicationInsights version to v2.1.0.
- Fixed some bugs.

## 3.6.0

- Updated ApplicationInsights version to v2.0.2.
- Added Spark 2.2 templates for HDInsight.
- Added SSH password expiration check.
- Fixed some bugs.

## 3.5.0

- Added open Azure Storage Explorer for exploring data in HDInsight cluster (blob or ADLS).
- Improved Spark remote debugging.
- Improved Spark job submission correctness check.
- Fixed an login issue.

## 3.4.0

- Users can use Ambari username/password to submit Spark job to HDInsight cluster, in additional to Azure subscription based authentication. This means users without Azure subscription permission can still use Ambari credentials to submit/debug their Spark jobs in HDInsight clusters.
- The dependency on storage permission is removed and users do not need to provide storage credentials for Spark job submission any more (storage credential is still needed if users want to use storage explorer).

## 3.3.0

- Added support of Enterprise Security Package HDInsight Spark cluster.
- Support submitting Spark jobs using Ambari username/password instead of the Azure subscription credential.
- Updated ApplicationInsights version to v1.0.10.
- Fixed some bugs.

## 3.2.0

- Fixed Spark job submission issue when user right click Spark project and submit Spark job in project explorer.
- Fixed HDInsight wasbs access bug when SSL encrypted access is used.
- Added JxBrowser support for new Spark job UI.
- Fixed winutils.exe not setup issue and updated error message.

## 3.1.0

- Fixed compatibility issue with IntelliJ IDEA 2017.3.
- HDInsight tools UI refactoring: Added toolbar entry and right click context menu entry for Spark job submission and local/in-cluster debugging, which make users submit or debug job easier.
- Fixed some bugs.

## 3.0.12

- Support submitting the script to HDInsight cluster without modification in Spark local run.
- Fixed some bugs.

## 3.0.11

- Support view/edit properties of Azure Web App (Windows/Linux).
- Support interactive login mode for Azure China.
- Support running docker locally for multiple modules in current project (simultaneously).
- Users can now use the same code for both Spark local run and cluster run, which means they can test locally and then submit to cluster without modification.
- HDInsight tools for IntelliJ now generate run/debug configuration automatically to make Spark job run/debug easier for both local and cluster run.
- Fixed some bugs.

## 3.0.10

- Support pushing docker image of the project to Azure Container Registry.
- Support navigating Azure Container Registry in Azure Explorer.
- Support pulling image from Azure Container Registry in Azure Explorer.
- Fixed some bugs.

## 3.0.9

- Fixed "Unexpected token" error when using Run on Web App (Linux). ([#1014](https://github.com/Microsoft/azure-tools-for-java/issues/1014))

## 3.0.8

- Support Spring Boot Project: The Azure Toolkits for IntelliJ now support running your Spring Boot Project (Jar package) on Azure Web App and Azure Web App (Linux).
- Docker Run Locally: You can now docker run your projects locally after adding docker support.
- New Node in Azure Explorer: You can now view the property of your resources in Azure Container Registry.
- Added validation for Spark remote debug SSH authentication.
- Fixed some bugs.

## 3.0.7

- Support Community Edition: The Azure Toolkit for IntelliJ now supports deploying your Maven projects to Azure App Service from IntelliJ IDEA, both Community and Ultimate Edition.
- Improved Web App Workflow: You can now run your web applications on Azure Web App with One-Click experience using Azure Toolkit for IntelliJ.
- New Container Workflow: You can now dockerize and run your web application on Azure Web App (Linux) via Azure Container Registry.
- Spark remote debugging in IntelliJ now support debugging of both driver and executor code depending on where the breakpoint is set.
- Fixed some bugs.

## 3.0.6

- Added the Redis Cache Explorer that allows users to scan/get keys and their values.
- Improved Spark job remote debugging support(show log in console, apply and load debugging config).
- Fixed some bugs.
