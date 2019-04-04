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

package com.microsoft.intellij.runner.functionapp.config

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.microsoft.intellij.runner.AzureRunConfigurationBase
import com.microsoft.intellij.runner.functionapp.config.validator.FunctionAppConfigValidator
import com.microsoft.intellij.runner.functionapp.model.FunctionAppSettingModel
import com.microsoft.intellij.runner.validator.ConfigurationValidator
import com.microsoft.intellij.runner.validator.ProjectConfigValidator
import com.microsoft.intellij.runner.validator.SqlDatabaseConfigValidator
import org.jdom.Element

class FunctionAppConfiguration(project: Project, factory: ConfigurationFactory, name: String?) :
        AzureRunConfigurationBase<FunctionAppSettingModel>(project, factory, name) {

    private val myModel = FunctionAppSettingModel()

    override fun getSubscriptionId() = myModel.functionAppModel.subscription?.subscriptionId() ?: ""
    override fun getTargetPath() = myModel.functionAppModel.publishableProject?.projectFilePath ?: ""
    override fun getTargetName() = myModel.functionAppModel.publishableProject?.projectName ?: ""
    override fun getModel() = myModel

    override fun getConfigurationEditor() = FunctionAppSettingEditor(project, this)
    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment) = FunctionAppRunState(project, myModel)

    override fun validate() { }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        myModel.functionAppModel.readExternal(project, element)
        myModel.databaseModel.readExternal(element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        myModel.functionAppModel.writeExternal(element)
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

        ProjectConfigValidator.validateProjectForFunctionApp(myModel.functionAppModel.publishableProject)
        FunctionAppConfigValidator.validateFunctionApp(myModel.functionAppModel)
        SqlDatabaseConfigValidator.validateDatabaseConnection(myModel.databaseModel)

        if (myModel.databaseModel.isDatabaseConnectionEnabled) {
            FunctionAppConfigValidator.checkConnectionStringNameExistence(
                    subscriptionId = myModel.functionAppModel.subscription?.subscriptionId() ?: "",
                    appId = myModel.functionAppModel.appId,
                    connectionStringName = myModel.databaseModel.connectionStringName)
        }
    }
}