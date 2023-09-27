package com.microsoft.azure.cosmos.spark.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.microsoft.azure.hdinsight.common.logger.ILogger
import com.microsoft.azure.hdinsight.spark.run.action.SeqActions
import com.microsoft.azure.toolkit.intellij.common.action.AzureAnAction
import com.microsoft.azuretools.telemetrywrapper.Operation

class CosmosServerlessSparkSelectAndSubmitAction : SeqActions("Actions.SelectCosmosServerlessSparkType", "Actions.SubmitSparkApplicationAction")

class CosmosSparkSelectAndSubmitAction : SeqActions("Actions.SelectCosmosSparkType", "Actions.SubmitSparkApplicationAction")