package com.microsoft.azure.toolkit.intellij.sparkoncosmos.actions;

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.cosmos.spark.actions.CosmosSparkSelectAndSubmitAction;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkCosmosCluster;
import com.microsoft.azure.cosmos.spark.actions.SparkAppSubmitContext;
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosSparkConfigurationFactory;
import com.microsoft.azure.hdinsight.spark.run.configuration.CosmosSparkConfigurationType;
import com.microsoft.azure.toolkit.lib.sparkoncosmos.SparkOnCosmosClusterNode;

import java.util.List;

import static com.microsoft.azure.hdinsight.spark.actions.SparkDataKeys.CLUSTER;
import static com.microsoft.azure.hdinsight.spark.actions.SparkDataKeys.RUN_CONFIGURATION_SETTING;

public class SubmitClusterJobAction {
    public static void submit(SparkOnCosmosClusterNode target, AnActionEvent e) {
        try {
            AzureSparkCosmosCluster cluster = target.getRemote();
            SparkAppSubmitContext context = new SparkAppSubmitContext();
            Project project = e.getProject();

            final RunManager runManager = RunManager.getInstance(project);
            final List<RunnerAndConfigurationSettings> batchConfigSettings = runManager
                    .getConfigurationSettingsList(CosmosSparkConfigurationType.INSTANCE);

            final String runConfigName = "[Spark on Cosmos] " + cluster.getClusterIdForConfiguration();
            final RunnerAndConfigurationSettings runConfigurationSetting = batchConfigSettings.stream()
                    .filter(settings -> settings.getConfiguration().getName().startsWith(runConfigName))
                    .findFirst()
                    .orElseGet(() -> runManager.createRunConfiguration(
                            runConfigName,
                            new CosmosSparkConfigurationFactory(CosmosSparkConfigurationType.INSTANCE)));

            context.putData(RUN_CONFIGURATION_SETTING, runConfigurationSetting)
                    .putData(CLUSTER, cluster);

            Presentation actionPresentation = new Presentation("Submit Job");
            actionPresentation.setDescription("Submit specified Spark application into the remote cluster");

            AnActionEvent event = AnActionEvent.createFromDataContext(
                    String.format("Azure Data Lake Spark pool %s:%s context menu",
                            cluster.getAccount().getName(), cluster.getName()),
                    actionPresentation,
                    context);

            new CosmosSparkSelectAndSubmitAction().actionPerformed(event);
        } catch (Exception ignore) {
        }
    }
}
