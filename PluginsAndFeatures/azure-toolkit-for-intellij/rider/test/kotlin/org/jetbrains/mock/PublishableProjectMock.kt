package org.jetbrains.mock

import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.PublishableProjectOutput
import java.io.File

object PublishableProjectMock {

    fun createMockProject(
            name: String = "TestAzureProject",
            isDotNetCore: Boolean = false,
            isWeb: Boolean = false,
            isAzureFunction: Boolean = false,
            projectOutputs: List<PublishableProjectOutput> = emptyList(),
            hasPublishTarget: Boolean = false,
            hasWebPublishTarget: Boolean = false
    ): PublishableProjectModel = PublishableProjectModel(
            projectName = name,
            projectModelId = 1,
            projectFilePath = File(".").canonicalPath,
            isDotNetCore = isDotNetCore,
            isWeb = isWeb,
            isAzureFunction = isAzureFunction,
            pubXmls = emptyList(),
            projectOutputs = projectOutputs,
            hasPublishTarget = hasPublishTarget,
            hasWebPublishTarget = hasWebPublishTarget
    )
}
