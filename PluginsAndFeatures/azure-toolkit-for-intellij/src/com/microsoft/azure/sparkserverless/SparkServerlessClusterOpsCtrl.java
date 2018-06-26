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

package com.microsoft.azure.sparkserverless;

import com.intellij.openapi.diagnostic.Logger;
import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.sparkserverless.serverexplore.sparkserverlessnode.SparkServerlessClusterOps;
import com.microsoft.azure.sparkserverless.serverexplore.ui.SparkServerlessClusterDestoryDialog;
import com.microsoft.azure.sparkserverless.serverexplore.ui.SparkServerlessClusterMonitorDialog;
import com.microsoft.azure.sparkserverless.serverexplore.ui.SparkServerlessClusterUpdateDialog;
import com.microsoft.azure.sparkserverless.serverexplore.ui.SparkServerlessProvisionDialog;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.intellij.rxjava.IdeaSchedulers;

public class SparkServerlessClusterOpsCtrl {
    @NotNull
    private final SparkServerlessClusterOps sparkServerlessClusterOps;
    private static Logger LOG = Logger.getInstance(SparkServerlessClusterOpsCtrl.class.getName());
    private IdeSchedulers ideSchedulers = new IdeaSchedulers(null);

    public SparkServerlessClusterOpsCtrl(@NotNull SparkServerlessClusterOps sparkServerlessClusterOps) {
        this.sparkServerlessClusterOps = sparkServerlessClusterOps;

        this.sparkServerlessClusterOps.getDestroyAction()
                .observeOn(ideSchedulers.dispatchUIThread())
                .subscribe(triplet -> {
                    LOG.info(String.format("Destroy message received. AdlAccount: %s, cluster: %s, currentNode: %s",
                            triplet.getLeft().getName(), triplet.getMiddle(), triplet.getRight().getName()));
                    SparkServerlessClusterDestoryDialog destroyDialog = new SparkServerlessClusterDestoryDialog(
                            triplet.getRight(), triplet.getMiddle());
                    destroyDialog.show();
                }, ex -> LOG.error(ex.getMessage(), ex));

        this.sparkServerlessClusterOps.getProvisionAction()
                .observeOn(ideSchedulers.dispatchUIThread())
                .subscribe(pair -> {
                    LOG.info(String.format("Provision message received. AdlAccount: %s, node: %s",
                            pair.getLeft().getName(), pair.getRight().getName()));
                    SparkServerlessProvisionDialog provisionDialog = new SparkServerlessProvisionDialog(
                            pair.getRight(), pair.getLeft());
                    provisionDialog.show();
                }, ex -> LOG.error(ex.getMessage(), ex));

        this.sparkServerlessClusterOps.getMonitorAction()
                .observeOn(ideSchedulers.dispatchUIThread())
                .subscribe(pair -> {
                    LOG.info(String.format("Monitor message received. cluster: %s, node: %s",
                            pair.getLeft(), pair.getRight()));
                    SparkServerlessClusterMonitorDialog monitorDialog = new SparkServerlessClusterMonitorDialog(
                            pair.getRight(), pair.getLeft());
                    monitorDialog.show();
                }, ex -> LOG.error(ex.getMessage(), ex));

        this.sparkServerlessClusterOps.getUpdateAction()
                .observeOn(ideSchedulers.dispatchUIThread())
                .subscribe(pair -> {
                    LOG.info(String.format("Update message received. cluster: %s, node: %s",
                            pair.getLeft(), pair.getRight()));
                    SparkServerlessClusterUpdateDialog updateDialog = new SparkServerlessClusterUpdateDialog(
                            pair.getRight(), pair.getLeft());
                    updateDialog.show();
                }, ex -> LOG.error(ex.getMessage(), ex));
    }

}
