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

package com.microsoft.intellij.helpers.deploy

import com.jetbrains.rider.util.idea.getLogger
import com.microsoft.azure.management.appservice.PublishingProfile
import com.microsoft.azure.management.appservice.WebAppBase
import com.microsoft.intellij.helpers.UiConstants
import com.microsoft.intellij.runner.RunProcessHandler
import com.microsoft.intellij.runner.utils.AppDeploySession
import com.microsoft.intellij.runner.webapp.config.runstate.WebAppDeployStateUtil
import okhttp3.Response
import java.io.File

object KuduClient {

    private val logger = getLogger<WebAppDeployStateUtil>()

    private const val URL_KUDU_ZIP_DEPLOY_SUFFIX = "/api/zipdeploy"
    private const val URL_AZURE_BASE = ".azurewebsites.net"
    private const val URL_KUDU_BASE = ".scm$URL_AZURE_BASE"
    private const val URL_KUDU_ZIP_DEPLOY = "$URL_KUDU_BASE$URL_KUDU_ZIP_DEPLOY_SUFFIX"

    private const val SLEEP_TIME_MS = 5000L
    private const val DEPLOY_TIMEOUT_MS = 180000L
    private const val UPLOADING_MAX_TRY = 3

    /**
     * Method to publish specified ZIP file to Azure server. We make up to 3 tries for uploading a ZIP file.
     *
     * Note: Azure SDK supports a native [FunctionApp.zipDeploy(File)] method. Hoverer, we cannot use it for files with BOM
     *       Method throws an exception while reading the JSON file that contains BOM. Use our own implementation until fixed
     *
     * @param zipFile - zip file instance to be published
     * @param processHandler - a process handler to show a process message
     *
     * @throws [RuntimeException] in case REST request was not succeed or timed out after 3 attempts
     */
    fun kuduZipDeploy(zipFile: File, app: WebAppBase, processHandler: RunProcessHandler) {

        val appName = app.name()
        val kuduBaseUrl = "https://" + app.defaultHostName().toLowerCase()
                .replace("http://", "")
                .replace(app.name().toLowerCase(), app.name().toLowerCase() + ".scm")
                .trimEnd('/') + URL_KUDU_ZIP_DEPLOY_SUFFIX

        kuduZipDeploy(zipFile, app.publishingProfile, appName, kuduBaseUrl, processHandler)
    }

    /**
     * Method to publish specified ZIP file to Azure server. We make up to 3 tries for uploading a ZIP file.
     *
     * Note: Azure SDK supports a native [FunctionApp.zipDeploy(File)] method. Hoverer, we cannot use it for files with BOM
     *       Method throws an exception while reading the JSON file that contains BOM. Use our own implementation until fixed
     *
     * @param zipFile - zip file instance to be published
     * @param processHandler - a process handler to show a process message
     *
     * @throws [RuntimeException] in case REST request was not succeed or timed out after 3 attempts
     */
    fun kuduZipDeploy(zipFile: File, publishingProfile: PublishingProfile, appName: String, kuduBaseUrl: String?, processHandler: RunProcessHandler) {

        val session = AppDeploySession(publishingProfile.gitUsername(), publishingProfile.gitPassword())

        var success = false
        var uploadCount = 0
        var response: Response? = null

        try {
            do {
                processHandler.setText(String.format(UiConstants.ZIP_DEPLOY_START_PUBLISHING, uploadCount + 1, UPLOADING_MAX_TRY))

                try {
                    response = session.publishZip(
                            kuduBaseUrl ?: "https://" + appName.toLowerCase() + URL_KUDU_ZIP_DEPLOY,
                            zipFile,
                            DEPLOY_TIMEOUT_MS)
                    success = response.isSuccessful
                } catch (e: Throwable) {
                    processHandler.setText("${UiConstants.ZIP_DEPLOY_PUBLISH_FAIL}: $e")
                }

            } while (!success && ++uploadCount < UPLOADING_MAX_TRY && isWaitFinished())

            if (response == null || !success) {
                val message = "${UiConstants.ZIP_DEPLOY_PUBLISH_FAIL}: Response code: ${response?.code()}. Response message: ${response?.message()}"
                processHandler.setText(message)
                throw RuntimeException(message)
            }

            val message = UiConstants.ZIP_DEPLOY_PUBLISH_SUCCESS
            processHandler.setText(message)

        } finally {
            response?.body()?.close()
        }
    }

    /**
     * Sleep for [timeout] ms
     */
    private fun isWaitFinished(timeout: Long = SLEEP_TIME_MS): Boolean {
        try {
            Thread.sleep(timeout)
        } catch (e: InterruptedException) {
            logger.warn(e)
        }

        return true
    }

}