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
package com.microsoft.azure.sparkserverless.serverexplore;

import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;

public class SparkServerlessClusterProvisionCtrlProvider {
    public static final int ILLEGAL_NUMERIC_INPUT = 0;

    @NotNull
    private SettableControl<SparkServerlessClusterProvisionSettingsModel> controllableView;

    @NotNull
    private IdeSchedulers ideSchedulers;

    public SparkServerlessClusterProvisionCtrlProvider(
            @NotNull SettableControl<SparkServerlessClusterProvisionSettingsModel> controllableView,
            @NotNull IdeSchedulers ideSchedulers) {
        this.controllableView = controllableView;
        this.ideSchedulers = ideSchedulers;
    }

    @NotNull
    public static int editorStringToInt(@Nullable String str) {
        return StringUtils.isNumeric(str) ? Integer.valueOf(str) : ILLEGAL_NUMERIC_INPUT;
    }

    // TODO: Update formula for getCalculatedAU
    @NotNull
    public int getCalculatedAU(@Nullable String masterCoresStr,
                               @Nullable String masterMemoryStr,
                               @Nullable String workerCoresStr,
                               @Nullable String workerMemoryStr,
                               @Nullable String workerNumberOfContainersStr) {
        int masterCores = editorStringToInt(masterCoresStr);
        int masterMemory = editorStringToInt(masterMemoryStr);
        int workerCores = editorStringToInt(workerCoresStr);
        int workerMemory = editorStringToInt(workerMemoryStr);
        int workerNumberOfContainers = editorStringToInt(workerNumberOfContainersStr);
        return workerNumberOfContainers == ILLEGAL_NUMERIC_INPUT ? ILLEGAL_NUMERIC_INPUT :
                masterCores + masterMemory + workerCores + workerMemory + workerNumberOfContainers;
    }

    // TODO: Update getTotalAU
    @NotNull
    public int getTotalAU(@Nullable String adlAccount) {
        // TODO: adlAccount validity check
        return 300;
    }

    // TODO: Update getAvailableAU
    @NotNull
    public int getAvailableAU(@Nullable String adlAccount) {
        // TODO: adlAccount validity check
        return 30;
    }

    public Observable<SparkServerlessClusterProvisionSettingsModel> validateAndProvision() {
        return Observable.just(new SparkServerlessClusterProvisionSettingsModel())
                .doOnNext(controllableView::getData)
                .observeOn(ideSchedulers.processBarVisibleAsync("Validating the cluster settings..."))
                .map(toUpdate -> {
                    String clusterName = toUpdate.getClusterName();
                    String adlAccount = toUpdate.getAdlAccount();
                    String previousSparkEvents = toUpdate.getPreviousSparkEvents();

                    int masterCores = toUpdate.getMasterCores();
                    int masterMemory = toUpdate.getMasterMemory();
                    int workerCores = toUpdate.getWorkerCores();
                    int workerMemory = toUpdate.getWorkerMemory();
                    int workerNumberOfContainers = toUpdate.getWorkerNumberOfContainers();

                    // Incomplete data check
                    // TODO: Check all of the necessary fields
                    if (StringHelper.isNullOrWhiteSpace(clusterName) ||
                            StringHelper.isNullOrWhiteSpace(adlAccount) ||
                            StringHelper.isNullOrWhiteSpace(previousSparkEvents) ||
                            StringHelper.isNullOrWhiteSpace(String.valueOf(workerNumberOfContainers))) {
                        String highlightPrefix = "* ";
                        if (!toUpdate.getAdlAccountLabelTitle().startsWith(highlightPrefix)) {
                            toUpdate.setAdlAccountLabelTitle(highlightPrefix + toUpdate.getAdlAccountLabelTitle());
                        }
                        if (!toUpdate.getClusterNameLabelTitle().startsWith(highlightPrefix)) {
                            toUpdate.setClusterNameLabelTitle(highlightPrefix + toUpdate.getClusterNameLabelTitle());
                        }
                        if (!toUpdate.getPreviousSparkEventsLabelTitle().startsWith(highlightPrefix)) {
                            toUpdate.setPreviousSparkEventsLabelTitle(
                                    highlightPrefix + toUpdate.getPreviousSparkEventsLabelTitle());
                        }
                        if (!toUpdate.getWorkerNumberOfContainersLabelTitle().startsWith(highlightPrefix)) {
                            toUpdate.setWorkerNumberOfContainersLabelTitle(
                                    highlightPrefix + toUpdate.getWorkerNumberOfContainersLabelTitle());
                        }
                        return toUpdate.setErrorMessage("All (*) fields are required.");
                    }

                    // Numeric field check
                    // TODO: only workerNumberOfContainers field numeric check will be reserved finally
                    if (masterCores == ILLEGAL_NUMERIC_INPUT ||
                            masterMemory == ILLEGAL_NUMERIC_INPUT ||
                            workerCores == ILLEGAL_NUMERIC_INPUT ||
                            workerMemory == ILLEGAL_NUMERIC_INPUT ||
                            workerNumberOfContainers == ILLEGAL_NUMERIC_INPUT) {
                        return toUpdate.setErrorMessage(
                                "These fields should be numeric: Master cores, master memory, " +
                                        "worker cores, worker memory and worker number of containers.");
                    }

                    // TODO: cluster name uniqueness check, AU adequation check

                    // TODO: create a spark serverless cluster here

                    return toUpdate.setErrorMessage(null);
                })
                .observeOn(ideSchedulers.dispatchUIThread())
                .doOnNext(controllableView::setData)
                .filter(data -> StringUtils.isEmpty(data.getErrorMessage()));
    }
}
