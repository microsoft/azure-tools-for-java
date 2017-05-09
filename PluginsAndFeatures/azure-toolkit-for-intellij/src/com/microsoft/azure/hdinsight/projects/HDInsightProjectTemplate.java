/**
 * Copyright (c) Microsoft Corporation
 * <p/>
 * All rights reserved.
 * <p/>
 * MIT License
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.microsoft.azure.hdinsight.projects;

import com.intellij.ide.util.projectWizard.AbstractModuleBuilder;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.ProjectTemplate;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.StreamUtil;
import com.microsoft.azure.hdinsight.projects.template.CustomHDInsightTemplateItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;

public class HDInsightProjectTemplate implements ProjectTemplate {
    private HDInsightTemplateItem templateItem;

    private static HashMap<HDInsightTemplatesType, ImageIcon> imageMap = new HashMap<HDInsightTemplatesType, ImageIcon>() {
        {
            put(HDInsightTemplatesType.Java, StreamUtil.getImageResourceFile(CommonConst.JavaProjectIconPath));
            put(HDInsightTemplatesType.Scala, StreamUtil.getImageResourceFile(CommonConst.ScalaProjectIconPath));
            put(HDInsightTemplatesType.JavaLocalSample, StreamUtil.getImageResourceFile(CommonConst.JavaProjectIconPath));
            put(HDInsightTemplatesType.ScalaClusterSample, StreamUtil.getImageResourceFile(CommonConst.ScalaProjectIconPath));
            put(HDInsightTemplatesType.ScalaLocalSample, StreamUtil.getImageResourceFile(CommonConst.ScalaProjectIconPath));
        }
    };

    public HDInsightProjectTemplate(HDInsightTemplateItem parameterItem) {
        this.templateItem = parameterItem;
    }

    @NotNull
    @Override
    public String getName() { return templateItem.getDisplayText(); }

    @Nullable
    @Override
    public String getDescription() {
        switch (templateItem.getType()) {
            case Java:
            case Scala:
                return "HDInsight Spark blank module.";
            case JavaLocalSample:
                return "HDInsight Spark samples written in Scala; This code sample should be executed locally.";
            case ScalaLocalSample:
                return "HDInsight Spark samples written in Java; This code sample should be executed locally.";
            case ScalaClusterSample:
                return "HDInsight Spark samples written in Java; This code sample should be submitted to HDInsight cluster.";
            default:
                return "HDInsight Tools";
        }
    }

    @Override
    public Icon getIcon() {
        if(this.templateItem instanceof CustomHDInsightTemplateItem) {
            return new ImageIcon(((CustomHDInsightTemplateItem) this.templateItem).getTemplateInfo().getIconPath());
        } else {
            return imageMap.get(templateItem.getType());
        }
    }

    @NotNull
    @Override
    public AbstractModuleBuilder createModuleBuilder() {
        HDInsightModuleBuilder builder = new HDInsightModuleBuilder();
        builder.setSelectedTemplate(templateItem);
        return builder;
    }

    @Nullable
    @Override
    public ValidationInfo validateSettings() { return null; }
}
