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

package org.jetbrains.plugins.azure.identity.ad.actions

import com.intellij.ide.BrowserUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.rdclient.editors.FrontendTextControlHost
import com.jetbrains.rider.model.EditableEntityModelId
import com.jetbrains.rider.projectView.ProjectModelViewHost
import com.jetbrains.rider.projectView.RiderProjectDataRule
import com.jetbrains.rider.projectView.nodes.ProjectModelNode
import com.jetbrains.rider.projectView.nodes.containingProject
import com.jetbrains.rider.util.idea.PsiFile
import com.microsoft.azure.management.graphrbac.GraphErrorException
import com.microsoft.azure.management.graphrbac.implementation.ApplicationCreateParametersInner
import com.microsoft.azure.management.graphrbac.implementation.ApplicationInner
import com.microsoft.azure.management.graphrbac.implementation.ApplicationUpdateParametersInner
import com.microsoft.azure.management.graphrbac.implementation.GraphRbacManagementClientImpl
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.authmanage.RefreshableTokenCredentials
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction
import com.microsoft.azuretools.sdkmanage.AzureManager
import org.jetbrains.plugins.azure.AzureNotifications
import org.jetbrains.plugins.azure.RiderAzureBundle
import org.jetbrains.plugins.azure.identity.ad.appsettings.AppSettingsAzureAdSection
import org.jetbrains.plugins.azure.identity.ad.appsettings.AppSettingsAzureAdSectionManager
import org.jetbrains.plugins.azure.identity.ad.ui.RegisterApplicationInAzureAdDialog
import org.jetbrains.plugins.azure.isValidGuid
import java.net.URI
import java.util.*
import javax.swing.event.HyperlinkEvent

