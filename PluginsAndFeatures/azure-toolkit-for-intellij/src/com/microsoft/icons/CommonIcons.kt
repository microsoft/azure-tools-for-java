/**
 * Copyright (c) 2019 JetBrains s.r.o.
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object CommonIcons {

    val Azure by lazy { load("Azure.svg") }
    val Subscriptions by lazy { load("Subscriptions.svg") }
    val Database by lazy { load("Database.svg") }
    val WebApp by lazy { load("WebApp.svg") }
    val Firewall by lazy { load("Firewall.svg") }

    val Warning by lazy { load("AzureWarning.svg") }

    val Search by lazy { load("Search.svg") }
    val Refresh by lazy { load("refresh.svg") }
    val OpenParent by lazy { load("AzureOpenParent.svg") }
    val Discard by lazy { load("Discard.svg") }
    val SaveChanges by lazy { load("SaveChanges.svg") }
    val Open by lazy { load("AzureOpen.svg") }
    val Upload by lazy { load("AzureUpload.svg") }
    val Download by lazy { load("download.svg") }

    val PublishAzure by lazy { load("publishAzure.svg") }

    val Azurite by lazy { load("AzureStorageEmulator.svg") }

    object OS {
        val Windows by lazy { load("OSWindows.svg") }
        val Linux by lazy { load("OSLinux.svg") }
    }

    object AzureFunctions {
        val FunctionApp by lazy { load("FunctionApp.svg") }
        val FunctionAppRunConfiguration by lazy { load("FunctionAppRunConfiguration.svg") }
        val FunctionAppConfigurationType by lazy { load("FunctionApp.svg") }
        val TemplateAzureFunc by lazy { load("TemplateAzureFunc.svg") }
    }

    @Suppress("unused")
    object ResourceManagement {
        val ResourceManagement by lazy { load("AzureARM.svg") }
        val ResourceGroup by lazy { load("AzureARMResourceGroup.svg") }
        val Deployment by lazy { load("AzureARMDeployment.svg") }
    }

    private fun load(path: String): Icon = IconLoader.getIcon("/icons/$path")
}