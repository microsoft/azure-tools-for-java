package com.microsoft.intellij.runner.db.dbconfig

import com.intellij.openapi.project.Project
import com.microsoft.intellij.runner.AzureRiderSettingPanel
import com.microsoft.intellij.runner.AzureRiderSettingsEditor
import com.microsoft.intellij.runner.db.dbconfig.ui.RiderDatabaseSettingPanel

class RiderDatabaseSettingEditor(project: Project,
                                 databaseConfiguration: RiderDatabaseConfiguration)
    : AzureRiderSettingsEditor<RiderDatabaseConfiguration>() {

    private val myPanel: RiderDatabaseSettingPanel = RiderDatabaseSettingPanel(project, databaseConfiguration)

    override val panel: AzureRiderSettingPanel<RiderDatabaseConfiguration>
        get() = this.myPanel
}
