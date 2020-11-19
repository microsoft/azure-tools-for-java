/**
 * Copyright (c) 2020 JetBrains s.r.o.
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

package com.microsoft.intellij.deploy
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.microsoft.azuretools.azurecommons.deploy.DeploymentEventArgs
import com.microsoft.intellij.AzurePlugin
import java.net.MalformedURLException
import java.net.URL
import java.util.*

class AzureDeploymentProgressNotification {

    /**
     * Unlike Eclipse plugin, here startDate is deployment start time, not the event timestamp
     */
    fun notifyProgress(deploymentId: String?,
                       startDate: Date?,
                       deploymentURL: String?,
                       progress: Int,
                       message: String?,
                       vararg args: Any?) {
        var deploymentURL = deploymentURL
        val arg = DeploymentEventArgs(this)
        arg.id = deploymentId
        if (deploymentURL != null) {
            try {
                URL(deploymentURL)
            } catch (e: MalformedURLException) {
                deploymentURL = null
            }
        }
        arg.deploymentURL = deploymentURL
        arg.deployMessage = String.format(message!!, *args)
        arg.deployCompleteness = progress
        arg.startTime = startDate
        AzurePlugin.fireDeploymentEvent(arg)
    }
}
