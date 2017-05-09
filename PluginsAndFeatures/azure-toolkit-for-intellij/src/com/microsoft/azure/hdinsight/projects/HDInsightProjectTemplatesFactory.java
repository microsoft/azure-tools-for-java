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

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.util.IconLoader;
import com.intellij.platform.ProjectTemplate;
import com.intellij.platform.ProjectTemplatesFactory;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.projects.template.CustomHDInsightTemplateItem;
import com.microsoft.azure.hdinsight.projects.template.CustomProjectTemplate;
import com.microsoft.azure.hdinsight.projects.template.TemplatesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class HDInsightProjectTemplatesFactory extends ProjectTemplatesFactory{
    @NotNull
    @Override
    public String[] getGroups() {
        return new String[] { "HDInsight" };
    }

    @NotNull
    @Override
    public ProjectTemplate[] createTemplates(@Nullable String var1, WizardContext var2) {
        ArrayList<HDInsightTemplateItem> templateItems = HDInsightTemplates.getTemplates();
        int templateCount = templateItems.size();
        List<CustomHDInsightTemplateItem> customHDInsightTemplateItems = TemplatesUtil.getCustomTemplate();

        ProjectTemplate[] projectTemplates = new ProjectTemplate[templateCount + customHDInsightTemplateItems.size()];
        for (int i = 0; i < templateCount ; i++) {
            projectTemplates[i] = new HDInsightProjectTemplate(templateItems.get(i));
        }
        for(int i = templateCount; i < templateCount + customHDInsightTemplateItems.size(); ++i) {
            projectTemplates[i] = new CustomProjectTemplate(customHDInsightTemplateItems.get(i - templateCount));
        }

        return projectTemplates;
    }

    @Override
    public Icon getGroupIcon(String group) {
        return IconLoader.getIcon(CommonConst.ProductIConPath);
    }
}
