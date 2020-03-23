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

package com.microsoft.azure.hdinsight.spark.run.configuration;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;

import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.IconPathBuilder;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJobConfigurableModel;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.intellij.util.PluginUtil;

import javax.swing.*;

public class LivySparkBatchJobRunConfigurationType implements ConfigurationType {
    private class DefaultConfigFactory extends ConfigurationFactory {
        private static final String NAME = "HDInsight Spark";

        DefaultConfigFactory() {
            super(LivySparkBatchJobRunConfigurationType.this);
        }

        @Override
        public String getName() {
            return NAME;
        }

        @NotNull
        @Override
        public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
            return new LivySparkBatchJobRunConfiguration(project, new SparkBatchJobConfigurableModel(project), this, NAME);
        }
    }

    @NotNull
    private final ConfigurationFactory myConfigurationFactory;

    public LivySparkBatchJobRunConfigurationType() {
        this.myConfigurationFactory = new DefaultConfigFactory();
    }

    @Override
    public String getDisplayName() {
        return "Apache Spark on HDInsight";
    }

    @Override
    public String getConfigurationTypeDescription() {
        return "Spark on HDInsight Run Configuration";
    }

    @Override
    public Icon getIcon() {
        return PluginUtil.getIcon(IconPathBuilder
                .custom(CommonConst.OpenSparkUIIconName)
                .build());
    }

    @NotNull
    @Override
    public String getId() {
        return "SubmitSparkJob_Configuration";
    }

    @Override
    public ConfigurationFactory[] getConfigurationFactories() {
        return new ConfigurationFactory[] { myConfigurationFactory };
    }

    @NotNull
    public static LivySparkBatchJobRunConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(LivySparkBatchJobRunConfigurationType.class);
    }
}
