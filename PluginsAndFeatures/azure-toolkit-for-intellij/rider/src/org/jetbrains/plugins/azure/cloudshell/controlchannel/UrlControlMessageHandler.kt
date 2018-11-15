/**
 * Copyright (c) 2018 JetBrains s.r.o.
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
package org.jetbrains.plugins.azure.cloudshell.controlchannel

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.azure.cloudshell.AzureCloudShellNotifications
import org.jetbrains.plugins.azure.util.WaitForUrl

class UrlControlMessageHandler(
        private val gson: Gson,
        private val project: Project)
    : ControlMessageHandler {

    companion object {
        private val logger = Logger.getInstance(UrlControlMessageHandler::class.java)

        private const val waitingText = "Waiting for cloud shell URL to become available..."
    }

    override fun handle(jsonControlMessage: String) {
        val message = gson.fromJson(jsonControlMessage, UrlControlMessage::class.java)

        if (!message.url.isEmpty()) {
            logger.info("Waiting for success status code for URL: {message.url}")

            ProgressManager.getInstance().run(object : Task.Backgroundable(project, waitingText, true, PerformInBackgroundOption.DEAF) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        WaitForUrl(message.url).withRetries(30,
                                { attempt, of -> indicator.text = "$waitingText (attempt ${attempt + 1}/$of)" },
                                { it.code() in 200..400 })

                        logger.info("Opening browser for URL: {message.url}")
                        BrowserUtil.browse(message.url)
                    } catch (e: Exception) {
                        logger.error("Opening browser failed for URL: {message.url}", e)

                        AzureCloudShellNotifications.notify(project,
                                "Failed to browse cloud shell URL",
                                "An error occurred while browsing cloud shell URL",
                                "Error browsing {message.url}: " + e.message,
                                NotificationType.ERROR)
                    }
                }
            })
        }
    }

    private data class UrlControlMessage(
            @SerializedName("audience")
            val audience : String,

            @SerializedName("url")
            val url : String
    )
}