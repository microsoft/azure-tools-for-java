/*
 * Copyright (c) Microsoft Corporation
 * Copyright (c) 2018 JetBrains s.r.o.
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

package com.microsoft.intellij;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.IdeActions;
import com.microsoft.azure.cosmosspark.CosmosSparkClusterOpsCtrl;
import com.microsoft.azure.cosmosspark.serverexplore.cosmossparknode.CosmosSparkClusterOps;
import com.microsoft.azure.hdinsight.common.HDInsightHelperImpl;
import com.microsoft.azure.hdinsight.common.HDInsightLoader;
import com.microsoft.azuretools.service.ServiceManager;
import com.microsoft.intellij.helpers.IDEHelperJavaImpl;
import com.microsoft.tooling.msservices.helpers.IDEHelper;

public class AzureJavaActionsComponent extends AzureActionsComponent {

    public AzureJavaActionsComponent() {
        super();
        HDInsightLoader.setHHDInsightHelper(new HDInsightHelperImpl());
    }

    @Override
    public void initComponent() {
        if (!AzurePlugin.IS_ANDROID_STUDIO) {
            // enable spark serverless node subscribe actions
            ServiceManager.setServiceProvider(CosmosSparkClusterOpsCtrl.class,
                    new CosmosSparkClusterOpsCtrl(CosmosSparkClusterOps.getInstance()));

            ActionManager am = ActionManager.getInstance();
            DefaultActionGroup popupGroup = (DefaultActionGroup) am.getAction(IdeActions.GROUP_PROJECT_VIEW_POPUP);
            popupGroup.add(am.getAction("AzurePopupGroup"));
        }
        super.initComponent();
    }

    @Override
    protected IDEHelper createIDEHelper() {
        return new IDEHelperJavaImpl();
    }
}
