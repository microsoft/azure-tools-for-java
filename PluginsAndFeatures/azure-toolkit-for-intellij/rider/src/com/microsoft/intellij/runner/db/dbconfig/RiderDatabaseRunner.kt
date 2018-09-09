package com.microsoft.intellij.runner.db.dbconfig

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.DefaultProgramRunner

class RiderDatabaseRunner : DefaultProgramRunner() {

    companion object {
        private const val ID = "com.microsoft.intellij.runner.db.dbconfig.RiderDatabaseRunner"
    }

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return DefaultRunExecutor.EXECUTOR_ID == executorId &&
                profile is RiderDatabaseConfiguration
    }

    override fun getRunnerId(): String {
        return ID
    }
}
