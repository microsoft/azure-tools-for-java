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

package com.microsoft.intellij.runner.webapp.webappconfig

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.intellij.runner.AzureRunConfigurationBase
import com.microsoft.intellij.runner.webapp.model.DotNetWebAppSettingModel
import com.microsoft.intellij.runner.webapp.webappconfig.validator.ConfigurationValidator
import com.microsoft.intellij.runner.webapp.webappconfig.validator.ProjectConfigValidator
import com.microsoft.intellij.runner.webapp.webappconfig.validator.SqlDatabaseConfigValidator
import com.microsoft.intellij.runner.webapp.webappconfig.validator.WebAppConfigValidator

class RiderWebAppConfiguration(project: Project, factory: ConfigurationFactory, name: String?) :
        AzureRunConfigurationBase<DotNetWebAppSettingModel>(project, factory, name) {

    private val myModel = DotNetWebAppSettingModel()

    override fun getSubscriptionId(): String {
        return myModel.webAppModel.subscription?.subscriptionId() ?: ""
    }

    override fun getTargetPath(): String {
        return myModel.webAppModel.publishableProject?.projectFilePath ?: ""
    }

    override fun getTargetName(): String {
        return myModel.webAppModel.publishableProject?.projectName ?: ""
    }

    override fun getModel(): DotNetWebAppSettingModel {
        return myModel
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return RiderWebAppSettingEditor(project, this)
    }

    @Throws(ExecutionException::class)
    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState? {
        return RiderWebAppRunState(project, myModel)
    }

    override fun validate() { }

    /**
     * Validate the configuration to run
     *
     * @throws [RuntimeConfigurationError] when configuration miss expected fields
     */
    @Throws(RuntimeConfigurationError::class)
    override fun checkConfiguration() {
        ConfigurationValidator().validateAzureAccountIsSignedIn()

        ProjectConfigValidator.validateProject(myModel.webAppModel.publishableProject)
        WebAppConfigValidator.validateWebApp(myModel.webAppModel)
        SqlDatabaseConfigValidator.validateDatabaseConnection(myModel.databaseModel)

        if (myModel.databaseModel.isDatabaseConnectionEnabled) {
            WebAppConfigValidator.checkConnectionStringNameExistence(
                    myModel.databaseModel.connectionStringName, myModel.webAppModel.webAppId)
        }
    }
}
