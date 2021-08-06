/*
 * Copyright (c) Microsoft Corporation
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

package com.microsoft.azure.hdinsight.spark.console

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configuration.AbstractRunConfiguration
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.packaging.impl.artifacts.ArtifactUtil
import com.intellij.packaging.impl.run.BuildArtifactsBeforeRunTaskProvider.setBuildArtifactBeforeRun
import com.microsoft.azure.arcadia.sdk.common.livy.interactive.MfaEspSparkSession
import com.microsoft.azure.hdinsight.common.AbfsUri
import com.microsoft.azure.hdinsight.common.ClusterManagerEx
import com.microsoft.azure.hdinsight.common.WasbUri
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.sdk.cluster.IClusterDetail
import com.microsoft.azure.hdinsight.sdk.cluster.LivyCluster
import com.microsoft.azure.hdinsight.sdk.cluster.MfaEspCluster
import com.microsoft.azure.hdinsight.sdk.common.livy.interactive.SparkSession
import com.microsoft.azure.hdinsight.spark.common.Deployable
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitModel
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitStorageType
import com.microsoft.azure.hdinsight.spark.run.SparkBatchJobDeployFactory
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration
import com.microsoft.azure.hdinsight.spark.run.configuration.RunProfileStatePrepare
import org.apache.commons.lang3.exception.ExceptionUtils
import rx.Observable
import java.net.URI
import java.util.*
import java.util.AbstractMap.SimpleImmutableEntry

open class SparkScalaLivyConsoleRunConfiguration(project: Project,
                                                 configurationFactory: SparkScalaLivyConsoleRunConfigurationFactory,
                                                 private val batchRunConfiguration: LivySparkBatchJobRunConfiguration?,
                                                 name: String)
    : RunProfileStatePrepare<SparkScalaLivyConsoleRunConfiguration>, ILogger, AbstractRunConfiguration(
        name, batchRunConfiguration?.configurationModule ?: RunConfigurationModule(project), configurationFactory) {
    open val runConfigurationTypeName = "HDInsight Spark Run Configuration"

    protected val submitModel: SparkSubmitModel?
        get() = batchRunConfiguration?.submitModel

    private var deployDelegate: Deployable? = null

    private val clusterName: String
        get() = submitModel?.submissionParameter?.clusterName
                ?: throw RuntimeConfigurationWarning("A(n) $runConfigurationTypeName should be selected to start a console")

    private var cluster: IClusterDetail? = null

    open fun findCluster(clusterName: String): IClusterDetail = ClusterManagerEx.getInstance()
            .getClusterDetailByName(clusterName)
            .orElseThrow {
                RuntimeConfigurationError(
                        "Can't prepare Spark Livy interactive session since the target cluster isn't set or found")
            }

    private val consoleBuilder: SparkScalaConsoleBuilder
        get() {
            batchRunConfiguration?.configurationModule?.setModuleToAnyFirstIfNotSpecified()

            return SparkScalaConsoleBuilder(project, batchRunConfiguration?.modules?.firstOrNull()
                    ?: throw RuntimeConfigurationError(
                            "The default module needs to be set in the local run tab of Run Configuration"))
        }


    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
            SparkScalaLivyConsoleRunConfigurationEditor()

    override fun getValidModules(): MutableCollection<Module> {
        val moduleName = batchRunConfiguration?.model?.localRunConfigurableModel?.classpathModule
        val moduleManager = ModuleManager.getInstance(project)
        val module = moduleName?.let { moduleManager.findModuleByName(it) }
                ?: moduleManager.modules.first { it.name.equals(project.name, ignoreCase = true) }

        return module?.let { mutableListOf(it) } ?: mutableListOf()
    }

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
        try {
            val sparkCluster = cluster ?: throw RuntimeConfigurationError(
                    "The Spark cluster is not set. Invoke prepare() method firstly.")
            val artifactDeploy = deployDelegate ?: throw RuntimeConfigurationError(
                    "The Spark deploy delegate is not set. Invoke prepare() method firstly.")
            val sparkSession = createSession(sparkCluster).apply {
                applyRunConfiguration(sparkCluster, this, artifactDeploy)
            }

            return SparkScalaLivyConsoleRunProfileState(consoleBuilder, sparkSession)
        } catch (err: Throwable) {
            throw ExecutionException(err)
        }
    }

    open fun createSession(sparkCluster: IClusterDetail): SparkSession {
        val url = URI.create((sparkCluster as? LivyCluster)?.livyConnectionUrl
                ?: throw RuntimeConfigurationError("Can't prepare Spark interactive session since Livy URL is empty"))

        return if (sparkCluster is MfaEspCluster) MfaEspSparkSession(
                name,
                url,
                sparkCluster.tenantId)
        else SparkSession(
                name,
                url,
                sparkCluster.httpUserName,
                sparkCluster.httpPassword)
    }

    private val batchSubmitModel: SparkSubmitModel
        get() = this.batchRunConfiguration?.submitModel
                ?: throw RuntimeConfigurationError("Can't find Spark batch job configuration to inherit")

    protected open fun transformToGen2Uri(url: String?): String? {
        return if (AbfsUri.isType(url)) AbfsUri.parse(url).uri.toString() else url
    }

    open fun applyRunConfiguration(sparkCluster: IClusterDetail, session: SparkSession, deploy: Deployable) {
        session.createParameters
                .referJars(*batchSubmitModel.referenceJars.map { url -> transformToGen2Uri(url) }.toTypedArray())
                .referFiles(*batchSubmitModel.referenceFiles.map { url -> transformToGen2Uri(url) }.toTypedArray())

        if (batchSubmitModel.jobUploadStorageModel.storageAccountType == SparkSubmitStorageType.BLOB) {
            try {
                val fsRoot = WasbUri.parse(batchSubmitModel.jobUploadStorageModel.uploadPath)
                val storageKey = batchSubmitModel.jobUploadStorageModel.storageKey
                val updatedStorageConfig = batchSubmitModel.jobConfigs.map { SimpleImmutableEntry(it[0], it[1]) }.toMutableList().apply {
                    // We need the following config to fix issue https://github.com/microsoft/azure-tools-for-java/issues/5002
                    add(SimpleImmutableEntry("spark.hadoop." + fsRoot.hadoopBlobFsPropertyKey, storageKey))
                    add(SimpleImmutableEntry("spark.hadoop." + fsRoot.keyProviderPropertyKey, fsRoot.defaultKeyProviderPropertyValue))
                }
                session.createParameters.conf(updatedStorageConfig)
            } catch (error: UnknownFormatConversionException) {
                val errorHint = "Azure blob storage uploading path is not in correct format"
                log().warn(String.format("%s. Uploading Path: %s. Error message: %s. Stacktrace:\n%s",
                        errorHint, batchSubmitModel.jobUploadStorageModel.uploadPath, error.message,
                        ExceptionUtils.getStackTrace(error)))
                throw RuntimeConfigurationError(errorHint)
            } catch (error: Exception) {
                val errorHint = "Failed to update config for linked Azure Blob storage"
                log().warn(String.format("%s. Error message: %s. Stacktrace:\n%s",
                        errorHint, error.message, ExceptionUtils.getStackTrace(error)))
                throw RuntimeConfigurationError(errorHint)
            }
        }

        batchSubmitModel.artifactPath.ifPresent {
            session.deploy = deploy
            synchronized(session.artifactsToDeploy) {
                session.artifactsToDeploy.clear()
                session.artifactsToDeploy.add(it)
            }
        }

        if (!batchSubmitModel.isLocalArtifact) {
            ArtifactUtil.getArtifactWithOutputPaths(project)
                    .first { artifact -> artifact.name == batchSubmitModel.artifactName }
                    ?.let { setBuildArtifactBeforeRun(project, this, it) }
        }
    }

    override fun checkRunnerSettings(runner: ProgramRunner<*>,
                                     runnerSettings: RunnerSettings?,
                                     configurationPerRunnerSettings: ConfigurationPerRunnerSettings?) {
        batchRunConfiguration?.checkRunnerSettings(runner, runnerSettings, configurationPerRunnerSettings)
    }

    override fun prepare(runner: ProgramRunner<RunnerSettings>): Observable<SparkScalaLivyConsoleRunConfiguration> {
        return Observable.fromCallable {
            try {
                val sparkCluster = findCluster(clusterName)
                deployDelegate = SparkBatchJobDeployFactory.getInstance().buildSparkBatchJobDeploy(
                        batchSubmitModel, sparkCluster)
                cluster = sparkCluster

                this
            } catch (err: Throwable) {
                throw ExecutionException(err)
            }
        }
    }
}
