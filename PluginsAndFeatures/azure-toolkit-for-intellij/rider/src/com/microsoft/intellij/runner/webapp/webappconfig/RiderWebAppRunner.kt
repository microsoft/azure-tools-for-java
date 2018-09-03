package com.microsoft.intellij.runner.webapp.webappconfig

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.DefaultProgramRunner

class RiderWebAppRunner : DefaultProgramRunner() {

    companion object {
        private const val ID = "com.microsoft.intellij.runner.webapp.webappconfig.RiderWebAppRunner"
    }

    override fun canRun(executorId: String, profile: RunProfile): Boolean {
        return DefaultRunExecutor.EXECUTOR_ID == executorId &&
                profile is RiderWebAppConfiguration
    }

    override fun getRunnerId(): String {
        return ID
    }
}
