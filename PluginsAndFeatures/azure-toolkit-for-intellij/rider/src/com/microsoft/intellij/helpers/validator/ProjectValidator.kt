/**
 * Copyright (c) 2018-2020 JetBrains s.r.o.
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

package com.microsoft.intellij.helpers.validator

import com.intellij.openapi.util.SystemInfo
import com.jetbrains.rider.model.PublishableProjectModel
import org.jetbrains.plugins.azure.RiderAzureBundle.message

object ProjectValidator : AzureResourceValidator() {

    private val projectNotDefinedMessage = message("run_config.publish.validation.project.not_defined")

    /**
     * Validate publishable project in the config
     *
     * Note: for .NET web apps we need to check for the "WebApplication" targets
     *       that contains tasks for generating publishable package
     */
    fun validateProjectForWebApp(publishableProject: PublishableProjectModel?): ValidationResult {
        val status = ValidationResult()
        publishableProject ?: return status.setInvalid(projectNotDefinedMessage)

        if (!publishableProject.isWeb)
            return status.setInvalid(
                    message("run_config.publish.validation.project.publish_not_supported", publishableProject.projectName))

        if (!isPublishToWebAppSupported(publishableProject))
            return status.setInvalid(
                    message("run_config.publish.validation.project.publish_os_not_supported", publishableProject.projectName, SystemInfo.OS_NAME))

        if (!publishableProject.isDotNetCore && !publishableProject.hasWebPublishTarget)
            return status.setInvalid(
                    message("run_config.publish.validation.project.target_not_defined", publishableProject.projectName))

        return status
    }

    fun validateProjectForFunctionApp(publishableProject: PublishableProjectModel?): ValidationResult {
        val status = ValidationResult()
        publishableProject ?: return status.setInvalid(projectNotDefinedMessage)

        if (!isPublishToFunctionAppSupported(publishableProject))
            return status.setInvalid(
                    message("run_config.publish.validation.project.not_function_app", publishableProject.projectName))

        return status
    }

    private fun isPublishToWebAppSupported(publishableProject: PublishableProjectModel) =
            publishableProject.isWeb && (publishableProject.isDotNetCore || SystemInfo.isWindows)

    private fun isPublishToFunctionAppSupported(publishableProject: PublishableProjectModel) =
            publishableProject.isAzureFunction
}
