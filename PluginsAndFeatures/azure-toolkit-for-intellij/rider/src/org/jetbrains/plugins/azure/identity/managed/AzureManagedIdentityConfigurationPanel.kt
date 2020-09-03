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

package org.jetbrains.plugins.azure.identity.managed

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBList
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.applyToComponent
import com.intellij.ui.layout.panel
import com.intellij.util.ui.UIUtil
import com.microsoft.azuretools.authmanage.AuthMethod
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction
import com.microsoft.intellij.configuration.ui.AzureRiderAbstractConfigurablePanel
import net.miginfocom.swing.MigLayout
import org.jetbrains.plugins.azure.RiderAzureBundle
import java.io.IOException

@Suppress("UNUSED_LAMBDA_EXPRESSION")
class AzureManagedIdentityConfigurationPanel(private val project: Project) : AzureRiderAbstractConfigurablePanel {

    private val logger = Logger.getInstance(AzureRiderAbstractConfigurablePanel::class.java)

    private fun createPanel(): DialogPanel {
        lateinit var dialogPanel: DialogPanel
        dialogPanel = panel {
            noteRow(RiderAzureBundle.message("settings.managedidentity.description"))
            row {
                link(RiderAzureBundle.message("settings.managedidentity.info.title")) {
                    BrowserUtil.open(RiderAzureBundle.message("settings.managedidentity.info.url"))
                }
            }

            // When not signed in, show sign in button
            lateinit var signInRow: Row
            signInRow = row {
                button(RiderAzureBundle.message("settings.managedidentity.sign_in")) {
                    try {
                        AuthMethodManager.getInstance().signOut()
                        if (AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) {
                            dialogPanel.reset()
                        }
                    } catch (e: Exception) {
                        logger.error("Error while signing in", e)
                    }
                }
            }.onGlobalReset {
                signInRow.visible = !isSignedIn()
            }

            // When signed in, show sign out button
            lateinit var signOutRow: Row
            signOutRow = row {
                button(RiderAzureBundle.message("settings.managedidentity.sign_out")) {
                    try {
                        AuthMethodManager.getInstance().signOut()
                        dialogPanel.reset()
                    } catch (e: Exception) {
                        logger.error("Error while signing out", e)
                    }
                }
            }.onGlobalReset {
                signOutRow.visible = isSignedIn()
            }

            // Shown when not signed in with Azure Cli
            lateinit var rowNotSignedInWithAzureCli: Row
            rowNotSignedInWithAzureCli = row {
                label(RiderAzureBundle.message("settings.managedidentity.not_signed_in_with_cli")).applyToComponent {
                    icon = AllIcons.General.Warning
                }
            }.onGlobalReset { rowNotSignedInWithAzureCli.visible = !isSignedInWithAzureCli() }

            // Shown when signed in with Azure Cli
            lateinit var rowSignedInWithAzureCli: Row
            lateinit var subscriptionsList: JBList<SubscriptionDetail>
            rowSignedInWithAzureCli = row {
                subRowIndent = 0

                row {
                    label(RiderAzureBundle.message("settings.managedidentity.signed_in_with_cli")).applyToComponent {
                        icon = AllIcons.General.InspectionsOK
                    }
                }

                row {
                    placeholder()
                }

                row {
                    label(RiderAzureBundle.message("settings.managedidentity.signed_in_with_cli.accessible_subscriptions"))
                }

                row {
                    subscriptionsList = JBList(emptyList<SubscriptionDetail>()).apply {
                        cellRenderer = SubscriptionDetailRenderer()
                        setEmptyText(RiderAzureBundle.message("settings.managedidentity.signed_in_with_cli.accessible_subscriptions"))
                    }
                    scrollPane(subscriptionsList)
                }
            }.onGlobalReset {
                rowSignedInWithAzureCli.subRowsVisible = isSignedInWithAzureCli()
                ApplicationManager.getApplication().executeOnPooledThread {
                    val subscriptionDetails = try {
                        AuthMethodManager.getInstance()
                                ?.azureManager?.subscriptionManager?.subscriptionDetails
                    } catch (e: IOException) {
                        logger.error("Error while retrieving subscription details", e)
                        null
                    }

                    UIUtil.invokeAndWaitIfNeeded(Runnable {
                        if (isSignedInWithAzureCli()) {
                            subscriptionsList.setListData(subscriptionDetails?.toTypedArray()
                                    ?: emptyArray<SubscriptionDetail>())
                        }
                    })
                }
            }

            row {
                placeholder().constraints(growY, pushY)
            }
        }
        dialogPanel.layout = MigLayout("novisualpadding, ins 0, fillx, wrap 1, hidemode 3")
        return dialogPanel
    }

    private fun isSignedIn() = try {
        AuthMethodManager.getInstance().isSignedIn
    } catch (e: Exception) {
        false
    }

    private fun isSignedInWithAzureCli() = try {
        val authMethodManager = AuthMethodManager.getInstance()

        authMethodManager.isSignedIn
                && authMethodManager.authMethod == AuthMethod.AZ
    } catch (e: Exception) {
        false
    }

    override val panel = createPanel().apply {
        reset()
    }

    override val displayName: String = RiderAzureBundle.message("settings.managedidentity.name")

    override fun doOKAction() {
        panel.apply()
    }
}