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

package com.microsoft.azure.hdinsight.spark.ui

import com.microsoft.azure.hdinsight.common.ClusterManagerEx
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.common.mvc.SettableControl
import com.microsoft.azure.hdinsight.sdk.common.HDIException
import com.microsoft.azure.hdinsight.spark.common.DebugParameterDefinedException
import com.microsoft.azure.hdinsight.spark.common.SparkBatchDebugSession
import com.microsoft.azure.hdinsight.spark.common.SparkBatchRemoteDebugJobSshAuth.SSHAuthType.UseKeyFile
import com.microsoft.azure.hdinsight.spark.common.SparkBatchRemoteDebugJobSshAuth.SSHAuthType.UsePassword
import com.microsoft.azure.hdinsight.spark.common.SparkSubmitAdvancedConfigModel
import com.microsoft.azuretools.securestore.SecureStore
import com.microsoft.azuretools.service.ServiceManager
import org.apache.commons.lang3.StringUtils
import rx.Subscription
import rx.subjects.PublishSubject
import java.util.concurrent.TimeUnit

abstract class SparkSubmissionAdvancedConfigCtrl(
        val view: SettableControl<SparkSubmitAdvancedConfigModel>,
        private val sshCheckSubject: PublishSubject<String>) : ILogger {
    companion object {
        const val passedText = "passed"

        private fun isParameterReady(model: SparkSubmitAdvancedConfigModel) =
                model.sshUserName.isNotBlank() && when (model.sshAuthType) {
                    UsePassword -> model.sshPassword.isNotBlank()
                    UseKeyFile -> model.sshKeyFile?.exists() == true
                    else -> false
                }

        private fun probeAuth(modelToProbe: SparkSubmitAdvancedConfigModel): CheckResult = try {
            val clusterDetail = ClusterManagerEx.getInstance()
                    .getClusterDetailByName(modelToProbe.clusterName)
                    .orElseThrow { HDIException( "No cluster name matched selection: ${modelToProbe.clusterName}") }

            // Verify the certificate
            val debugSession = SparkBatchDebugSession.factoryByAuth(clusterDetail.connectionUrl, modelToProbe)
                    .open()
                    .verifyCertificate()

            debugSession.close()

            CheckResult(modelToProbe, passedText)
        } catch (ex: SparkBatchDebugSession.SshPasswordExpiredException) {
            CheckResult(modelToProbe, "failed (password expired)")
        } catch (ex: Exception) {
            CheckResult(modelToProbe, "failed")
        }

        @Throws(DebugParameterDefinedException::class)
        fun checkSettings(model: SparkSubmitAdvancedConfigModel) {
            if (model.enableRemoteDebug && (!isParameterReady(model) || probeAuth(model).message != passedText)) {
                throw DebugParameterDefinedException("SSH Authentication is failed")
            }
        }
    }

    data class CheckResult(val model: SparkSubmitAdvancedConfigModel, val message: String)

    private val model: SparkSubmitAdvancedConfigModel
        get() = SparkSubmitAdvancedConfigModel().apply { view.getData(this@apply) }

    init {
        registerAsyncSshAuthCheck()
    }

    fun selectCluster(clusterName: String) {
        sshCheckSubject.onNext("Selected cluster $clusterName")
    }

    abstract fun getClusterNameToCheck(): String?

    private fun isReadyToCheck(model: SparkSubmitAdvancedConfigModel) = model.enableRemoteDebug && isParameterReady(model)

    private fun registerAsyncSshAuthCheck(): Subscription = sshCheckSubject
            .throttleWithTimeout(500, TimeUnit.MILLISECONDS)
            .doOnNext { log().debug("Receive checking message $it") }
            .map { getClusterNameToCheck() }
            .filter { StringUtils.isNotBlank(it) }
            .map { selectedClusterName -> model.apply {
                clusterName = selectedClusterName
            }}
            .filter { isReadyToCheck(it) }
            .doOnNext { modelToProbe ->
                log().info("Check SSH authentication for cluster ${modelToProbe.clusterName} ...")
                view.setData(model.apply {
                    checkingMessage = "SSH Authentication is checking..."
                    isChecking = true
                })
            }
            .map { probeAuth(it).apply {
                if (this.message == passedText) {
                    ServiceManager.getServiceProvider(SecureStore::class.java)?.savePassword(
                            model.credentialStoreAccount, model.sshUserName, model.sshPassword)
                }
            }}
            .subscribe { (probedModel, message) ->
                log().info("...Result: $message")

                val current = model

                // Double check the the result is met the current user input
                val checkingResultMessage = if (current.enableRemoteDebug &&
                        probedModel.sshAuthType == current.sshAuthType &&
                        when (current.sshAuthType) {
                            UseKeyFile -> probedModel.sshKeyFile?.absolutePath == current.sshKeyFile?.absolutePath
                            else -> probedModel.sshPassword == current.sshPassword
                        }) {
                    // Checked parameter is matched with current content

                    "SSH Authentication is $message"
                } else {
                    ""
                }

                view.setData(current.apply {
                    clusterName = probedModel.clusterName
                    isChecking = false
                    checkingMessage = checkingResultMessage
                })
            }
}