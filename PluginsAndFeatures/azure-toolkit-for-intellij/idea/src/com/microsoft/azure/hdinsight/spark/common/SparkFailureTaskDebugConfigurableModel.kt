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

package com.microsoft.azure.hdinsight.spark.common

import com.intellij.execution.CommonJavaRunConfigurationParameters
import com.intellij.execution.configurations.RunConfigurationModule
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializer
import com.microsoft.azure.hdinsight.spark.run.SparkFailureTaskDebugSettingsModel
import org.jdom.Element
import java.nio.file.Paths

// As a model adapter
class SparkFailureTaskDebugConfigurableModel(project: Project)
    : RunConfigurationModule(project), CommonJavaRunConfigurationParameters {
    var settings: SparkFailureTaskDebugSettingsModel = SparkFailureTaskDebugSettingsModel()

    val log4jProperties: String?
        get() = settings.log4jProperties

    override fun getEnvs(): MutableMap<String, String> {
        return settings.envs
    }

    override fun setAlternativeJrePath(ignored: String?) {
    }

    override fun isPassParentEnvs(): Boolean {
        return settings.isPassParentEnvs
    }

    override fun setProgramParameters(programeParameters: String?) {
        settings.programParameters = programParameters
    }

    override fun setVMParameters(vmParameters: String?) {
        settings.vmParameters = vmParameters
    }

    override fun isAlternativeJrePathEnabled(): Boolean {
        return false
    }

    override fun getPackage(): String? {
        return null
    }

    override fun getRunClass(): String {
        return "org.apache.spark.tools.FailureTaskRecoveryApp"
    }

    override fun getWorkingDirectory(): String? {
        // Set the working directory to the one of Spark Failure Task Context
        return settings.failureContextPath?.let { Paths.get(it).parent?.toString() }
    }

    override fun setAlternativeJrePathEnabled(ignored: Boolean) {
    }

    override fun getVMParameters(): String? {
        return settings.vmParameters
    }

    override fun setWorkingDirectory(ignored: String?) {
    }

    override fun setEnvs(envs: MutableMap<String, String>) {
        settings.envs = envs
    }

    override fun setPassParentEnvs(isPassParentEnvs: Boolean) {
        settings.isPassParentEnvs = isPassParentEnvs
    }

    override fun getProgramParameters(): String? {
        return settings.programParameters
    }

    override fun getAlternativeJrePath(): String? {
        return null
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)

        val settingsElement : Element? = element.getChild(SparkFailureTaskDebugSettingsModel::class.simpleName)

        if (settingsElement != null) {
            settings = XmlSerializer.deserialize(settingsElement, SparkFailureTaskDebugSettingsModel::class.java)
        }
    }

    override fun writeExternal(parent: Element) {
        super.writeExternal(parent)

        parent.addContent(XmlSerializer.serialize(settings))
    }
}