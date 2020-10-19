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

    @JvmField val Azure = load("Azure.svg")
    @JvmField val AzureExplorer = load("AzureExplorer.svg")
    @JvmField val Subscriptions = load("Subscriptions.svg")
    @JvmField val Database = load("Database.svg")
    @JvmField val WebApp = load("WebApp.svg")
    @JvmField val Firewall = load("Firewall.svg")

    @JvmField val Warning = load("AzureWarning.svg")

    @JvmField val Search = load("Search.svg")
    @JvmField val Refresh = load("refresh.svg")
    @JvmField val OpenParent = load("AzureOpenParent.svg")
    @JvmField val Discard = load("Discard.svg")
    @JvmField val SaveChanges = load("SaveChanges.svg")
    @JvmField val Open = load("AzureOpen.svg")
    @JvmField val Upload = load("AzureUpload.svg")

    @JvmField val PublishAzure = load("publishAzure.svg")
    @JvmField val CodeSamples = load("CodeSamples.svg")

    @JvmField val Azurite = load("AzureStorageEmulator.svg")

    @JvmField val AzureActiveDirectory = load("AzureActiveDirectory.svg")

    object OS {
        @JvmField val Windows = load("OSWindows.svg")
        @JvmField val Linux = load("OSLinux.svg")
    }

    object CloudShell {
        @JvmField val AzureOpenCloudShell = load("AzureOpenCloudShell.svg")
    }

    object ToolWindow {
        @JvmField val Azure = load("toolWindowAzure.svg")
        @JvmField val AzureLog = load("toolWindowAzureLog.svg")
        @JvmField val AzureStreamingLog = load("toolWindowAzureStreamingLog.svg")
    }

    object AzureFunctions {
        @JvmField val FunctionApp = load("FunctionApp.svg")
        @JvmField val FunctionAppRunConfiguration = load("FunctionAppRunConfiguration.svg")
        @JvmField val FunctionAppConfigurationType = load("FunctionApp.svg")
        @JvmField val TemplateAzureFunc = load("TemplateAzureFunc.svg")
    }

    @Suppress("unused")
    object ResourceManagement {
        @JvmField val ResourceManagement = load("AzureARM.svg")
        @JvmField val ResourceGroup = load("AzureARMResourceGroup.svg")
        @JvmField val Deployment = load("AzureARMDeployment.svg")
    }

    private fun load(path: String): Icon = IconLoader.getIcon("/icons/$path", this::class.java)
}