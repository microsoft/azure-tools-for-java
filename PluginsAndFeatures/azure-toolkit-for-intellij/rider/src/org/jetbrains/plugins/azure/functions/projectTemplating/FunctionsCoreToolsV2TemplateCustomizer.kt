package org.jetbrains.plugins.azure.functions.projectTemplating

import com.intellij.openapi.util.IconLoader
import com.jetbrains.rider.projectView.actions.projectTemplating.backend.ReSharperProjectTemplateCustomizer
import javax.swing.Icon

class FunctionsCoreToolsV2TemplateCustomizer : ReSharperProjectTemplateCustomizer {
    override val categoryName: String
        get() = "Azure Functions"

    override val newIcon: Icon
        get() = IconLoader.getIcon("icons/TemplateAzureFunc.svg")

    override val newName: String
        get() = "Azure Functions"
}