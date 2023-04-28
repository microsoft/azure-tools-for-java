<!-- Version: 3.76.0 -->
# What's new in Azure Toolkit for IntelliJ

## 3.76.0
### Added
- Basic resource management support for service connections
- New one click action to deploy Dockerfile (build image first) to Azure Container App
  <img alt="Azure Container Apps" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202304/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202304.aca.gif" width="500"/>
- Finer granular resource management(registry/repository/images/...) for Azure Container Registry    
  <img alt="Azure Container Registry" src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202304/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202304.acr.png" width="500"/>
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
  <img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202210/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202210.cosmosdb.png" alt="screenshot of 'cosmos db account'" width="500"/>
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
  <img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202208/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202208.datasource.gif" alt="screenshot of 'cosmos datasource'" width="1200"/>
- Added support for connecting an `Azure Virtual Machine` using SSH directly from an `Azure Virtual Machine` resource node in Azure Explorer.
- Added support for browsing files of an `Azure Virtual Machine` from an `Azure Virtual Machine` resource node in Azure Explorer (`IntelliJ IDEA Ultimate Edition` only).      
  <img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202208/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202208.vm.gif" alt="screenshot of 'virtual machine'" width="1200"/>
- Added support for adding dependencies to current local project from `Azure SDK reference book`.
- Added support for jumping to corresponding Azure SDK page in `Azure SDK reference book` from Azure Explorer nodes.      
  <img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202208/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202208.sdk.gif" alt="screenshot of 'sdk reference book'" width="1200"/>
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
  <img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202207/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202207.k8s.gif" alt="screenshot of 'k8s'" width="500"/>
- Support for running or debugging local projects directly on Azure Virtual Machine by leveraging [`Run Targets`](https://www.jetbrains.com/help/idea/run-targets.html).     
  <img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202207/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202207.runtarget.png" alt="screenshot of 'run target'" width="500"/>

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
  <img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202206/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202206.gettingstarted.gif" alt="screenshot of 'getting started'" width="500"/>
- Support for IntelliJ IDEA 2022.2(EAP).
- SNAPSHOT and BETA versions of this plugin are available in [`Dev` channel](https://plugins.jetbrains.com/plugin/8053-azure-toolkit-for-intellij/versions/dev).    
  <img src="https://raw.githubusercontent.com/microsoft/azure-tools-for-java/endgame-202206/PluginsAndFeatures/azure-toolkit-for-intellij/azure-intellij-plugin-lib/src/main/resources/whatsnew.assets/202206.devchannel.png" alt="screenshot of 'dev channel'" width="500"/>

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
