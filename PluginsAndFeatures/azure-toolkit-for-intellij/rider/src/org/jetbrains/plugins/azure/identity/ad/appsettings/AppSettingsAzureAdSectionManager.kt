/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package org.jetbrains.plugins.azure.identity.ad.appsettings

import com.intellij.json.JsonUtil
import com.intellij.json.psi.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotifications
import org.jetbrains.plugins.azure.util.JsonParser.findStringProperty
import org.jetbrains.plugins.azure.util.JsonParser.jsonFileFromVirtualFile
import org.jetbrains.plugins.azure.util.JsonParser.writeProperty

class AppSettingsAzureAdSectionManager {

    fun readAzureAdSectionFrom(virtualFile: VirtualFile, project: Project): AppSettingsAzureAdSection? {
        val jsonFile = ApplicationManager.getApplication().runReadAction(Computable {
            jsonFileFromVirtualFile(virtualFile, project)
        }) ?: return null

        return readAzureAdSectionFrom(jsonFile)
    }

    fun readAzureAdSectionFrom(jsonFile: JsonFile): AppSettingsAzureAdSection? {
        val topLevelObject = JsonUtil.getTopLevelObject(jsonFile) ?: return null
        val azureAdObject = topLevelObject.findProperty("AzureAd")?.value as JsonObject? ?: return null

        return AppSettingsAzureAdSection(
                findStringProperty(azureAdObject, "Instance"),
                findStringProperty(azureAdObject, "Domain"),
                findStringProperty(azureAdObject, "TenantId"),
                findStringProperty(azureAdObject, "ClientId"),
                findStringProperty(azureAdObject, "CallbackPath")
        )
    }

    fun writeAzureAdSectionTo(azureAdSection: AppSettingsAzureAdSection, virtualFile: VirtualFile, project: Project, openInEditor: Boolean) {
        val jsonFile = ApplicationManager.getApplication().runReadAction(Computable {
            jsonFileFromVirtualFile(virtualFile, project)
        })  ?: return

        val topLevelObject = JsonUtil.getTopLevelObject(jsonFile) ?: return

        WriteCommandAction.runWriteCommandAction(project, (Computable {
            val azureAdObject = topLevelObject.findProperty("AzureAd")?.value as JsonObject?
                    ?: writeProperty(project, topLevelObject, "AzureAd", "{}", false)?.value as JsonObject?
                    ?: return@Computable

            writeProperty(project, azureAdObject, "Instance", azureAdSection.instance ?: "https://login.microsoftonline.com/")
            writeProperty(project, azureAdObject, "Domain", azureAdSection.domain ?: "qualified.domain.name")
            writeProperty(project, azureAdObject, "TenantId", azureAdSection.tenantId ?: "22222222-2222-2222-2222-222222222222")
            writeProperty(project, azureAdObject, "ClientId", azureAdSection.clientId ?: "11111111-1111-1111-11111111111111111")
            writeProperty(project, azureAdObject, "CallbackPath", azureAdSection.callbackPath ?: "/signin-oidc")

            ApplicationManager.getApplication().invokeLater {
                EditorNotifications.getInstance(project).updateNotifications(virtualFile)

                if (openInEditor) {
                    val fileManager = FileEditorManager.getInstance(project)
                    fileManager.openFile(virtualFile, true)
                    fileManager.selectedTextEditor?.caretModel?.moveToOffset(azureAdObject.textRange.endOffset)
                }
            }
        }))
    }
}
