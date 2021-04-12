/**
 * Copyright (c) 2021 JetBrains s.r.o.
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

package com.microsoft.intellij.serviceexplorer.azure.database.actions

import com.intellij.openapi.project.Project
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.intellij.AzurePlugin
import com.microsoft.intellij.actions.AzureSignInAction
import com.microsoft.intellij.ui.messages.AzureBundle
import com.microsoft.intellij.util.AzureLoginHelper
import com.microsoft.tooling.msservices.components.DefaultLoader

fun runWhenSignedIn(project: Project, action: (Unit) -> Unit) {
    AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)
            .subscribe { isSuccess: Boolean? ->
                run {
                    try {
                        if (isSuccess != true ||
                                !AzureLoginHelper.isAzureSubsAvailableOrReportError(AzureBundle.message("common.error.signIn"))) {
                            return@run
                        }
                    } catch (ex: Exception) {
                        AzurePlugin.log(AzureBundle.message("common.error.signIn"), ex)
                        DefaultLoader.getUIHelper().showException(AzureBundle.message("common.error.signIn"), ex, AzureBundle.message("common.error.signIn"), false, true)
                    }

                    action.invoke(Unit)
                }
            }
}