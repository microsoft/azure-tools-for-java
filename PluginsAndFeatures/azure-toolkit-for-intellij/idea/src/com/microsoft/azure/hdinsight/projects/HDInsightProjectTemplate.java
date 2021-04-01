/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.projects;

import com.intellij.ide.util.projectWizard.AbstractModuleBuilder;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.ProjectTemplate;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class HDInsightProjectTemplate implements ProjectTemplate {
    private HDInsightTemplatesType templateType;

    public HDInsightProjectTemplate(HDInsightTemplatesType templatesType) {
        this.templateType = templatesType;
    }

    @NotNull
    @Override
    public String getName() {
        switch (this.templateType) {
            case Java:
                return "Spark Project (Java)";
            case Scala:
                return "Spark Project (Scala)";
            case ScalaClusterSample:
                return "Spark Project with Samples (Scala)";
            case ScalaFailureTaskDebugSample:
                return "Spark Project with Failure Task Debugging Sample (Preview) (Scala)";
            default:
                return "HDInsight Tools";
        }
    }

    @Nullable
    @Override
    public String getDescription() {
        switch (this.templateType) {
            case Java:
            case Scala:
                return "Apache Spark blank module project.";
            case ScalaClusterSample:
                return "Apache Spark samples written in Scala.";
            case ScalaFailureTaskDebugSample:
                return "Apache Spark samples with Failure Task Debugging feature enabled, written in Scala";
            default:
                return "HDInsight Tools";
        }
    }

    @Override
    public Icon getIcon() {
        switch (this.templateType) {
            case Java:
                return StreamUtil.getImageResourceFile(CommonConst.JavaProjectIconPath);
            case Scala:
            case ScalaClusterSample:
            case ScalaFailureTaskDebugSample:
                return StreamUtil.getImageResourceFile(CommonConst.ScalaProjectIconPath);
            default:
                return StreamUtil.getImageResourceFile(CommonConst.JavaProjectIconPath);
        }
    }

    @Override
    public AbstractModuleBuilder createModuleBuilder() {
        return null;
    }

    @Nullable
    @Override
    public ValidationInfo validateSettings() {
        return null;
    }

    public HDInsightTemplatesType getTemplateType() {
        return templateType;
    }
}
