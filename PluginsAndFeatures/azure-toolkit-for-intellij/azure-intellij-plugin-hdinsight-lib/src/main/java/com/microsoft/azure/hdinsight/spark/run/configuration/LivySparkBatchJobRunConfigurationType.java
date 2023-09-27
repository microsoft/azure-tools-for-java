/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.run.configuration;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJobConfigurableModel;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

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

        @Override
        public String getId() {
            return NAME;
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
        return "Apache Spark on HDInsight Run Configuration";
    }

    @Override
    public Icon getIcon() {
        return IntelliJAzureIcons.getIcon(AzureIcons.HDInsight.MODULE);
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