class RegisterApplicationInAzureAdAction
    : AnAction(
        RiderAzureBundle.message("action.identity.ad.register_app.name"),
        RiderAzureBundle.message("action.identity.ad.register_app.description"),
        null) {

    companion object {

        private const val appSettingsJsonFileName = "appsettings.json"

        fun isAppSettingsJsonFileName(fileName: String?): Boolean {
            if (fileName == null) return false

            return fileName.equals(appSettingsJsonFileName, true)
                    || (fileName.startsWith("appsettings.", true) && fileName.endsWith(".json", true))
        }

        private val logger = Logger.getInstance(RegisterApplicationInAzureAdAction::class.java)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return

        val projectModelNode = tryGetProjectModelNodeFromFile(project, e.dataContext.PsiFile?.virtualFile)
                ?: e.dataContext.getData(RiderProjectDataRule.RD_PROJECT_MODEL_NODE)
                ?: tryGetProjectModelNodeFromLastFocusedTextControl(project)

        e.presentation.isEnabledAndVisible = projectModelNode != null &&
                tryGetAppSettingsJsonVirtualFile(projectModelNode) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        if (!AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) return

        val projectModelNode = tryGetProjectModelNodeFromFile(project, e.dataContext.PsiFile?.virtualFile)
                ?: e.dataContext.getData(RiderProjectDataRule.RD_PROJECT_MODEL_NODE)
                ?: tryGetProjectModelNodeFromLastFocusedTextControl(project)
                ?: return

        val appSettingsJsonVirtualFile = tryGetAppSettingsJsonVirtualFile(projectModelNode) ?: return
        if (appSettingsJsonVirtualFile.isDirectory || !appSettingsJsonVirtualFile.exists()) return

        val application = ApplicationManager.getApplication()
        val azureManager = AuthMethodManager.getInstance().azureManager ?: return

        // Read local settings
        val azureAdSettings = AppSettingsAzureAdSectionManager()
                .readAzureAdSectionFrom(appSettingsJsonVirtualFile, project)

        val matchingTenantIdFromAppSettings = if (azureAdSettings != null
                && !azureAdSettings.isDefaultProjectTemplateContent()
                && azureAdSettings.tenantId != null) azureAdSettings.tenantId
        else null

        // Retrieve matching subscription/tenant
        object : Task.Backgroundable(project, RiderAzureBundle.message("progress.common.start.retrieving_subscription"), true, PerformInBackgroundOption.DEAF) {
            override fun run(indicator: ProgressIndicator) {
                logger.debug("Retrieving Azure subscription details...")

                val selectedSubscriptions = azureManager.subscriptionManager.subscriptionDetails
                        .asSequence()
                        .filter { it.isSelected }
                        .toList()

                logger.debug("Retrieved ${selectedSubscriptions.count()} Azure subscriptions")

                // One subscription? Only one tenant? No popup needed
                val bestMatchingSubscription = when {
                    matchingTenantIdFromAppSettings != null -> selectedSubscriptions.firstOrNull { it.tenantId == matchingTenantIdFromAppSettings }
                    selectedSubscriptions.count() == 1 -> selectedSubscriptions.first()
                    else -> null
                }
                if (bestMatchingSubscription != null) {
                    application.invokeLater {
                        if (project.isDisposed) return@invokeLater

                        fetchDataAndShowDialog(projectModelNode, project, azureManager, bestMatchingSubscription)
                    }
                    return
                }

                // Multiple subscriptions? Popup.
                application.invokeLater {
                    if (project.isDisposed) return@invokeLater

                    val step = object : BaseListPopupStep<SubscriptionDetail>(RiderAzureBundle.message("popup.common.start.select_subscription"), selectedSubscriptions) {
                        override fun getTextFor(value: SubscriptionDetail?): String {
                            if (value != null) {
                                return "${value.subscriptionName} (${value.subscriptionId})"
                            }

                            return super.getTextFor(value)
                        }

                        override fun onChosen(selectedValue: SubscriptionDetail, finalChoice: Boolean): PopupStep<*>? {
                            doFinalStep {
                                fetchDataAndShowDialog(projectModelNode, project, azureManager, selectedValue)
                            }
                            return PopupStep.FINAL_CHOICE
                        }
                    }

                    logger.debug("Showing popup to select Azure subscription")

                    val popup = JBPopupFactory.getInstance().createListPopup(step)
                    popup.showCenteredInCurrentWindow(project)
                }
            }
        }.queue()
    }

    private fun fetchDataAndShowDialog(projectModelNode: ProjectModelNode,
                                       project: Project,
                                       azureManager: AzureManager,
                                       selectedSubscription: SubscriptionDetail) {

        val appSettingsJsonVirtualFile = tryGetAppSettingsJsonVirtualFile(projectModelNode) ?: return
        if (appSettingsJsonVirtualFile.isDirectory || !appSettingsJsonVirtualFile.exists()) return

        val application = ApplicationManager.getApplication()

        // Create graph client
        logger.debug("Using subscription ${selectedSubscription.subscriptionId}; tenant ${selectedSubscription.tenantId}")
        val tokenCredentials = RefreshableTokenCredentials(azureManager, selectedSubscription.tenantId)

        val graphClient = GraphRbacManagementClientImpl(
                azureManager.environment.azureEnvironment.graphEndpoint(), tokenCredentials).withTenantID(selectedSubscription.tenantId)

        // Read local settings
        val azureAdSettings = AppSettingsAzureAdSectionManager()
                .readAzureAdSectionFrom(appSettingsJsonVirtualFile, project)

        // Build model
        val domain = try {
            defaultDomainForTenant(graphClient)
        } catch (e: GraphErrorException) {
            logger.error("Failed to get default domain for tenant", e)

            reportError(project,
                    RiderAzureBundle.message("notification.identity.ad.register_app.failed.common.subtitle"),
                    RiderAzureBundle.message("notification.identity.ad.register_app.failed.get_default_domain.message", selectedSubscription.tenantId))

            return
        }
        val model = if (azureAdSettings == null || azureAdSettings.isDefaultProjectTemplateContent()) {
            buildDefaultRegistrationModel(projectModelNode, domain)
        } else {
            buildRegistrationModelFrom(azureAdSettings, domain, graphClient, projectModelNode)
        }

        // Show dialog
        val dialog = RegisterApplicationInAzureAdDialog(project, model)
        if (dialog.showAndGet()) {
            object : Task.Backgroundable(project, RiderAzureBundle.message("progress.identity.ad.registering"), true, PerformInBackgroundOption.DEAF) {
                override fun run(indicator: ProgressIndicator) {
                    // 1. Save changes to AD
                    val existingApplication = if (model.updatedClientId.isNotEmpty()) {
                        tryGetRegisteredApplication(model.updatedClientId, graphClient)
                    } else if (azureAdSettings != null && !azureAdSettings.isDefaultProjectTemplateContent() && azureAdSettings.clientId != null) {
                        tryGetRegisteredApplication(azureAdSettings.clientId, graphClient)
                    } else null

                    if (indicator.isCanceled) return
                    logger.debug("Updating Azure AD application registration...")
                    val updatedApplication = if (existingApplication != null && model.allowOverwrite && model.hasChanges) {
                        // Update
                        var parameters = ApplicationUpdateParametersInner()

                        if (model.updatedDisplayName != model.originalDisplayName)
                            parameters = parameters.withDisplayName(model.updatedDisplayName)

                        if (model.updatedCallbackUrl != model.originalCallbackUrl) {
                            val replyUrls = existingApplication.replyUrls()
                            replyUrls.remove(model.originalCallbackUrl)
                            replyUrls.add(model.updatedCallbackUrl)

                            parameters = parameters.withReplyUrls(replyUrls)
                        }

                        if (model.updatedIsMultiTenant != model.originalIsMultiTenant)
                            parameters = parameters.withAvailableToOtherTenants(model.updatedIsMultiTenant)

                        try {
                            graphClient.applications().patch(existingApplication.objectId(), parameters)
                            graphClient.applications().get(existingApplication.objectId())
                        } catch (e: GraphErrorException) {
                            logger.error("Failed to update application", e)

                            reportError(project,
                                    RiderAzureBundle.message("notification.identity.ad.register_app.failed.common.subtitle"),
                                    RiderAzureBundle.message("notification.identity.ad.register_app.failed.update.message", selectedSubscription.tenantId, e.body().message()))

                            return
                        }
                    } else if (existingApplication == null) {
                        // Create
                        try {
                            graphClient.applications().create(ApplicationCreateParametersInner()
                                    .withDisplayName(model.updatedDisplayName)
                                    .withIdentifierUris(listOf("https://" + domain + "/" + (model.updatedDisplayName + UUID.randomUUID().toString().substring(0, 6)).filter { it.isLetterOrDigit() }))
                                    .withReplyUrls(listOf(model.updatedCallbackUrl))
                                    .withAvailableToOtherTenants(model.updatedIsMultiTenant))
                        } catch (e: GraphErrorException) {
                            logger.error("Failed to create application", e)

                            reportError(project,
                                    RiderAzureBundle.message("notification.identity.ad.register_app.failed.common.subtitle"),
                                    if (e.body().code() == "403") {
                                        RiderAzureBundle.message("notification.identity.ad.register_app.failed.create.permissions")
                                    } else {
                                        RiderAzureBundle.message("notification.identity.ad.register_app.failed.create.message", e.body().message())
                                    })

                            return
                        }
                    } else null

                    // 2. Save changes to appsettings.json
                    application.invokeLater {
                        logger.debug("Saving changes to appsettings.json...")
                        AppSettingsAzureAdSectionManager()
                                .writeAzureAdSectionTo(AppSettingsAzureAdSection(
                                        instance = azureAdSettings?.instance,
                                        domain = model.updatedDomain,
                                        tenantId = selectedSubscription.tenantId,
                                        clientId = updatedApplication?.appId() ?: existingApplication?.appId()
                                        ?: azureAdSettings?.clientId,
                                        callbackPath = URI.create(model.updatedCallbackUrl).rawPath
                                ), appSettingsJsonVirtualFile, project, model.hasChanges)
                    }
                }
            }.queue()
        }
    }

    private fun tryGetProjectModelNodeFromFile(project: Project, file: VirtualFile?): ProjectModelNode? {
        if (file == null) return null

        return ProjectModelViewHost.getInstance(project).getItemsByVirtualFile(file).firstOrNull()
    }

    private fun tryGetProjectModelNodeFromLastFocusedTextControl(project: Project): ProjectModelNode? {
        val documentId = FrontendTextControlHost.getInstance(project).lastFocusedTextControl.value?.id?.documentId
        if (documentId != null) {
            val modelId = documentId as? EditableEntityModelId
            val projectModelId = modelId?.projectModelElementId
            if (projectModelId != null) {
                return ProjectModelViewHost.getInstance(project).getItemById(projectModelId)
            }
        }
        return null
    }

    private fun tryGetAppSettingsJsonVirtualFile(item: ProjectModelNode): VirtualFile? {
        val itemVirtualFile = item.getVirtualFile()

        if (isAppSettingsJsonFileName(itemVirtualFile?.name)) return itemVirtualFile

        return item.containingProject()?.getVirtualFile()?.parent?.findChild(appSettingsJsonFileName)
    }

    private fun defaultDomainForTenant(graphClient: GraphRbacManagementClientImpl) =
            graphClient.domains().list()
                    .filter { it.isDefault }
                    .map { it.name() }
                    .firstOrNull() ?: ""

    private fun tryGetRegisteredApplication(clientId: String?, graphClient: GraphRbacManagementClientImpl): ApplicationInner? {
        if (clientId == null || !clientId.isValidGuid()) return null

        try {
            val matchingApplication = graphClient.applications()
                    .list("appId eq '$clientId'")
                    .firstOrNull()
            if (matchingApplication != null) {
                return graphClient.applications().get(matchingApplication.objectId())
            }
        } catch (e: Throwable) {
            logger.error(e)
        }

        return null
    }

    private fun buildDefaultRegistrationModel(projectModelNode: ProjectModelNode, domain: String) =
            RegisterApplicationInAzureAdDialog.RegistrationModel(
                    originalDisplayName = projectModelNode.containingProject()!!.name,
                    originalClientId = "",
                    originalDomain = domain,
                    originalCallbackUrl = "https://localhost:5001/signin-oidc", // IMPROVEMENT: can we get the URL from the current project?
                    originalIsMultiTenant = false,
                    allowOverwrite = false
            )

    private fun buildRegistrationModelFrom(azureAdSettings: AppSettingsAzureAdSection,
                                           domain: String,
                                           graphClient: GraphRbacManagementClientImpl,
                                           projectModelNode: ProjectModelNode): RegisterApplicationInAzureAdDialog.RegistrationModel {

        // 1. If an application exists, use its data.
        val application = tryGetRegisteredApplication(azureAdSettings.clientId, graphClient)
        if (application != null) {
            val replyUrls = application.replyUrls()
                    .filter { azureAdSettings.callbackPath != null && it.endsWith(azureAdSettings.callbackPath) }

            return RegisterApplicationInAzureAdDialog.RegistrationModel(
                    originalDisplayName = application.displayName(),
                    originalClientId = application.appId(),
                    originalDomain = azureAdSettings.domain ?: domain,
                    originalCallbackUrl = replyUrls.firstOrNull()
                            ?: "https://localhost:5001/" + (azureAdSettings.callbackPath?.trimStart('/') ?: "signin-oidc"),
                    originalIsMultiTenant = application.availableToOtherTenants(),
                    allowOverwrite = false
            )
        }

        // 2. If no application exists, use whatever we can recover from appsettings.json
        return RegisterApplicationInAzureAdDialog.RegistrationModel(
                originalDisplayName = projectModelNode.containingProject()!!.name,
                originalClientId = azureAdSettings.clientId ?: "",
                originalDomain = azureAdSettings.domain ?: domain,
                originalCallbackUrl = "https://localhost:5001/" + (azureAdSettings.callbackPath?.trimStart('/') ?: "signin-oidc"),
                originalIsMultiTenant = false,
                allowOverwrite = false
        )
    }

    private fun reportError(project: Project, subtitle: String, message: String) {
        AzureNotifications.notify(project,
                RiderAzureBundle.message("common.azure"),
                subtitle,
                message,
                NotificationType.ERROR,
                object : NotificationListener.Adapter() {
                    override fun hyperlinkActivated(notification: Notification, e: HyperlinkEvent) {
                        if (!project.isDisposed) {
                            when (e.description) {
                                "permissions" -> BrowserUtil.browse("https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-how-applications-are-added#who-has-permission-to-add-applications-to-my-azure-ad-instance")
                            }
                        }
                    }
                })
    }
}