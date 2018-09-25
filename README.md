[![official JetBrains project](http://jb.gg/badges/official-flat-square.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)

# Azure plugin for JetBrains Rider [![Teamcity](http://teamcity.jetbrains.com/app/rest/builds/buildType:(id:AzureToolsForIntellij_Build)/statusIcon)](http://teamcity.jetbrains.com/viewType.html?buildTypeId=AzureToolsForIntellij_Build&guest=1)

The Azure plugin for [JetBrains Rider](https://www.jetbrains.com/rider) is an open-source project that helps .NET developers easily create, develop, configure, test, manage and deploy highly available and scalable web apps to Azure.

## Functionality

* Sign in to an Azure account
* Manage one or multiple subscriptions
* Basic management of container hosts (create/delete, start/stop, view details)
* List container registries
* Basic management of Redis caches (create/delete, start/stop, view details, list keys/values)
* Basic management of storage accounts (create/delete, list/create/delete blob container, list/upload/download/delete blobs)
* Basic management of virtual machines (create/delete, start/stop, view details)
* Basic management of web apps (create/delete, start/stop, view details)
* Run configuration to deploy ASP.NET Core web app (can also provision SQL database)

Feature requests can be logged in our [issue tracker](https://github.com/JetBrains/azure-tools-for-intellij/issues), we also welcome contributions.

## Resources

* [Issue tracker](https://github.com/JetBrains/azure-tools-for-intellij/issues)
* [Plugin page](https://plugins.jetbrains.com/)

## History and differences with Microsoft Azure Toolkits for Java

The Azure plugin for [JetBrains Rider](https://www.jetbrains.com/rider) is a fork of the [Microsoft Azure Toolkits for Java](https://docs.microsoft.com/en-us/java/azure/intellij/azure-toolkit-for-intellij-installation), available [on GitHub](https://github.com/Microsoft/azure-tools-for-java).

Microsoft's Azure Toolkits for Java provide similar functionality to the Azure plugin for [JetBrains Rider](https://www.jetbrains.com/rider), however focus on the Java/JVM ecosystem and development flows. JetBrains [opened a pull request](https://github.com/Microsoft/azure-tools-for-java/pull/1725) to split base functionality (such as browsing Azure resources) from Java/JVM-specific features (such as deploying a `.war` file to the HDInsight service), which would open up the original plugin for various other ecosystems.

While waiting for this PR to be accepted, we are releasing the forked Azure plugin for [JetBrains Rider](https://www.jetbrains.com/rider), with several notable differences:

* No telemetry or usage data is collected and sent to Microsoft
* Icons have been replaced by custom icons, as [the original icons are not open-source](https://github.com/Microsoft/azure-tools-for-java/issues/1626)
* Java/JVM-specific functionality was removed
* .NET-specific functionality, such as deploying an ASP.NET Core web application, has been added

## Contributing

Please see the [contribution instructions](CONTRIBUTING.md) if you wish to build the plugin from source.

## Disclaimer

*azure-tools-for-java uses JxBrowser http://www.teamdev.com/jxbrowser, which is proprietary software. The use of JxBrowser is governed by JxBrowser Product Licence Agreement http://www.teamdev.com/jxbrowser-licence-agreement. If you would like to use JxBrowser in your development, please contact TeamDev.*
