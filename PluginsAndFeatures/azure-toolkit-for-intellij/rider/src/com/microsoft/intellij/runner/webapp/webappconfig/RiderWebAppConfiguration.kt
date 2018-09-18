package com.microsoft.intellij.runner.webapp.webappconfig

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.publishableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azuretools.authmanage.AuthMethodManager
import com.microsoft.azuretools.utils.AzureModel
import com.microsoft.intellij.runner.AzureRunConfigurationBase
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppMvpModel
import com.microsoft.intellij.runner.webapp.AzureDotNetWebAppSettingModel
import com.microsoft.intellij.runner.webapp.webappconfig.validator.SqlDatabaseValidator
import com.microsoft.intellij.runner.webapp.webappconfig.validator.SqlDatabaseValidator.validateDatabaseConnection
import com.microsoft.intellij.runner.webapp.webappconfig.validator.WebAppValidator.validateWebApp
import java.io.IOException

class RiderWebAppConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
        AzureRunConfigurationBase<AzureDotNetWebAppSettingModel>(project, factory, name) {

    private val myModel = AzureDotNetWebAppSettingModel()

    init {
        myModel.webAppModel.publishableProject = project.solution.publishableProjectsModel.publishableProjects.values
                .sortedWith(compareBy({ it.isWeb }, { it.projectName })).firstOrNull()
    }

    override fun getSubscriptionId(): String {
        return myModel.webAppModel.subscriptionId
    }

    override fun getTargetPath(): String {
        return myModel.webAppModel.publishableProject?.projectFilePath ?: ""
    }

    override fun getTargetName(): String {
        return myModel.webAppModel.publishableProject?.projectName ?: ""
    }

    override fun getModel(): AzureDotNetWebAppSettingModel {
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
        validateAzureAccountIsSignedIn()

        validateWebApp(myModel.webAppModel)
        validateDatabaseConnection(myModel.databaseModel)

        if (myModel.databaseModel.isDatabaseConnectionEnabled) {
            checkConnectionStringNameExistence(myModel.databaseModel.connectionStringName, myModel.webAppModel.webAppId)
        }
    }

    /**
     * Check whether user is signed in to Azure account
     *
     * @throws [ConfigurationException] in case validation is failed
     */
    @Throws(RuntimeConfigurationError::class)
    private fun validateAzureAccountIsSignedIn() {
        try {
            if (!AuthMethodManager.getInstance().isSignedIn) {
                throw RuntimeConfigurationError(UiConstants.SIGN_IN_REQUIRED)
            }
        } catch (e: IOException) {
            throw RuntimeConfigurationError(UiConstants.SIGN_IN_REQUIRED)
        }
    }

    /**
     * Check Connection String name existence for a web app
     *
     * @param name connection string name
     * @param webAppId a web app to check
     * @throws [RuntimeConfigurationError] when connection string with a specified name already configured for a web app
     */
    @Throws(RuntimeConfigurationError::class)
    private fun checkConnectionStringNameExistence(name: String, webAppId: String) {
        SqlDatabaseValidator.checkValueIsSet(name, UiConstants.CONNECTION_STRING_NAME_NOT_DEFINED)
        val webApp = AzureModel.getInstance().resourceGroupToWebAppMap
                .flatMap { it.value }
                .firstOrNull { it.id() == webAppId } ?: return

        if (AzureDotNetWebAppMvpModel.checkConnectionStringNameExists(webApp, name))
            throw RuntimeConfigurationError(String.format(UiConstants.CONNECTION_STRING_NAME_ALREADY_EXISTS, name))
    }
}
