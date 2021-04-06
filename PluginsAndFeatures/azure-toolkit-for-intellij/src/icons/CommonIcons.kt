/**
 * Copyright (c) 2019-2020 JetBrains s.r.o.
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object CommonIcons {

    @JvmField val Azure = load("Common/Azure.svg")
    @JvmField val AzureExplorer = load("AzureExplorer/AzureExplorer.svg")
    @JvmField val Subscriptions = load("Common/Subscriptions.svg")
    @JvmField val Database = load("SQLDatabase/Database.svg")
    @JvmField val WebApp = load("WebApp/WebApp.svg")
    @JvmField val Firewall = load("Common/Firewall.svg")

    @JvmField val Warning = load("Common/Warning.svg")

    @JvmField val Search = load("Common/Search.svg")
    @JvmField val Refresh = load("Common/Refresh.svg")
    @JvmField val OpenParent = load("Common/OpenParent.svg")
    @JvmField val Discard = load("Common/Delete.svg")
    @JvmField val SaveChanges = load("Common/SaveChanges.svg")
    @JvmField val Open = load("Common/Open.svg")
    @JvmField val Upload = load("Common/Upload.svg")

    @JvmField val PublishAzure = load("Common/PublishAzure.svg")
    @JvmField val CodeSamples = load("Common/CodeSamples.svg")

    @JvmField val Azurite = load("StorageAccount/AzureStorageEmulator.svg")

    @JvmField val AzureActiveDirectory = load("ActiveDirectory/ActiveDirectory.svg")

    object OS {
        @JvmField val Windows = load("Common/OSWindows.svg")
        @JvmField val Linux = load("Common/OSLinux.svg")
    }

    object CloudShell {
        @JvmField val AzureOpenCloudShell = load("CloudShell/OpenCloudShell.svg")
    }

    object ToolWindow {
        @JvmField val Azure = load("ToolWindow/AzureToolWindow.svg")
        @JvmField val AzureLog = load("ToolWindow/ActivityLog.svg")
        @JvmField val AzureStreamingLog = load("ToolWindow/StreamingLog.svg")
    }

    object AzureFunctions {
        @JvmField val FunctionApp = load("FunctionApp/FunctionApp.svg")
        @JvmField val FunctionAppRunConfiguration = load("FunctionApp/FunctionAppRunConfiguration.svg")
        @JvmField val FunctionAppConfigurationType = load("FunctionApp/FunctionApp.svg")
        @JvmField val TemplateAzureFunc = load("FunctionApp/TemplateAzureFunc.svg")
    }

    @Suppress("unused")
    object ResourceManagement {
        @JvmField val ResourceManagement = load("ResourceManagement/ResourceManagement.svg")
        @JvmField val ResourceGroup = load("ResourceManagement/ResourceManagementResourceGroup.svg")
        @JvmField val Deployment = load("ResourceManagement/ResourceManagementDeployment.svg")
    }

    private fun load(path: String): Icon = IconLoader.getIcon("/icons/$path", this::class.java)
}