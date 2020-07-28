/*
 * Copyright (c) 2020 JetBrains s.r.o.
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

package com.microsoft.intellij.components;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.arcadia.serverexplore.ArcadiaSparkClusterRootModuleImpl;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkClusterRootModuleImpl;
import com.microsoft.azure.hdinsight.common.HDInsightUtil;
import com.microsoft.azure.sqlbigdata.serverexplore.SqlBigDataClusterModule;
import com.microsoft.tooling.msservices.serviceexplorer.azure.AzureModule;

public class JavaAzureModuleProvider implements AzureModuleProvider {

    @Override
    public void initAzureModule(final Project project, final AzureModule azureModule) {
        HDInsightUtil.setHDInsightRootModule(azureModule);

        azureModule.setSparkServerlessModule(new CosmosSparkClusterRootModuleImpl(azureModule));
        azureModule.setArcadiaModule(new ArcadiaSparkClusterRootModuleImpl(azureModule));

        // initialize aris service module
        SqlBigDataClusterModule arisModule = new SqlBigDataClusterModule(project);
    }
}
