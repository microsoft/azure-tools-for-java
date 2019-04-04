/**
 * Copyright (c) 2018-2019 JetBrains s.r.o.
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

package com.microsoft.intellij.runner.webapp.config

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.microsoft.intellij.runner.AzureRunConfigurationBase
import com.microsoft.intellij.runner.validator.ConfigurationValidator
import com.microsoft.intellij.runner.validator.ProjectConfigValidator
import com.microsoft.intellij.runner.validator.SqlDatabaseConfigValidator
import com.microsoft.intellij.runner.webapp.config.validator.WebAppConfigValidator
import com.microsoft.intellij.runner.webapp.model.DotNetWebAppSettingModel
import org.jdom.Element

class RiderWebAppConfiguration(project: Project, factory: ConfigurationFactory, name: String?) :
        AzureRunConfigurationBase<DotNetWebAppSettingModel>(project, factory, name) {

    private val myModel = DotNetWebAppSettingModel()

    override fun getSubscriptionId() = myModel.webAppModel.subscription?.subscriptionId() ?: ""
    override fun getTargetPath() = myModel.webAppModel.publishableProject?.projectFilePath ?: ""
    override fun getTargetName() = myModel.webAppModel.publishableProject?.projectName ?: ""
    override fun getModel() = myModel

    override fun getConfigurationEditor() = RiderWebAppSettingEditor(project, this)

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState? {
        return RiderWebAppRunState(project, myModel)
    }

    override fun validate() { }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        myModel.webAppModel.readExternal(project, element)
        myModel.databaseModel.readExternal(element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        myModel.webAppModel.writeExternal(element)
        myModel.databaseModel.writeExternal(element)
    }

    /**
     * Validate the configuration to run
     *
     * @throws [RuntimeConfigurationError] when configuration miss expected fields
     */
    @Throws(RuntimeConfigurationError::class)
    override fun checkConfiguration() {

        ConfigurationValidator().validateAzureAccountIsSignedIn()

        ProjectConfigValidator.validateProjectForWebApp(myModel.webAppModel.publishableProject)
        WebAppConfigValidator.validateWebApp(myModel.webAppModel)
        SqlDatabaseConfigValidator.validateDatabaseConnection(myModel.databaseModel)

        if (myModel.databaseModel.isDatabaseConnectionEnabled) {
            WebAppConfigValidator.checkConnectionStringNameExistence(
                    myModel.databaseModel.connectionStringName, myModel.webAppModel.appId)
        }
    }
}
