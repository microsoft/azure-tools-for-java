package com.microsoft.azure.hdinsight.spark.console

import com.intellij.ide.actions.QuickSwitchSchemeAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.microsoft.azure.hdinsight.spark.run.action.SeqActions

class SparkRunLocalConsoleActionGroups : QuickSwitchSchemeAction() {
    override fun fillActions(project: Project?, group: DefaultActionGroup, dataContext: DataContext) {
        group.add(ActionManager.getInstance().getAction("Actions.RunLivySparkLocalConsoleAction"))
        group.add(ActionManager.getInstance().getAction("Actions.RunCosmosSparkLocalConsoleAction"))
        group.add(ActionManager.getInstance().getAction("Actions.RunArisSparkLocalConsoleAction"))
    }
}

class SelectLivySparkTypeThenRunLocalConsoleAction : SeqActions("Actions.SelectHDInsightSparkType", "Spark.RunScalaLocalConsole")
class SelectCosmosSparkTypeThenRunLocalConsoleAction : SeqActions("Actions.SelectCosmosSparkType", "Spark.RunScalaLocalConsole")
class SelectArisSparkTypeThenRunLocalConsoleAction : SeqActions("Actions.SelectArisSparkType", "Spark.RunScalaLocalConsole")
