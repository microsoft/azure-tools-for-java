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
import java.util.ArrayList;

public class HDInsightTemplates {
    private static final String[] TemplateNames = {
            "Spark on HDInsight (Scala)",
            "Spark on HDInsight (Java)",
            "Spark on HDInsight Local Run Sample (Scala)",
            "Spark on HDInsight Local Run Sample (Java)",
            "Spark on HDInsight Cluster Run Sample (Scala)",
    };

    private static ArrayList<HDInsightTemplateItem> templates = new ArrayList<HDInsightTemplateItem>();

    static {
        templates.add(new HDInsightTemplateItem(TemplateNames[0], HDInsightTemplatesType.Scala));
        templates.add(new HDInsightTemplateItem(TemplateNames[1], HDInsightTemplatesType.Java));
        templates.add(new HDInsightTemplateItem(TemplateNames[2], HDInsightTemplatesType.ScalaLocalSample));
        templates.add(new HDInsightTemplateItem(TemplateNames[3], HDInsightTemplatesType.JavaLocalSample));
        templates.add(new HDInsightTemplateItem(TemplateNames[4], HDInsightTemplatesType.ScalaClusterSample));
    }

    public static String[] getTemplateNames() { return TemplateNames; }

    public static ArrayList<HDInsightTemplateItem> getTemplates() {
        return templates;
    }
}

