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

package com.microsoft.intellij.helpers.functionapp

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.microsoft.azure.auth.exception.AzureLoginFailureException
import com.microsoft.intellij.helpers.UIHelperImpl
import com.microsoft.intellij.helpers.base.AppBasePropertyViewProvider
import java.lang.IllegalStateException

class FunctionAppPropertyViewProvider : AppBasePropertyViewProvider() {

    companion object {
        const val TYPE = "FUNCTION_APP_PROPERTY"
    }

    override fun getType() = TYPE

    override fun createEditor(project: Project, virtualFile: VirtualFile): FileEditor {
        val subscriptionId = virtualFile.getUserData(UIHelperImpl.SUBSCRIPTION_ID)
                ?: throw IllegalStateException("Unable to get SubscriptionID from User Data.")

        val appId = virtualFile.getUserData(UIHelperImpl.RESOURCE_ID)
                ?: throw IllegalStateException("Unable to get ResourceID from User Data.")

        return FunctionAppPropertyView.create(project, subscriptionId, appId)
    }
}