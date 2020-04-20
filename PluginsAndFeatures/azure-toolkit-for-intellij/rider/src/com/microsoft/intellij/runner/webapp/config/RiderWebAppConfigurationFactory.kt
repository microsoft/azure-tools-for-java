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

package com.microsoft.intellij.runner.webapp.config

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class RiderWebAppConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

    companion object {
        // Note: this FACTORY_ID can not be changed, as it may break existing run configurations out here.
        private const val FACTORY_ID = "Azure Web App"

        // TODO: FIX_LOCALIZATION: This name should be moved to RiderAzureMessages and might cause issues with existing
        //       run configuration when localization is enabled
        private const val FACTORY_NAME = "Azure Web App"
    }

    override fun getId() = FACTORY_ID

    override fun getName() = FACTORY_NAME

    override fun createTemplateConfiguration(project: Project) =
            RiderWebAppConfiguration(project, this, project.name)

    override fun createConfiguration(name: String?, template: RunConfiguration) =
            RiderWebAppConfiguration(template.project, this, name)
}