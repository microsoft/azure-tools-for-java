<!-- Version: 3.88.0 -->
# What's new in Azure Toolkit for IntelliJ

## 3.90.0
### Added
- Support IntelliJ 2024.2 EAP
- Support workload profiles environment type in Azure Container Apps
  - Support create workload profiles during the creation of an Azure Container Apps environment.
  - Enable setting of workload profiles during the creation of container apps and function apps.

## 3.89.0
### Added
- Added a "get started with Azure Container Apps" course.

### Fixed
- Error occurs when pushing image to container registry with docker file.
- WebApp of deploy WebApp run configuration shows incorrectly after deploying to a slot.
- "Load .env" before run task doesn't show in cases when deploying Azure WebApp.
- Some other know issues.

## 3.88.1
### Added
- Support creation/deployment for flex consumption function app.

### Fixed
- Fixes duplicate before run tasks for Azure Container Apps deployment run configuration

## 3.88.0
### Added
- Support for deploying source code/artifacts to Azure Container Apps directly.      
  <img alt="deploy source code to Azure" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202404/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib-java/src/main/resources/whatsnew.assets/202404.aca.newdeploymenttypes.png" width="600"/>

### Fixed
- error throws when starting streaming log for container apps env.

## 3.87.0
### Added
- Support for connecting Azure Storage account with connection string and managing them in both `Services` view and `Project Explorer`. 

