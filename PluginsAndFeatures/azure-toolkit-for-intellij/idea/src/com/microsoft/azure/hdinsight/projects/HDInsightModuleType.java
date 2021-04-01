/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
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
