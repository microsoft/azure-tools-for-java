/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.spark.run;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.ConfigurationFromContext;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.junit.JavaRunConfigurationProducerBase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.microsoft.azure.hdinsight.spark.common.SparkBatchJobConfigurableModel;
import com.microsoft.azure.hdinsight.spark.run.action.SelectSparkApplicationTypeAction;
import com.microsoft.azure.hdinsight.spark.run.action.SparkApplicationType;
import com.microsoft.azure.hdinsight.spark.run.configuration.LivySparkBatchJobRunConfiguration;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

import static com.intellij.openapi.roots.TestSourcesFilter.isTestSources;

public class SparkBatchJobLocalRunConfigurationProducer
        extends JavaRunConfigurationProducerBase<LivySparkBatchJobRunConfiguration> {
    private final SparkApplicationType applicationType;

    public SparkBatchJobLocalRunConfigurationProducer(final ConfigurationFactory configFactory,
                                                      final SparkApplicationType applicationType) {
        super(configFactory);
        this.applicationType = applicationType;
    }

    public SparkBatchJobLocalRunConfigurationProducer(final ConfigurationType configType,
                                                      final SparkApplicationType applicationType) {
        super(configType);
        this.applicationType = applicationType;
    }

    @Override
    public boolean setupConfigurationFromContext(final LivySparkBatchJobRunConfiguration configuration,
                                                 final ConfigurationContext context,
                                                 final Ref<PsiElement> sourceElement) {
        if (SelectSparkApplicationTypeAction.getSelectedSparkApplicationType() != this.applicationType) {
            return false;
        } else {
            return Optional.ofNullable(context.getModule())
                           .map(Module::getProject)
                           .flatMap(project -> Optional
                                   .ofNullable(SparkContextUtilsKt.getSparkMainClassWithElement(context))
                                   .filter(mainClass ->
                                                   SparkContextUtilsKt.isSparkContext(mainClass.getContainingFile()) &&
                                                           !isTestSources(mainClass.getContainingFile()
                                                                                   .getVirtualFile(), project)))
                           .map(mainClass -> {
                               setupConfiguration(configuration, mainClass, context);

                               return true;
                           })
                           .orElse(false);
        }
    }

    private void setupConfiguration(final LivySparkBatchJobRunConfiguration configuration,
                                    final PsiClass clazz,
                                    final ConfigurationContext context) {
        final SparkBatchJobConfigurableModel jobModel = configuration.getModel();

        getNormalizedClassName(clazz)
                .ifPresent(mainClass -> {
                    jobModel.getSubmitModel().getSubmissionParameter().setClassName(mainClass);
                    jobModel.getLocalRunConfigurableModel().setRunClass(mainClass);
                });

        configuration.setGeneratedName();
        configuration.setActionProperty(LivySparkBatchJobRunConfiguration.ACTION_TRIGGER_PROP, "Context");
        setupConfigurationModule(context, configuration);
    }

    private static Optional<String> getNormalizedClassName(@NotNull final PsiClass clazz) {
        return Optional.ofNullable(SparkContextUtilsKt.getNormalizedClassNameForSpark(clazz));
    }

    /**
     * The function to help reuse RunConfiguration
     *
     * @param jobConfiguration Run Configuration to test
     * @param context          current Context
     * @return true for reusable
     */
    @Override
    public boolean isConfigurationFromContext(final LivySparkBatchJobRunConfiguration jobConfiguration,
                                              final ConfigurationContext context) {
        return Optional.ofNullable(SparkContextUtilsKt.getSparkMainClassWithElement(context))
                       .map(mainClass -> {
                           if (!StringUtils.equals(jobConfiguration.getModel()
                                                                   .getLocalRunConfigurableModel()
                                                                   .getRunClass(),
                                                   SparkContextUtilsKt.getNormalizedClassNameForSpark(mainClass))) {
                               return false;
                           }

                           if (isTestSources(mainClass.getContainingFile().getVirtualFile(),
                                             jobConfiguration.getProject())) {
                               return false;
                           }

                           final Module configurationModule = jobConfiguration.getConfigurationModule().getModule();

                           if (!Comparing.equal(context.getModule(), configurationModule)) {

                               RunConfiguration template = context
                                       .getRunManager()
                                       .getConfigurationTemplate(getConfigurationFactory())
                                       .getConfiguration();

                               if (!(template instanceof LivySparkBatchJobRunConfiguration)) {
                                   return false;
                               }

                               final Module predefinedModule = ((LivySparkBatchJobRunConfiguration) template)
                                       .getConfigurationModule()
                                       .getModule();

                               if (!Comparing.equal(predefinedModule, configurationModule)) {
                                   return false;
                               }
                           }

                           jobConfiguration.setActionProperty(LivySparkBatchJobRunConfiguration.ACTION_TRIGGER_PROP,
                                                              "ContextReuse");
                           return true;
                       })
                       .orElse(false);
    }

    @Override
    public boolean shouldReplace(@NotNull final ConfigurationFromContext self,
                                 @NotNull final ConfigurationFromContext anyOther) {
        return true;
    }
}
