/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.hdinsight.projects;

import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.util.IconLoader;
import com.microsoft.azure.hdinsight.common.CommonConst;
import com.microsoft.azure.hdinsight.common.IconPathBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class HDInsightModuleType extends ModuleType<HDInsightModuleBuilder> {
    private static final HDInsightModuleType INSTANCE = new HDInsightModuleType();

    public HDInsightModuleType() {
        super("JAVA_MODULE");
    }

    public static HDInsightModuleType getInstance() {
        return INSTANCE;
    }

    @NotNull
    @Override
    public HDInsightModuleBuilder createModuleBuilder() {
        return new HDInsightModuleBuilder();
    }

    @NotNull
    @Override
    public String getName() {
        return "HDInsight Projects";
    }

    @NotNull
    @Override
    public String getDescription() {
        return "Support HDInsight products.";
    }

    public Icon getBigIcon() {
        return null;
    }

    @Override
    public Icon getNodeIcon(@Deprecated boolean b) {
        return IconLoader.getIcon(IconPathBuilder
                .custom(CommonConst.ProductIconName)
                .build());
    }
}
