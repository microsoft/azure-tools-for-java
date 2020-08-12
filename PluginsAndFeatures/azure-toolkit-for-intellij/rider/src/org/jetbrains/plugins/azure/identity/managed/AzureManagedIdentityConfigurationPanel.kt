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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.panel
import com.microsoft.azuretools.authmanage.AuthMethod
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.ijidea.actions.AzureSignInAction
import com.microsoft.intellij.configuration.ui.AzureRiderAbstractConfigurablePanel
import org.jetbrains.plugins.azure.RiderAzureBundle

@Suppress("UNUSED_LAMBDA_EXPRESSION")
class AzureManagedIdentityConfigurationPanel(private val project: Project) : AzureRiderAbstractConfigurablePanel {

    private val logger = Logger.getInstance(AzureRiderAbstractConfigurablePanel::class.java)

    private fun createPanel(): DialogPanel =
            panel {
                noteRow(RiderAzureBundle.message("settings.managedidentity.description"))
                row {
                    link(RiderAzureBundle.message("settings.managedidentity.info.title")) {
                        BrowserUtil.open(RiderAzureBundle.message("settings.managedidentity.info.url"))
                    }
                }

                val rowConfiguredCorrectly = row {
                    label(RiderAzureBundle.message("settings.managedidentity.signed_in_with_cli")).apply {
                        component.icon = AllIcons.General.InspectionsOK
                    }
                }

                lateinit var rowRequiresSignIn: Row
                rowRequiresSignIn = row {
                    subRowIndent = 0

                    row {
                        button(RiderAzureBundle.message("settings.managedidentity.sign_in_with_cli")) {
                            try {
                                AuthMethodManager.getInstance().signOut()
                                if (AzureSignInAction.doSignIn(AuthMethodManager.getInstance(), project)) {
                                    rowConfiguredCorrectly.visible = true
                                    rowRequiresSignIn.subRowsVisible = false
                                }
                            } catch (e: Exception) {
                                logger.error("Error while signing in with Azure CLI", e)
                            }
                        }
                    }

                    row {
                        label(RiderAzureBundle.message("settings.managedidentity.not_signed_in_with_cli")).apply {
                            component.icon = AllIcons.General.Warning
                        }
                    }
                }

                // Initial state
                if (isSignedInWithAzureCli()) {
                    rowConfiguredCorrectly.visible = true
                    rowRequiresSignIn.subRowsVisible = false
                } else {
                    rowConfiguredCorrectly.visible = false
                    rowRequiresSignIn.subRowsVisible = true
                }
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