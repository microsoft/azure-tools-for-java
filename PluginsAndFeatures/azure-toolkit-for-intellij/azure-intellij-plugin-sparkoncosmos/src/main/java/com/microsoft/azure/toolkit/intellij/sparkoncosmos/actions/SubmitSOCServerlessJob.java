package com.microsoft.azure.toolkit.intellij.sparkoncosmos.actions;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.cosmos.spark.actions.CosmosServerlessSparkSelectAndSubmitAction;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.cosmos.spark.actions.SparkAppSubmitContext;
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosServerlessSparkConfigurationFactory;
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosServerlessSparkConfigurationType;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosADLAccountNode;

import java.util.List;

import static com.microsoft.azure.hdinsight.spark.actions.SparkDataKeys.CLUSTER;
import static com.microsoft.azure.hdinsight.spark.actions.SparkDataKeys.RUN_CONFIGURATION_SETTING;

public class SubmitSOCServerlessJob {
    public static void submit(SparkOnCosmosADLAccountNode target, AnActionEvent e) {
        try {
            AzureSparkServerlessAccount adlAccount = target.getRemote();
            SparkAppSubmitContext context = new SparkAppSubmitContext();
            Project project = e.getProject();

            final RunManager runManager = RunManager.getInstance(project);
            final List<RunnerAndConfigurationSettings> batchConfigSettings = runManager
                    .getConfigurationSettingsList(CosmosServerlessSparkConfigurationType.INSTANCE);

            final String runConfigName = "[Spark on Cosmos Serverless] " + adlAccount.getName();
            final RunnerAndConfigurationSettings runConfigurationSetting = batchConfigSettings.stream()
                    .filter(settings -> settings.getConfiguration().getName().startsWith(runConfigName))
                    .findFirst()
                    .orElseGet(() -> runManager.createRunConfiguration(
                            runConfigName,
                            new CosmosServerlessSparkConfigurationFactory(CosmosServerlessSparkConfigurationType.INSTANCE)));

            context.putData(RUN_CONFIGURATION_SETTING, runConfigurationSetting)
                    .putData(CLUSTER, adlAccount);

            Presentation actionPresentation = new Presentation("Submit Cosmos Serverless Spark Job");
            actionPresentation.setDescription("Submit specified Spark application into the remote cluster");

            AnActionEvent event = AnActionEvent.createFromDataContext(
                    String.format("Cosmos Serverless Cluster %s:%s context menu",
                            adlAccount.getName(), adlAccount.getName()),
                    actionPresentation,
                    context);

            new CosmosServerlessSparkSelectAndSubmitAction().actionPerformed(event);
        } catch (Exception ignore) {

        }
    }
}
