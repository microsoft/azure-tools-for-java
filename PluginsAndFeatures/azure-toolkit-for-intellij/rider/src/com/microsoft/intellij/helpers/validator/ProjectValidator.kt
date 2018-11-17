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

package com.microsoft.intellij.helpers.validator

import com.intellij.openapi.util.SystemInfo
import com.jetbrains.rd.framework.impl.RpcTimeouts
import com.jetbrains.rider.model.PublishableProjectModel

object ProjectValidator : AzureResourceValidator() {

    private const val PROJECT_NOT_DEFINED = "Project is not defined"

    private const val PROJECT_PUBLISHING_NOT_SUPPORTED =
            "Selected project '%s' cannot be published. Please select a Web App"

    private const val PROJECT_PUBLISHING_OS_NOT_SUPPORTED =
            "Selected project '%s' cannot be published. Publishing .Net Web Apps is not yet supported on '%s'"

    private const val PROJECT_TARGETS_NOT_DEFINED =
            "Selected project '%s' cannot be published. Required target '%s' was not found."

    private const val WEB_APP_TARGET_NAME = "Microsoft.WebApplication.targets"

    private val timeouts = RpcTimeouts(500L, 2000L)

    /**
     * Validate publishable project in the config
     *
     * Note: for .NET web apps we ned to check for the "WebApplication" targets
     *       that contains tasks for generating publishable package
     */
    fun validateProject(publishableProject: PublishableProjectModel?): ValidationResult {

        val status = ValidationResult()

        publishableProject ?: return status.setInvalid(PROJECT_NOT_DEFINED)

        if (!publishableProject.isWeb)
            return status.setInvalid(
                    String.format(PROJECT_PUBLISHING_NOT_SUPPORTED, publishableProject.projectName))

        if (!isPublishSupported(publishableProject))
            return status.setInvalid(
                    String.format(PROJECT_PUBLISHING_OS_NOT_SUPPORTED, publishableProject.projectName, SystemInfo.OS_NAME))

        if (!publishableProject.isDotNetCore && !isWebTargetsPresent(publishableProject))
            return status.setInvalid(
                    String.format(PROJECT_TARGETS_NOT_DEFINED, publishableProject.projectName, WEB_APP_TARGET_NAME))

        return status
    }

    private fun isPublishSupported(publishableProject: PublishableProjectModel) =
            publishableProject.isWeb && (publishableProject.isDotNetCore || SystemInfo.isWindows)

    /**
     * Check whether necessary targets exists in a project that are necessary for web app deployment
     * Note: On Windows only
     *
     * @return [Boolean] whether WebApplication targets are present in publishable project
     */
    private fun isWebTargetsPresent(publishableProject: PublishableProjectModel) =
            publishableProject.hasTarget.sync(WEB_APP_TARGET_NAME, timeouts)
}
