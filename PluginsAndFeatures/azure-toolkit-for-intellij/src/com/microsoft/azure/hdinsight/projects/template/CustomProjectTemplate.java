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
package com.microsoft.azure.hdinsight.projects.template;

import com.intellij.ide.util.projectWizard.AbstractModuleBuilder;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.platform.ProjectTemplate;
import com.microsoft.azure.hdinsight.projects.HDInsightModuleBuilder;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CustomProjectTemplate implements ProjectTemplate {

    private CustomHDInsightTemplateItem templateItem;

    public CustomProjectTemplate(CustomHDInsightTemplateItem parameterItem) {
        this.templateItem = parameterItem;
    }

    @NotNull
    @Override
    public String getName() {
        return this.templateItem.getDisplayText();
    }

    @Nullable
    @Override
    public String getDescription() {
        String description = this.templateItem.getTemplateInfo().getDescription();
        return StringHelper.isNullOrWhiteSpace(description) ? description : "Custom Template of HDInsight Tools";
    }

    @Override
    public Icon getIcon() {
        return new ImageIcon(this.templateItem.getTemplateInfo().getIconPath());
    }

    @NotNull
    @Override
    public AbstractModuleBuilder createModuleBuilder() {
        HDInsightModuleBuilder builder = new HDInsightModuleBuilder();
        builder.setSelectedTemplate(this.templateItem);
        return builder;
    }

    @Nullable
    @Override
    public ValidationInfo validateSettings() {
        return null;
    }
}
