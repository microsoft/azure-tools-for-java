# Change Log

All notable changes to "Azure Toolkit for IntelliJ IDEA" will be documented in this file.

- [Change Log](#change-log)
  - [3.83.0](#3830)
  - [3.82.0](#3820)
  - [3.81.0](#3810)
  - [3.80.0](#3800)
  - [3.79.1](#3791)
  - [3.79.0](#3790)
  - [3.78.1](#3781)
  - [3.78.0](#3780)
  - [3.77.0](#3770)
  - [3.76.0](#3760)
  - [3.75.0](#3750)
  - [3.74.0](#3740)
  - [3.73.0](#3730)
  - [3.72.0](#3720)
  - [3.71.0](#3710)
  - [3.70.0](#3700)
  - [3.69.0](#3690)
  - [3.68.1](#3681)
  - [3.68.0](#3680)
  - [3.67.0](#3670)
  - [3.66.0](#3660)
  - [3.65.0](#3650)
  - [3.64.0](#3640)
  - [3.63.0](#3630)
  - [3.62.0](#3620)
  - [3.61.1](#3611)
  - [3.61.0](#3610)
  - [3.60.2](#3602)
  - [3.60.1](#3601)
  - [3.60.0](#3600)
  - [3.59.0](#3590)
  - [3.58.0](#3580)
  - [3.57.1](#3571)
  - [3.57.0](#3570)
  - [3.56.0](#3560)
  - [3.55.0](#3550)
  - [3.54.0](#3540)
  - [3.53.0](#3530)
  - [3.52.0](#3520)
  - [3.51.0](#3510)
  - [3.50.0](#3500)
  - [3.49.0](#3490)
  - [3.48.0](#3480)
  - [3.47.0](#3470)
  - [3.46.0](#3460)
  - [3.45.1](#3451)
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

## 3.83.0
### Added
- Add Azure Key Vault support in Azure Toolkits
  * Resource Management features in Azure explorer
    - Create new secret/certificate/key in toolkts
    - View/Download secret/certificate/key (need Azure CLI installed)
  * Code assistance of Key Vault for Spring project

### Fixed
- Get the error of "AzureToolkitRuntimeException" when opening project with resource connection configuration file
- Get duplicate property key when connect resource from .properties
- Generate deprecated configurations with code completions in spring properties
- Build task will be removed for project with resource connection

## 3.82.0
### Added
- Code assistance of Azure resources for Spring and Azure Functions.
- Azure Functions flex consumption tier support.

### Fixed
- [#7907](https://github.com/microsoft/azure-tools-for-java/issues/7907): Uncaught Exception Operator called default onErrorDropped java.lang.InterruptedException.
- other known issues.

## 3.81.1
### Fixed
- [#7897](https://github.com/microsoft/azure-tools-for-java/issues/7897): Uncaught Exception: Error was received while reading the incoming data. The connection will be closed.

## 3.81.0
### Added
- Bring all new feature to IntelliJ IDEA 2021.3

### Changed
- Upgrade Azure SDK to the latest.
- More UI actions are tracked (Telemetry).
- Resource Connections Explorer is deprecated.
- Some minor UI updates.

### Fixed
- NPE when creating storage account if region is loading.
- other known issues.

## 3.80.0
### Added
- Azure OpenAI (chat) playground for GTP* models.
- Guidance (Getting started course) to try Azure OpenAI and its playground (chat) in IntelliJ IDEA.    
- Azure OpenAI resource management.

### Changed
- some useful subsequent actions are added onto the resource creation/update success notifications.
- newly added resources connections/deployments will be automatically highlighted in Project Explorer.

### Fixed
- Fix: reset/save doesn't show/enable when removing existing values of jvm options and env var in spring app properties editor.
- Fix: the default runtime version of new spring apps doesn't match the version of current project/selected module.

## 3.79.1
### Fixed
- Fix: Code navigation was not working for bicep files.
- Fix: Textmate rendering was not functioning for bicep files in IntelliJ 2023.2.

## 3.79.0
### Added
- Support for creating Azure Spring apps/services of Enterprise/Standard/Basic tier in IDE.    
- Support for managing deployment target services directly in Project Explorer.    

### Fixed
- status shows inactive after creating/refreshing spring app.
- error may occur when importing document into SQL container.
- error may occur when connecting to the storage emulator and running locally.
- error may occur when deploy function app.
- HDInsight Job view nodes are displayed as 'folder icon + cluster name'.
- HDInsight Linked cluster cannot display in Azure Explorer when not signed in.

## 3.78.1
### Fixed
- Fix: error pops when starting/stopping/restarting spring app.
- Fix: error pops when deleting cosmos db document.
- Fix: updating firewall rules in sql database's properties editor view doesn't work.

## 3.78.0
### Added
- New UX for Azure resource connections in IntelliJ project view
  - Support list/add/remove Azure resource connections in project explorer
  - Support edit environment variables for Azure resource connections
  - Support manage connected Azure resources in project explorer
- Support IntelliJ 2023.2 EAP

### Fixed
- Fix: System environment variables may be missed during function run/deployment
- [#7651](https://github.com/microsoft/azure-tools-for-java/issues/7651): Uncaught Exception DeployFunctionAppAction#update, check if project is a valid function project.
- [#7653](https://github.com/microsoft/azure-tools-for-java/issues/7653): Uncaught Exception com.intellij.diagnostic.PluginException: No display name is specified for configurable com.microsoft.intellij.AzureConfigurable in xml file.
- [#7619](https://github.com/microsoft/azure-tools-for-java/issues/7619): Uncaught Exception Uncaught Exception java.lang.IllegalArgumentException: invalid arguments id/nameId.

## 3.77.0
### Added
- Azure Spring Apps: basic Standard Consumption plan(preview) support.
- Azure Storage Account: local Storage Account Emulator (Azurite) support.

### Changed
- Azure Spring Apps: performance of creating/updating resources is improved.
- Azure Functions: users are asked to select an Cloud/Emulated Storage Account in case of missing `AzureWebJobsStorage` at local run instead of fail directly.
- Resource Connection: data related to resource connections are moved from project dir to module dir and the schema is also changed.

### Fixed
- [#7564](https://github.com/microsoft/azure-tools-for-java/issues/7564): Uncaught Exception java.lang.NullPointerException: Cannot invoke "com.microsoft.azure.toolkit.ide.common.store.IIdeStore.getProperty(String, String)" because "store" is null.
- [#7561](https://github.com/microsoft/azure-tools-for-java/issues/7561): Uncaught Exception com.intellij.diagnostic.PluginException: 644 ms to call on EDT DeployFunctionAppAction#update@MainMenu (com.microsoft.azure.toolkit.intellij.legacy.function.action.DeployFunctionAppAction).
- [#7421](https://github.com/microsoft/azure-tools-for-java/issues/7421): Uncaught Exception com.intellij.diagnostic.PluginException: 303 ms to call on EDT ServerExplorerToolWindowFactory$RefreshAllAction#update@ToolwindowTitle (com.microsoft.intellij.ui.ServerExplorerToolWindowFactory$RefreshAllAction).
- [#7411](https://github.com/microsoft/azure-tools-for-java/issues/7411): Uncaught Exception com.intellij.diagnostic.PluginException: 338 ms to call on EDT RunFunctionAction#update@GoToAction (com.microsoft.azure.toolkit.intellij.legacy.function.action.RunFunctionAction).
- [#7185](https://github.com/microsoft/azure-tools-for-java/issues/7185): Uncaught Exception com.intellij.diagnostic.PluginException: 446 ms to call on EDT AzureSignInAction#update@ToolwindowTitle (com.microsoft.intellij.actions.AzureSignInAction).
- [#7143](https://github.com/microsoft/azure-tools-for-java/issues/7143): Uncaught Exception com.intellij.diagnostic.PluginException: 403 ms to call on EDT ShowGettingStartAction#update@GoToAction (com.microsoft.azure.toolkit.ide.guidance.action.ShowGettingStartAction).
- Fix : Toolkit could not authenticate with Azure CLI when it was run from the dock in Mac OS.
- Fix : Failed to upload Spark application artifacts in IntelliJ 2023.1.
- Fix : Local run and remote run failed, only repro in IntelliJ 2022.3.
- Fix : Show Failed to proceed after clicking on storage account node.
- Fix : Apache Spark on Azure Synapse\Apache Spark on Cosmos\SQL Server Big Data Cluster cannot be listed.
- Fix : Load cluster show errors.

## 3.76.0
### Added
- Basic resource management support for service connections
- New one click action to deploy Dockerfile (build image first) to Azure Container App
- Finer granular resource management(registry/repository/images/...) for Azure Container Registry
- Monitoring support for Azure Container Apps (azure monitor integration & log streaming)

### Changed
- Docker development/container based Azure services experience enhancement
  - UX enhancement for docker host run/deploy experience
  - Migrate docker client to docker java to unblock docker experience in MacOS
- UX enhancement for Azure Monitor
  - Finer time control (hour, minute, seconds...) for montior queries
  - Add customer filters persistence support

### Fixed
- [#7387](https://github.com/microsoft/azure-tools-for-java/issues/7387): Cannot invoke "com.intellij.openapi.editor.Editor.getDocument()" because "editor" is null
- [#7020](https://github.com/microsoft/azure-tools-for-java/issues/7020): Uncaught Exception java.util.ConcurrentModificationException
- [#7444](https://github.com/microsoft/azure-tools-for-java/issues/7444): Uncaught Exception com.microsoft.azure.toolkit.lib.common.operation.OperationException: initialize Azure explorer
- [#7432](https://github.com/microsoft/azure-tools-for-java/issues/7432): Cannot invoke "com.intellij.psi.PsiDirectory.getVirtualFile()" because "dir" is null
- [#7479](https://github.com/microsoft/azure-tools-for-java/issues/7479): Uncaught Exception java.lang.Throwable: Assertion failed

## 3.75.0
### Added
- New course about `Azure Spring Apps` in `Getting Started with Azure` course list.
- Resource Management of `Azure Database for PostgreSQL flexible servers`.
- Add `Azure Service Bus` support in Azure Toolkits.
  - Resource Management in Azure explorer.
  - Simple Service Bus client to send/receive messages.

### Changed
- Warn user if bytecode version of deploying artifact is not compatible of the runtime of target Azure Spring app.
- JDK version of current project is used as the default runtime of creating Spring App.
- Remove HDInsight related node favorite function.

### Fixed
- 'Send Message' action is missing if there is a long text to send
- [#7374](https://github.com/microsoft/azure-tools-for-java/issues/7374): Uncaught Exception com.microsoft.azure.toolkit.lib.common.operation.OperationException: initialize editor highlighter for Bicep files
- Fix : When not sign in to azure, the linked cluster does not display the linked label.
- Fix : Show the error " cannot find subscription with id '[LinkedCluster]' " in the lower right corner, and will display many in notification center.
- Fix : Graphics in job view are obscured.
- Fix : Under the theme of windows 10 light, the background color of debug verification information is inconsistent with the theme color.

## 3.74.0
### Added
- Support IntelliJ 2023.1 EAP.
- Add Azure Event Hub support in Azure Toolkits
  - Resource Management in Azure explorer
  - Simple event hub client to send/receive events

### Changed
- Azure Function: New function class creation workflow with resource connection
- Azure Function: Support customized function host parameters and path for `host.json` in function run/deployment
- App Service: New UX for runtime selection
- Azure Spring Apps: Integrate with control plane logs, more diagnostic info will be shown during deployment

### Fixed
- Fix: Toolkit will always select maven as build tool in function module creation wizard
- Fix: Copy connection string did not work for Cosmos DB
- Fix: Only `local.settings.json` in root module could be found when import app settings
- Fix: Linked cluster cannot display under the HDInsight node.
- Fix: Open the sign into Azure dialog after click on "Link a cluster/refresh" in the context menu.
- Fix: Failed to open Azure Storage Explorer.
- Fix: In config, only display linked cluster in cluster list, but in Azure explorer both linked cluster and signincluster exist.
## 3.73.0
### Added
- [Azure Monitor] Azure Monitor to view history logs with rich filters.    
- [Azure Container Apps] Creation of Azure Container Apps Environment.
- [Azure Explorer] Pagination support in Azure Explorer.

### Changed
- Update default Java runtime to Java 11 when creating Azure Spring App.
- Add setting item to allow users to choose whether to enable authentication cache.    

### Fixed
- [#7272](https://github.com/microsoft/azure-tools-for-java/issues/7272): `corelibs.log` duplicates all the logs from the IDE.
- [#7248](https://github.com/microsoft/azure-tools-for-java/issues/7248): Uncaught Exception java.lang.NullPointerException: Cannot invoke "Object.hashCode()" because "key" is null.
- No error message about failing to create a slot when the app pricing tier is Basic.
- Transport method for container app in properties window is different with in portal.
- Unable to download functional core tools from "Settings/Azure" on macOS when Proxy with certificate is configured.
- Error pops up when deleting App setting in property view of Azure Functions/Web app.
- Can't connect multiple Azure resources to modules using resource connection feature.

## 3.72.0
### Added
- Bicep Language Support (preview).
- Resource Management of Azure Container Apps.
- Resource Management of Azure Database for MySQL flexible server.
- Support for proxy with certificate.

### Changed
- deprecated Resource Management support for Azure Database for MySQL (single server).

### Fixed
- installed Function Core Tools doesn't take effect right now when run/debug functions locally from line gutter.
- Status/icon is wrong for a deleting resource.
- links are not rendered correctly in notifications.

## 3.71.0
### Added
- Code samples of management SDK are now available in Azure SDK Reference Book
- Function Core Tools can be installed and configured automatically inside IDE.
- Data sources can be created by selecting an existing Azure Database for MySQL/PostgreSQL or Azure SQL. (Ultimate Edition only)

### Changed
- Action icons of `Getting Started` would be highlighted for part of those who have never opened it before.
- UI of `Getting Started` courses panel is changed a little bit.

### Fixed
- [#7063](https://github.com/microsoft/azure-tools-for-java/issues/7063): ClassNotFoundException with local deployment of function app that depends on another module in the same project
- [#7089](https://github.com/microsoft/azure-tools-for-java/issues/7089): Uncaught Exception Access is allowed from event dispatch thread only
- [#7116](https://github.com/microsoft/azure-tools-for-java/issues/7116): IntelliJ Azure Function SQL Library is not copied to lib folder when running locally
- editor names of opened CosmosDB documents is not the same as that of the document.
- exception throws if invalid json is provided when signing in in Service Principal mode.
- Setting dialog will open automatically when running a function locally but Azure Function Core tools is not installed.

## 3.70.0
### Added
- Added support for remote debugging of `Azure Spring Apps`.
- Added support for remote debugging of `Azure Function Apps`.
- Added support for data management of `Azure Storage Account` in Azure Explorer.
- Added support for data management of `Azure Cosmos DB account` in Azure Explorer.
- Added support for filtering app settings of `Azure Web App/ Function App` in properties view and run configuration dialog.

### Fixed
- Fix `Open Spark History UI` link no reaction, when there is no job in the cluster. 
- Fix local console and Livy console run failed.
- Fix error getting cluster storage configuration. 
- Fix linked clusters cannot be expanded when not logged in to azure.
- Fix local console get IDE Fatal Error when the project first create.

## 3.69.0
### Added
- Users are able to deploy artifacts to Azure Functions Deployment Slot directly.

### Fixed
- [#6939](https://github.com/microsoft/azure-tools-for-java/issues/6939): Uncaught Exception java.lang.NullPointerException: Cannot invoke "com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager.runOnPooledThread(java.lang.Runnable)" because the return value of "com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager.getInstance()" is null
- [#6930](https://github.com/microsoft/azure-tools-for-java/issues/6930): com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException: you are not signed-in.
- [#6909](https://github.com/microsoft/azure-tools-for-java/issues/6909): Cannot invoke "org.jetbrains.idea.maven.project.MavenProject.getParentId()" because "result" is null
- [#6897](https://github.com/microsoft/azure-tools-for-java/issues/6897): There is a vulnerability in Postgresql JDBC Driver 42.3.1,upgrade recommended
- [#6894](https://github.com/microsoft/azure-tools-for-java/issues/6894): There is a vulnerability in MySQL Connector/J 8.0.25,upgrade recommended
- [#6893](https://github.com/microsoft/azure-tools-for-java/issues/6893): There is a vulnerability in Spring Framework 4.2.5.RELEASE,upgrade recommended
- [#6869](https://github.com/microsoft/azure-tools-for-java/issues/6869): Error was received while reading the incoming data. The connection will be closed. java.lang.IllegalStateException: block()/blockFirst()/blockLast() are blocking, which is not supported in thread reactor-http-nio-3
- [#6846](https://github.com/microsoft/azure-tools-for-java/issues/6846): java.lang.IndexOutOfBoundsException: Index 0 out of bounds for length 0
- [#6687](https://github.com/microsoft/azure-tools-for-java/issues/6687): Uncaught Exception java.lang.NullPointerException
- [#6672](https://github.com/microsoft/azure-tools-for-java/issues/6672): com.microsoft.azure.toolkit.lib.common.operation.OperationException: load Resource group (*)
- [#6670](https://github.com/microsoft/azure-tools-for-java/issues/6670): com.intellij.util.xmlb.XmlSerializationException: Cannot deserialize class com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy.FunctionDeployModel
- [#6605](https://github.com/microsoft/azure-tools-for-java/issues/6605): java.lang.NullPointerException
- [#6380](https://github.com/microsoft/azure-tools-for-java/issues/6380): spuriously adding before launch package command
- [#6271](https://github.com/microsoft/azure-tools-for-java/issues/6271): Argument for @NotNull parameter 'virtualFile' of com/microsoft/azure/toolkit/intellij/common/AzureArtifact.createFromFile must not be null
- [#4726](https://github.com/microsoft/azure-tools-for-java/issues/4726): Confusing workflow of "Get Publish Profile"
- [#4725](https://github.com/microsoft/azure-tools-for-java/issues/4725): Misaligned label in Web App property view
- [#301](https://github.com/microsoft/azure-tools-for-java/issues/301): Should validate username when creating a VM
- [#106](https://github.com/microsoft/azure-tools-for-java/issues/106): azureSettings file in WebApps shouldn't be created by default
- No response when click on Open `Azure Storage Expolrer for storage` while the computer does not install Azure Storage Explorer.
- The shortcut keys for the browser and expansion are the same.
- All the roles of the HDInsight cluster are reader.
- Local console and Livy console run failed.
- Job view page: The two links in the job view page open the related pages very slowly.
- Click on Job node, show IDE error occurred.
- Other bugs.

### Changed
- Remove menu `Submit Apache Spark Application`

## 3.68.1
### Fixed
- Fixed the data modification failure issue of `Azure Cosmos DB API for MongoDB` Data Sources.

### Changed
- Added feature toggle for creating Data Source of `Azure Cosmos DB API for Cassandra`, the toggle is **off** by default.
  - Support for opening `Azure Cosmos DB API for Cassandra` with `Database and SQL tools` plugin from `Azure Explorer` is disabled by default.
  - Support for creating Data Source of the `Azure Cosmos DB API for Cassandra` from `Database and SQL tools` plugin is disabled by default.

## 3.68.0
### Added
- Added support for resource management of `Azure Cosmos DB accounts` in Azure Explorer.
- Added support for resource connection to `Azure Cosmos DB accounts`.
- Added support for creating data source of the Mongo and Cassandra API for `Azure Cosmos DB` from both Azure Explorer and `Database` tool window (`IntelliJ IDEA Ultimate Edition` only).
- Added support for connecting an `Azure Virtual Machine` using SSH directly from an `Azure Virtual Machine` resource node in Azure Explorer.
- Added support for browsing files of an `Azure Virtual Machine` from an `Azure Virtual Machine` resource node in Azure Explorer (`IntelliJ IDEA Ultimate Edition` only).
- Added support for adding dependencies to current local project from `Azure SDK reference book`.
- Added support for jumping to corresponding Azure SDK page in `Azure SDK reference book` from Azure Explorer nodes.
- Added support for configuring environment variables when deploy artifacts to an `Azure Web App`.
- Added support for Java 17 for `Azure Functions`.
- Added support for refreshing items (when needed) of combobox components at place.

### Changed
- Default values of most input components in Azure resource creation/deployment dialogs are now learnt from history usage records.
- Local meta-data files of Azure SDK reference book is updated to latest.

### Fixed
- Loading spring apps take more time than normal.
- Creating resources shows repeatedly in ComboBox components sometimes.
- Stopped Azure Function app won't be the default app in deploy dialog.
- App settings of a newly deployed Azure Function app won't be updated in Properties view until sign-out and sign-in again.
- Validation error message doesn't popup when hovering on the input components.
- [#6790](https://github.com/microsoft/azure-tools-for-java/issues/6790): Uncaught Exception com.intellij.serviceContainer.AlreadyDisposedException: Already disposed: Project(*) (disposed)
- [#6784](https://github.com/microsoft/azure-tools-for-java/issues/6784): Uncaught Exception com.intellij.openapi.util.TraceableDisposable$DisposalException: Library LibraryId(*) already disposed
- [#6813](https://github.com/microsoft/azure-tools-for-java/issues/6813): Uncaught Exception com.microsoft.azure.toolkit.lib.common.operation.OperationException: setup run configuration for Azure Functions

## 3.67.0
### Added
- New Azure service support: Azure Kubernetes service.
  - direct resource management in Azure Explorer.
  - connection to other K8s plugins.    
- Support for running or debugging local projects directly on Azure Virtual Machine by leveraging [`Run Targets`](https://www.jetbrains.com/help/idea/run-targets.html).     

### Changed
- Most Tool Windows will hide by default and show only when they are triggered by related actions.
- An explicit search box is added on subscription dialog to filter subscriptions more conveniently.
  - support for toggling selection of subscriptions by `space` key even checkbox is not focused.
- A loading spinner would show first when the feedback page is loading.
- Entries of some common actions in `<Toolbar>/Tools/Azure` are also added into the gear actions group of Azure Explorer.

### Fixed
- Error occurs if expand or download files/logs of a stopped function app.
- Known CVE issues.

## 3.66.0
### Added
- New "Getting Started with Azure" experience.    
- Support for IntelliJ IDEA 2022.2(EAP).
- SNAPSHOT and BETA versions of this plugin are available in [`Dev` channel](https://plugins.jetbrains.com/plugin/8053-azure-toolkit-for-intellij/versions/dev).    

### Fixed
- Error "java.lang.IllegalStateException" occurs if there are resources having same name but different resource groups.
- Configurations go back to default after deploying an artifact to a newly created Azure Spring App.
- [#6730](https://github.com/microsoft/azure-tools-for-java/issues/6730): Uncaught Exception java.lang.NullPointerException when creating/updating spring cloud app.
- [#6725](https://github.com/microsoft/azure-tools-for-java/issues/6725): Uncaught Exception com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException: you are not signed-in. when deploying to Azure Web App.
- [#6696](https://github.com/microsoft/azure-tools-for-java/issues/6696): Unable to run debug on azure java function on intellij (2022.1) with azure toolkit (3.65.1).
- [#6671](https://github.com/microsoft/azure-tools-for-java/issues/6671): Uncaught Exception java.lang.Throwable: Executor with context action id: "RunClass" was already registered!

## 3.65.0
### Added
- New "Provide feedback" experience.
  <img src="https://user-images.githubusercontent.com/69189193/171312904-f52d6991-af50-4b81-a4d9-b4186a510e14.png" alt="screenshot of 'provide feedback'" width="500"/>
- New Azure service support: Azure Application Insights
  - direct resource management in Azure Explorer.
  - resource connection from both local projects and Azure computing services.
- Enhanced Azure Spring Apps support:
  - 0.5Gi memory and 0.5vCPU for all pricing tiers.
  - Enterprise tier.
- Double clicking on leaf resource nodes in Azure Explorer will open the resource's properties editor or its portal page if it has no properties editor.

### Changed
- The default titles (i.e. "Azure") of error notifications are removed to make notification more compact. 

### Fixed
- Log/notification contains message related to deployment even if user is only creating a spring app.
- Display of Azure Explorer get messed up sometimes after restarting IDE.
- [#6634](https://github.com/microsoft/azure-tools-for-java/issues/6634): ArrayIndexOutOfBoundsException when initializing Azure Explorer.
- [#6550](https://github.com/microsoft/azure-tools-for-java/issues/6550): Uncaught Exception com.intellij.diagnostic.PluginException: User data is not supported.

## 3.64.0
### Added
- Azure Explorer: add `Resource Groups` root node to enable "app-centric" resource management.   

### Changed
- `Resource Management` (ARM) in Azure Explorer is migrated to `Resource Groups`: Azure Resource Management deployments are 
  reorganized from `Azure/Resource Management/{resource_group}/` to `Azure/Resource Groups/{resource_group}/Deployments/`.
- Rename `Azure Spring Cloud` to `Azure Spring Apps`.
- Improve stability/reliability of Authentication.

### Fixed
- All level of CVE issues until now.
- Action `Access Test Endpoint` is missing from context menu of Azure Spring app.
- `Test Endpoint` entry is missing properties view of Azure Spring app.
- [#6590](https://github.com/microsoft/azure-tools-for-java/issues/6590): ClassCastException when get Azure Functions configuration
- [#6585](https://github.com/microsoft/azure-tools-for-java/issues/6585): ClassCastException when create application insights in Azure setting panel
- [#6569](https://github.com/microsoft/azure-tools-for-java/issues/6569): Uncaught Exception: Illegal char <:> at func path
- [#6568](https://github.com/microsoft/azure-tools-for-java/issues/6568): Uncaught Exception com.intellij.serviceContainer.AlreadyDisposedException: Already disposed

## 3.63.0
### Added
- Azure Explorer: add `Provide feedback` on toolbar of Azure explorer.
- Azure Explorer: add support for pinning favorite resources.
- Storage account: add `Open in Azure Storage Explorer` action on storage account nodes to open Storage account in local Azure Storage Explorer.
- Functions: add action on Function app node to trigger http function with IntelliJ native http client(Ultimate Edition only) directly. 
- App service: add support for `Tomcat 10`/`Java 17`.

### Changed
- Azure Explorer: node of newly created resource would be automatically focused.
- Storage account: more actions (e.g. copy Primary/Secondary key...) are added on Storage accounts' nodes.
- Authentication: performance of authentication with `Azure CLI` is improved a lot.
- Proper input would be focused automatically once a dialog is opened.

### Fixed
- [#6505](https://github.com/microsoft/azure-tools-for-java/issues/6505): IntelliJ Crash When logging in to Azure on Mac OS X.
- [#6511](https://github.com/microsoft/azure-tools-for-java/issues/6511): Failed to open streaming log for Function App.
- [Test] Some apps keep loading when expand a Spring cloud service node at first time.

## 3.62.0
### Added
- Azure Explorer: a `Create` action (link to portal) is added on `Spring Cloud` node's context menu to create Spring Cloud service on portal.
- Add support for IntelliJ 2022.1 EAP。

### Changed
- You need to confirm before deleting any Azure resource.
- App Service: files can be opened in editor by double clicking.
- Azure Explorer: most context menu actions of Azure Explorer nodes can be triggered via shortcuts.
- Functions: port of Azure Functions Core tools can be customized when run Function project.
- ARM & Application Insights: migrate to Track2 SDK.

### Fixed
- [#6370](https://github.com/microsoft/azure-tools-for-java/issues/6370): Opening IntelliJ IDEA settings takes 60+ seconds with Azure Toolkit plug-in enabled.
- [#6374](https://github.com/microsoft/azure-tools-for-java/issues/6374): Azure Functions local process not killed properly on Mac/IntelliJ.
- MySQL/SQL/PostgreSQL server: NPE when open properties editor of a deleting MySQL/SQL/PostgreSQL server.
- MySQL/SQL/PostgreSQL server: expandable sections in properties view of a stopped MySQL server can be folded but can not be expanded.
- Redis Cache: Redis data explorer UI blocks when read data in non-first database.
- Redis Cache: pricing tier keeps `Basic C0` no matter what user selects in Redis cache creation dialog.

## 3.61.1

### Fixed
- [#6364](https://github.com/microsoft/azure-tools-for-java/issues/6364): [IntelliJ][ReportedByUser] Uncaught Exception com.intellij.ide.ui.UITheme$1@5b3f3ba0 
  cannot patch icon path java.lang.StringIndexOutOfBoundsException: String index out of range: 0

## 3.61.0
### Added
- Add a placeholder tree node in Azure Explorer for resource that is being created.
- Show the status of resources explicitly on Azure Explorer node(right after the name).

### Changed
- Details of items in combo box are now loaded lazily, so that user needn't to wait too long.
  for the items to be ready to be selected when deploy a WebApp/Function App.
- Nodes in Azure Explorer are now ordered alphabetically.

### Fixed
- NPE when connecting storage account/redis cache if there is no such resources in the selected subscription.
- No default Offer/Sku/Image are selected in "Select VM Image" dialog.
- Validation passed in Create VM dialog even Maximum price per hour is empty.
- Some modified values will be changed to default when switch back to "More settings" in "Create App Service" dialog.
- Validation message is not right when selected subscription has no spring cloud service.
- Tooltips on nodes in azure explorer are not correct.
- Error occurs when run or deploy after docker support is added.
- Icon of action "Open by Database Tools" for PostgreSQL is missing.

## 3.60.2
### Changed
- upgrade log4j to the latest v2.17.1

## 3.60.1
### Fixed
- [#6294](https://github.com/microsoft/azure-tools-for-java/issues/6294): Uncaught Exception cannot create configurable component java.lang.NullPointerException
- Signin status will not keep after restarting if user signed in with Service Principal

## 3.60.0
### Added
- Add dependency support for Azure Functions related libs, so that our plugin can be recommended.
- Add actions on some error notifications, so that user knows what to do next.
- Add account registration link in "Sign in" dialog.

### Changed
- Performance of restoring-sign-in is improved.

### Fixed
- [#6120](https://github.com/microsoft/azure-tools-for-java/issues/6120): AzureOperationException: list all function modules in project.
- [#6090](https://github.com/microsoft/azure-tools-for-java/issues/6090): Uncaught Exception java.nio.file.InvalidPathException: Illegal char <:> at index 16: Active code page: 1252.
- [#5038](https://github.com/microsoft/azure-tools-for-java/issues/5038): Dependent Module Jars Are Not Added When Debugging With IDEA.
- [#5035](https://github.com/microsoft/azure-tools-for-java/issues/5035): Resources Are Not Added To Jar When Debugging With IDEA.
- [#6026](https://github.com/microsoft/azure-tools-for-java/issues/6026): Uncaught Exception java.lang.NullPointerException.
- Azure Explorer: some nodes are not sorted in natural order.
- Azure Explorer: keeps showing "signing in..." for a long time after restarting IntelliJ.
- Virtual Machine: Validation info about name of resource group and virtual machine doesn't contain letters length.
- Storage Account: "open in portal" and "open storage explorer" link to a same page.
- Spring Cloud: there is No default value for CPU and Memory if click more settings quickly in "Create Spring Cloud App" dialog.
- MySQL/SqlServer/PostgreSQL: Test connection result text box has white background color in IntelliJ Light theme.
- Postgre SQL: No icon in properties view tab title.
- Some message/icon related bugs.
- CVE issues

## 3.59.0
### Added
- Add Support for **Azure Database for PostgreSQL**, so that user can create/manage/consume PostgreSQL directly in IntelliJ.
  - create/manage PostgreSQL server instances
  - connect PostgreSQL with Intellij's DB Tools
  - consume PostgreSQL from local project/Azure WebApp via Resource Connector feature. 
- Add `Add SSH Configuration` on **Azure Virtual Machine** instance nodes, so that user can add Azure VM to SSH Configurations by one click. 
- Add dependency support for Azure SDK libs, so that our plugin will be suggested if Azure SDK is used but the plugin is not installed. 
- Add support for 2021.3

### Changed
- BeforeRunTask for Azure Resource Connector will show which resources are connected to the run configuration.

### Fixed
- CVE issues.
- progress indicator shows `<unknown>.<unknow>` on menu actions.
- URL starts with 'http' instead of 'https' in Web App properties window and Open in Browser option
- Pops up com.azure.core.management.exception.ManagementException while deploying spring cloud with a creating service
- Local run the project for connector, often pops up the error "java.util.ConcurrentModificationException"
- No validation for invalid values with VNet and Public IP in Create VM dialog
- Pops up NPE error with invalid values for Quota and Retention Period in Create Web App dialog
- Web App name and Function name can't pass if it starts with numbers
- Unclear validation info for invalid values in Create new Application Insights dialog
- BeforeRunTask `Azure Resource Connector` will not be added to self-defined run configuration
- Reopen projects for connector, often pops up the error "java.lang.ClassCastException"
- Pops up NPE when searching with GET and showing hash typed key value with Redis
- Creating Web App can be successfully submitted even with an existing name

## 3.58.0
### Added
- Add support for Azure AD (Preview)
  - Register Azure AD application
  - View Azure AD application templates
- Support connect to Azure Redis for spring boot project

### Changed
- Remove outdated spring cloud dependency management

### Fixed
- [#5923](https://github.com/microsoft/azure-tools-for-java/pull/5923) Fix bug: ADLA accounts can't be listed in Spark on Cosmos subscription issue
- [#5968](https://github.com/microsoft/azure-tools-for-java/pull/5968) Fix bug: HDInsight project wizard accessibility issue
- [#5996](https://github.com/microsoft/azure-tools-for-java/pull/5996) Fix bug: Config not applied when starting livy interactive console

## 3.57.1
### Fixed
- [#5888](https://github.com/microsoft/azure-tools-for-java/issues/5888) Fix bug: Conflicting component name 'RunManager'

## 3.57.0
### Added
- Support connect to Azure Storage account for spring boot project

### Changed
- Redesign the creation UI of VM
- Redesign the creation UI of Redis
- Show supported regions only for Redis/MySql/Sql Server/Storage account in creation dialog
- Remove JBoss 7.2 from webapp since it is deprecated
- Show intermediate status for login restore

### Fixed
- [#5857](https://github.com/microsoft/azure-tools-for-java/pull/5857) Fix bug: fail to load plugin error
- [#5761](https://github.com/microsoft/azure-tools-for-java/issues/5761) Fix bug: generated funciton jar cannot be started
- [#1781](https://github.com/microsoft/azure-maven-plugins/pull/1781) Fix bug: blank Sql Server version in property view

## 3.56.0
### Added
- Support proxy with credential(username, password)
- Add `Samples` link for SDK libs on Azure SDK reference book

### Changed
- Fix the high failure rate problem for SSH into Linux Webapp operation
- List all local-installed function core tools for function core tools path setting
- Synchronize status on storage account in different views
- Synchronize status on Azure Database for MySQL in different views
- Synchronize status on SQL Server in different views
- Redesign the creation UI of storage account

## 3.55.1
### Added
- Add support for IntelliJ 2021.2

## 3.55.0
### Added
- New Azure Resource Connector explorer for connection management
  - List all resource connections connected to project
  - Create new connections between Azure resources and module in project
  - Edit/Delete existing connections
  - Navigate to resource properties view of an existing connection
- Support native proxy settings in IntelliJ
- Add unified `Open In Portal` support for Web App/VM/Resource Group in Azure explorer

### Changed
- Enhance toolkit setting panel with more configuration
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
 - Separator for multiple referenced jars and referenced files is changed from semicolon to space in Spark batch job configuration
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
