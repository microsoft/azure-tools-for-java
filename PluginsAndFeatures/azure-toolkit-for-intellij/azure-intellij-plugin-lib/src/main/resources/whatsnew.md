<!-- Version: 3.68.0 -->
# What's new in Azure Toolkit for IntelliJ

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

## 3.64.0
### Added
- Azure Explorer: add `Resource Groups` root node to enable "app-centric" resource management.   
  <img src="https://user-images.githubusercontent.com/69189193/165674211-c21161e8-56a0-4dd9-95a5-677755112715.png" alt="resource groups" width="400"/>

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
  <img src="https://user-images.githubusercontent.com/69189193/160977559-0099d286-304a-42de-88c9-6407b8a0c09e.png" alt="provide feedback" width="400"/>
- Azure Explorer: add support for pinning favorite resources.    
  <img src="https://user-images.githubusercontent.com/69189193/160977373-ba28e5b2-87e8-43be-98bc-2cbe4e485fd4.png" alt="pin favorite resources" width="400"/>
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
- Some apps keep loading when expand a Spring cloud service node at first time.

## 3.62.0
### Added
- Add support for IntelliJ 2022.1 EAP.
- Azure Explorer: a `Create` action (link to portal) is added on `Spring Cloud` node's context menu to create Spring Cloud service on portal.

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
- Details of items in combo box are now loaded lazily, so that user needn't to wait too long 
  for the items to be ready to be selected when deploy a WebApp/Function App
- Nodes in Azure Explorer are now ordered alphabetically.

### Fixed
- NPE when connecting storage account/redis cache if there is no such resources in the selected subscription.
- No default Offer/Sku/Image are selected in "Select VM Image" dialog.
- Validation passed in Create VM dialog even Maximum price per hour is empty.
- Some modified values will be changed to default when switch back to "More settings" in "Create App Service" dialog
- Validation message is not right when selected subscription has no spring cloud service
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
- [#6120](https://github.com/microsoft/azure-tools-for-java/issues/6120) AzureOperationException: list all function modules in project
- [#6090](https://github.com/microsoft/azure-tools-for-java/issues/6090) Uncaught Exception java.nio.file.InvalidPathException: Illegal char <:> at index 16: Active code page: 1252
- [#5038](https://github.com/microsoft/azure-tools-for-java/issues/5038) Dependent Module Jars Are Not Added When Debugging With IDEA
- [#5035](https://github.com/microsoft/azure-tools-for-java/issues/5035) Resources Are Not Added To Jar When Debugging With IDEA
- [#6026](https://github.com/microsoft/azure-tools-for-java/issues/6026) Uncaught Exception java.lang.NullPointerException
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
We are pround to annouce the support for Azure AD in Azure Toolkit for IntelliJ! You may create your Azure AD application in IntelliJ and view the AD application templates as well as correspond code snippets. All the features could be founded in menu `Tools` -> `Azure`

  <img src="https://user-images.githubusercontent.com/12445236/139086423-8e6257f6-ae4b-4250-8347-8fba2b3cd077.png" alt="Register Azure AD Application" width="600"/>

  <img src="https://user-images.githubusercontent.com/12445236/139086065-e4ff319a-f974-4c1c-bd47-e4768f531f76.png" alt="Azure AD Application Templates" width="600"/>

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

    <img src="https://user-images.githubusercontent.com/12445236/135217488-09cbbc7a-620d-4d4b-9c91-5cbcb649dd32.png" />

    <img src="https://user-images.githubusercontent.com/12445236/135217769-4a411f20-ceee-4d44-9c16-33b80a1b19bd.png" />

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

  <img src="https://user-images.githubusercontent.com/19339116/130544615-7a8824aa-0a43-4a07-b91f-5ef10c017e40.png" />

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
