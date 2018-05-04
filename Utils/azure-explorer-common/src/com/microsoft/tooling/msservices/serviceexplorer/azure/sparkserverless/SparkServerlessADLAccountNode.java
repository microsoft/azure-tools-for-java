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

package com.microsoft.tooling.msservices.serviceexplorer.azure.sparkserverless;

import com.google.common.collect.ImmutableList;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;

import java.util.List;

public class SparkServerlessADLAccountNode extends AzureRefreshableNode {
    private static final String ADLAccount_MODULE_ID = SparkServerlessADLAccountNode.class.getName();
    // TODO: Update icon path
    private static final String ICON_PATH = "StorageAccount_16.png";
    // TODO: Update adlAccount type
    private final String adlAccount;

    public SparkServerlessADLAccountNode(@NotNull Node parent, @NotNull String adlAccountName) {
        super(ADLAccount_MODULE_ID, adlAccountName, parent, ICON_PATH, true);
        this.adlAccount = adlAccountName;
        this.loadActions();
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        synchronized (this) {
            // TODO: Update getClusterList
            List<String> clusterNameList = ImmutableList.of("spark20", "spark21", "spark22");
            clusterNameList.forEach(clusterName ->
                    addChildNode(new SparkServerlessClusterNode(this, clusterName, adlAccount)));
        }
    }

    @Override
    protected void loadActions() {
        super.loadActions();

        addAction("Provision a Serverless Cluster", new SparkServerlessProvisionAction(this, adlAccount));
    }

    public String getAdlAccount() {
        return adlAccount;
    }
}
