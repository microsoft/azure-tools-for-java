[![official JetBrains project](http://jb.gg/badges/official-flat-square.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

# Azure Toolkit for Rider [![Teamcity](http://teamcity.jetbrains.com/app/rest/builds/buildType:(id:AzureToolsForIntellij_Build)/statusIcon)](http://teamcity.jetbrains.com/viewType.html?buildTypeId=AzureToolsForIntellij_Build&guest=1)

The Azure Toolkit for [JetBrains Rider](https://www.jetbrains.com/rider) is an open-source project that helps .NET developers easily create, develop, configure, test, manage and deploy highly available and scalable web apps to Azure.

The plugin can be downloaded and installed in JetBrains Rider and is available [from the JetBrains plugins repository](https://plugins.jetbrains.com/plugin/11220-azure-toolkit-for-rider).

## Functionality

* Sign in to an Azure account
* Manage one or multiple subscriptions
* Basic management of container hosts (create/delete, start/stop, view details)
* List container registries
* Basic management of Redis caches (create/delete, start/stop, view details, list keys/values)
* Basic management of Sql databases (list, delete, open in browser, add firewall rule for current public IP, connect to database in database tools)
* Basic management of storage accounts (create/delete, list/create/delete blob container, list/upload/download/delete blobs)
* Basic management of virtual machines (create/delete, start/stop, view details)
* Basic management of web apps and deployment slots (create/delete, start/stop, view details, edit settings, swap slot)
* Run configuration to deploy ASP.NET web apps (can also provision SQL database)
  * ASP.NET Core web apps (any platform)
  * .NET framework web apps (Windows)
* Azure Cloud Shell support
  * Connect to cloud shell and work with terminal
  * Upload file action in Rider, support `download <file>` command in terminal
  * Support for `az aks browse` command opening browser

Feature requests can be logged in our [issue tracker](https://github.com/JetBrains/azure-tools-for-intellij/issues), we also welcome contributions.

## Resources

* [Issue tracker](https://github.com/JetBrains/azure-tools-for-intellij/issues)
* [Plugin page](https://plugins.jetbrains.com/plugin/11220-azure-toolkit-for-rider)

## History and differences with Microsoft Azure Toolkit for IntelliJ

The Azure Toolkit for [JetBrains Rider](https://www.jetbrains.com/rider) is a fork of the [Azure Toolkit for IntelliJ](https://docs.microsoft.com/en-us/java/azure/intellij/azure-toolkit-for-intellij-installation), available [on GitHub](https://github.com/Microsoft/azure-tools-for-java).

Microsoft's Azure Toolkit for IntelliJ provides similar functionality to the Azure plugin for [JetBrains Rider](https://www.jetbrains.com/rider), however focus on the Java/JVM ecosystem and development flows. JetBrains decided to fork the original plugin, and split base functionality (such as browsing Azure resources) from Java/JVM-specific features (such as deploying a `.war` file to the HDInsight service).

The Azure Toolkit for [JetBrains Rider](https://www.jetbrains.com/rider) is released with several notable differences:

* No telemetry or usage data is collected and sent to Microsoft
* Icons have been replaced by custom icons, as [the original icons are not open-source](https://github.com/Microsoft/azure-tools-for-java/issues/1626)
* Java/JVM-specific functionality was removed
* .NET-specific functionality, such as deploying an ASP.NET web application, has been added

JetBrains [opened a pull request](https://github.com/Microsoft/azure-tools-for-java/pull/1725) to Microsoft's Azure Toolkit for IntelliJ as well, which would contribute this effort back to the original plugin, and open up the original plugin to various other ecosystems.

## Contributing

Please see the [contribution instructions](CONTRIBUTING.md) if you wish to build the plugin from source.
