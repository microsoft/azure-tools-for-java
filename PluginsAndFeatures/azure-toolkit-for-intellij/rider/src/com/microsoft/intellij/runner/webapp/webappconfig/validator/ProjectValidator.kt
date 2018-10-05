package com.microsoft.intellij.runner.webapp.webappconfig.validator

import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.util.SystemInfo
import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.intellij.runner.webapp.webappconfig.UiConstants
import java.io.File

object ProjectValidator : ConfigurationValidator() {

    private const val PROJECT_TARGETS_NOT_DEFINED = "Selected project '%s' cannot be published. Please choose a Web App"
    private const val PROJECT_PUBLISHING_NOT_SUPPORTED = "Publishing .Net applications on %s is not yet supported"

    /**
     * Validate publishable project in the config
     *
     * Note: for .NET web apps we ned to check for the "WebApplication" targets
     *       that contains tasks for generating publishable package
     *
     * @throws [ConfigurationException] in case validation is failed
     */
    @Throws(RuntimeConfigurationError::class)
    fun validateProject(publishableProject: PublishableProjectModel?) {

        publishableProject ?: throw RuntimeConfigurationError(UiConstants.PROJECT_NOT_DEFINED)

        if (!isPublishingSupported(publishableProject))
            throw RuntimeConfigurationError(
                    String.format(PROJECT_PUBLISHING_NOT_SUPPORTED, SystemInfo.OS_NAME))

        if (!publishableProject.isDotNetCore && !isWebTargetsPresent(File(publishableProject.projectFilePath)))
            throw RuntimeConfigurationError(
                    String.format(PROJECT_TARGETS_NOT_DEFINED, UiConstants.WEB_APP_TARGET_NAME))
    }

    /**
     * Check whether necessary targets exists in a project that are necessary for web app deployment
     * Note: On Windows only
     *
     * TODO: We should replace this method with a target validation on a backend (RIDER-18500)
     *
     * @return [Boolean] whether WebApplication targets are present in publishable project
     */
    private fun isWebTargetsPresent(csprojFile: File): Boolean = csprojFile.readText().contains(UiConstants.WEB_APP_TARGET_NAME, true)

    private fun isPublishingSupported(publishableProject: PublishableProjectModel): Boolean {
        return publishableProject.isWeb && (publishableProject.isDotNetCore || SystemInfo.isWindows)
    }
}