### Fixed
- [#8205](https://github.com/microsoft/azure-tools-for-java/issues/8205): Could not emit tick xxx due to lack of requests (interval doesn't support small downstream requests that replenish slower than the ticks)
- [azure-sdk-for-java#39214] (https://github.com/Azure/azure-sdk-for-java/issues/39214) `msal-*` libs are falsely treated as "deprecated".

## 3.86.1
### Fixed
- [#8206](https://github.com/microsoft/azure-tools-for-java/issues/8206): After saving the web app run configuration, the app settings will be cleared.

## 3.86.0
### Added
- Support for managing Azure resources in integrated `Services` view.     
- Feature recommendation based on user project dependencies.

### Updated
- Startup notifications are quequed to prevent distraction.

### Fixed
- The link in warning for java 21 goes to a wrong page.
- Function fails running using cli downloaded with the plugin.
- [#8139](https://github.com/microsoft/azure-tools-for-java/issues/8139): NPE at web app configuration validation.

## 3.85.0
### Added
- Support for containerized FunctionApps (based on Azure Container Apps) 
- Support for creating docker WebApp/FunctionApp
- Support for Managing Azure Container Registries (ACR) with bundled Docker plugin (2023.3) in integrated `Services` view.     
- Warn user when creating/deploying WebApps/FunctionApps of (to-be-) deprected runtime.

### Updated
- Update icon of WebApp/FunctionApp to indicate its runtime.

### Fixed
- [#8118](https://github.com/microsoft/azure-tools-for-java/issues/8118): NPE at EnvVarLineMarkerProvider.
- [#8113](https://github.com/microsoft/azure-tools-for-java/issues/8113): java.nio.file.NoSuchFileException at AzureModule.getDefaultProfileName.
- [#8109](https://github.com/microsoft/azure-tools-for-java/issues/8109): java.lang.ClassCastException at StoragePathPrefixCompletionProvider.addCompletions.
- [#8093](https://github.com/microsoft/azure-tools-for-java/issues/8093): Speed Search not working correctly.
- [#8081](https://github.com/microsoft/azure-tools-for-java/issues/8081): java.lang.AssertionError at AzureCognitiveServices.accounts.
- [#8063](https://github.com/microsoft/azure-tools-for-java/issues/8063): java.lang.ArrayIndexOutOfBoundsException at TreeUtils.selectResourceNode.

## 3.84.0
### Added
- Browse Azure sample projects and create new project from them.     
  <img alt="browse azure sample projects" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202312/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib-java/src/main/resources/whatsnew.assets/202312.azure-samples.gif" width="1000"/>

- More "shortcut" actions to manage Azure Kubernetes Service (AKS) resources with bundled Kubernetes plugin (2023.3).     
  <img alt="add AKS into Kubernetes Explorer" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202312/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib-java/src/main/resources/whatsnew.assets/202312.k8s-1.png" width="600"/>     
  <img alt="open AKS in Kubernetes Explorer" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202312/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib-java/src/main/resources/whatsnew.assets/202312.k8s-2.png" width="600"/>

### Updated
- Migrate to use App Service stack API to get supported runtimes for Azure WebApps/Functions.

## 3.83.0
### Added
- Add Azure Key Vault support in Azure Toolkits
  * Resource Management features in Azure explorer
    - Create new secret/certificate/key in toolkts
    - View/Download secret/certificate/key (need Azure CLI installed)
  * Code assistance of Key Vault for Spring project    
    <img alt="keyvault code assistance" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202311/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib-java/src/main/resources/whatsnew.assets/202311.keyvaultcodeassistance.gif" width="1000"/>    
    <img alt="create secret from plain text" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202311/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib-java/src/main/resources/whatsnew.assets/202311.createsecretfromplaintext.gif" width="1000"/>

### Fixed
- Get the error of "AzureToolkitRuntimeException" when opening project with resource connection configuration file
- Get duplicate property key when connect resource from .properties
- Generate deprecated configurations with code completions in spring properties
- Build task will be removed for project with resource connection

## 3.82.0
### Added
- Code assistance of Azure resources for Spring and Azure Functions.       
  <img alt="spring cloud code assistance" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202310/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib-java/src/main/resources/whatsnew.assets/202310.springcodeassistance.gif" width="1000"/>    
  <img alt="function code assistance" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202310/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib-java/src/main/resources/whatsnew.assets/202310.functioncodeassistance.gif" width="1000"/>   
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
  <img alt="azure openai playground" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202308/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib-java/src/main/resources/whatsnew.assets/202308.openai-playground.gif" width="1000"/>
- Guidance (Getting started course) to try Azure OpenAI and its playground (chat) in IntelliJ IDEA.    
  <img alt="getting started with azure openai" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202308/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib-java/src/main/resources/whatsnew.assets/202308.openai-getting-started.gif" width="1000"/>
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
  <img alt="create spring apps" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202307/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202307.create-spring-apps.png" width="500"/>
- Support for managing deployment target services directly in Project Explorer.    
  <img alt="deployment targets" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202307/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202307.deployment-targets.png" width="600"/>

### Fixed
- status shows inactive after creating/refreshing spring app.
- error may occur when importing document into SQL container.
- error may occur when connecting to the storage emulator and running locally.
- error may occur when deploy function app.
- HDInsight Job view nodes are displayed as 'folder icon + cluster name'.
- HDInsight Linked cluster cannot display in Azure Explorer when not signed in.

## 3.78.0
### Added
- New UX for Azure resource connections in IntelliJ project view
  - Support list/add/remove Azure resource connections in project explorer
  - Support edit environment variables for Azure resource connections
  - Support manage connected Azure resources in project explorer

  <img alt="app-centric" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202306/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202306.app-centric.gif" width="1000"/>
- Support IntelliJ 2023.2 EAP

### Fixed
- Fix: System environment variables may be missed during function run/deployment
- [#7651](https://github.com/microsoft/azure-tools-for-java/issues/7651): Uncaught Exception DeployFunctionAppAction#update, check if project is a valid function project.
- [#7653](https://github.com/microsoft/azure-tools-for-java/issues/7653): Uncaught Exception com.intellij.diagnostic.PluginException: No display name is specified for configurable com.microsoft.azure.toolkit.intellij.settings.AzureSettingsConfigurable in xml file.
- [#7619](https://github.com/microsoft/azure-tools-for-java/issues/7619): Uncaught Exception Uncaught Exception java.lang.IllegalArgumentException: invalid arguments id/nameId.

## 3.77.0
### Added
- Azure Spring Apps: basic Standard Consumption plan(preview) support.
- Azure Storage Account: local Storage Account Emulator (Azurite) support.

  <img alt="Azurite" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202305/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202305.azurite.gif" width="1000"/>

### Changed
- Azure Spring Apps: performance of creating/updating resources is improved.
- Azure Functions: users are asked to select an Cloud/Emulated Storage Account in case of missing `AzureWebJobsStorage` at local run instead of fail directly.
- Resource Connection: data related to resource connections are moved from project dir to module dir and the schema is also changed.

### Fixed
- [#7564](https://github.com/microsoft/azure-tools-for-java/issues/7564): Uncaught Exception java.lang.NullPointerException: Cannot invoke "com.microsoft.azure.toolkit.ide.common.store.IIdeStore.getProperty(String, String)" because "store" is null.
- [#7561](https://github.com/microsoft/azure-tools-for-java/issues/7561): Uncaught Exception com.intellij.diagnostic.PluginException: 644 ms to call on EDT DeployFunctionAppAction#update@MainMenu (com.microsoft.azure.toolkit.intellij.legacy.function.action.DeployFunctionAppAction).
- [#7421](https://github.com/microsoft/azure-tools-for-java/issues/7421): Uncaught Exception com.intellij.diagnostic.PluginException: 303 ms to call on EDT ServerExplorerToolWindowFactory$RefreshAllAction#update@ToolwindowTitle (com.microsoft.azure.toolkit.intellij.explorer.ServerExplorerToolWindowFactory$RefreshAllAction).
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
  <img alt="Azure Container Apps" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202304/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202304.aca.gif" width="1000"/>
- Finer granular resource management(registry/repository/images/...) for Azure Container Registry    
  <img alt="Azure Container Registry" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202304/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202304.acr.png" width="1000"/>
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
  <img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202301/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202301.azure-monitor.gif" alt="gif of Azure Monitor"/>
- [Azure Container Apps] Creation of Azure Container Apps Environment.    
- [Azure Explorer] Pagination support in Azure Explorer.    
  <img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202301/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202301.loadmore.png" alt="load more in azure explorer"/>

### Changed
- Update default Java runtime to Java 11 when creating Azure Spring App.
- Add setting item to allow users to choose whether to enable authentication cache.    
  <img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202301/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202301.enableauthcache.png" alt="setting item to enable auth cache"/>

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
  <img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202211/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202211.sdk.gif" alt="gif of examples in sdk reference book"/>
- Function Core Tools can be installed and configured automatically inside IDE.
- Data sources can be created by selecting an existing Azure Database for MySQL/PostgreSQL or Azure SQL. (Ultimate Edition only)<br>
  <img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202211/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202211.sqldatabase.png" alt="screenshot of 'creating data source'"/>

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
- Added support for remote debugging of `Azure Spring Apps`.<br>
<img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202210/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202210.springremotedebugging.gif" alt="screenshot of 'spring remote debugging'" width="1200"/>
- Added support for remote debugging of `Azure Function Apps`.<br>
<img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202210/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202210.functionremotedebugging.gif" alt="screenshot of 'function remote debugging'" width="1200"/>
- Added support for data management of `Azure Storage Account` in Azure Explorer.<br>
<img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202210/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202210.storageaccount.png" alt="screenshot of 'storage account'" width="400"/>
- Added support for data management of `Azure Cosmos DB account` in Azure Explorer.<br>
  <img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202210/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202210.cosmosdb.png" alt="screenshot of 'cosmos db account'" width="1000"/>
- Added support for filtering app settings of `Azure Web App/ Function App` in properties view and run configuration dialog.<br>
  <img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202210/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202210.filterappsettings.png" alt="screenshot of 'app settings configuration'" width="600"/>

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
