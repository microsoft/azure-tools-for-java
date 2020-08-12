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

package org.jetbrains.plugins.azure.identity.ad.notifications

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.microsoft.intellij.configuration.AzureRiderSettings
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.identity.ad.actions.RegisterApplicationInAzureAdAction
import org.jetbrains.plugins.azure.identity.ad.appsettings.AppSettingsAzureAdSectionManager

class DefaultAzureAdApplicationRegistrationNotificationProvider : EditorNotifications.Provider<EditorNotificationPanel>() {

    private val KEY = Key.create<EditorNotificationPanel>("azure.defaultazureadappliationregistration")

    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
        if (PropertiesComponent.getInstance().getBoolean(AzureRiderSettings.DISMISS_NOTIFICATION_AZURE_AD_REGISTER)) return null

        if (!RegisterApplicationInAzureAdAction.isAppSettingsJsonFileName(file.name)) return null

        val azureAdSettings = AppSettingsAzureAdSectionManager().readAzureAdSectionFrom(file, project) ?: return null
        if (!azureAdSettings.isDefaultProjectTemplateContent()) return null

        val panel = EditorNotificationPanel()
                .text(RiderAzureBundle.message("notification.identity.ad.register_app.title"))

        val action = ActionManager.getInstance().getAction("AzureToolkit.AzureAd.RegisterApplication")
        if (action != null) {
            panel.createActionLabel(RiderAzureBundle.message("notification.identity.ad.register_app.action.register"), {
                ActionUtil.invokeAction(action, fileEditor.component, ActionPlaces.INTENTION_MENU, null, null)
            }, true)
        }

        panel.createActionLabel(RiderAzureBundle.message("notification.identity.ad.register_app.action.dismiss"), {
            dismissNotification(file, project)
        }, true)

        return panel
    }

    private fun dismissNotification(file: VirtualFile, project: Project) {
        PropertiesComponent.getInstance(project).setValue(AzureRiderSettings.DISMISS_NOTIFICATION_AZURE_AD_REGISTER, true)
        EditorNotifications.getInstance(project).updateNotifications(file)
    }
}