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
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessAccount;
import com.microsoft.azure.hdinsight.sdk.common.azure.serverless.AzureSparkServerlessCluster;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import rx.Observable;

import java.util.HashSet;
import java.util.Set;

public class SparkServerlessClusterProvisionCtrlProvider {

    @NotNull
    private SettableControl<SparkServerlessClusterProvisionSettingsModel> controllableView;

    @NotNull
    private IdeSchedulers ideSchedulers;

    @NotNull
    private AzureSparkServerlessAccount account;
    public SparkServerlessClusterProvisionCtrlProvider(
            @NotNull SettableControl<SparkServerlessClusterProvisionSettingsModel> controllableView,
            @NotNull IdeSchedulers ideSchedulers,
            @NotNull AzureSparkServerlessAccount account) {
        this.controllableView = controllableView;
        this.ideSchedulers = ideSchedulers;
        this.account = account;
    }

    public int getCalculatedAU(int masterCores,
                               int workerCores,
                               int masterMemory,
                               int workerMemory,
                               int workerContainer) {
        return (int) Math.max(
                Math.ceil((masterCores + workerCores * workerContainer) / 2.0),
                Math.ceil((masterMemory + workerMemory * workerContainer) / 6.0));
    }

    public void updateCalculatedAU() {
        Observable.just(new SparkServerlessClusterProvisionSettingsModel())
                .doOnNext(controllableView::getData)
                .map(toUpdate -> toUpdate.setCalculatedAU(getCalculatedAU(
                        toUpdate.getMasterCores(),
                        toUpdate.getWorkerCores(),
                        toUpdate.getMasterMemory(),
                        toUpdate.getWorkerMemory(),
                        toUpdate.getWorkerNumberOfContainers())))
                .observeOn(ideSchedulers.dispatchUIThread())
                .doOnNext(controllableView::setData)
                .subscribe();
    }

    public void updateTotalAU() {
        Observable.just(new SparkServerlessClusterProvisionSettingsModel())
                .doOnNext(controllableView::getData)
                .map(toUpdate -> {
                    // TODO: update totalAU field
                    return toUpdate;
                })
                .observeOn(ideSchedulers.dispatchUIThread())
                .doOnNext(controllableView::setData)
                .subscribe();
    }

    public void updateAvailableAU() {
        Observable.just(new SparkServerlessClusterProvisionSettingsModel())
                .doOnNext(controllableView::getData)
                .map(toUpdate -> {
                    // TODO: update availableAU field
                    return toUpdate;
                })
                .observeOn(ideSchedulers.dispatchUIThread())
                .doOnNext(controllableView::setData)
                .subscribe();
    }

    @NotNull
    public SparkServerlessClusterProvisionSettingsModel validateClusterNameUniqueness(
            @NotNull SparkServerlessClusterProvisionSettingsModel toUpdate) {
        if (!StringUtils.isEmpty(toUpdate.getErrorMessage())) {
            return toUpdate;
        }

        Set<String> names = new HashSet<>();
        account.refresh().getClusters().forEach(cluster -> names.add(cluster.getName()));
        if (names.contains(toUpdate.getClusterName())) {
            return toUpdate.setErrorMessage("Cluster name already exists.");
        }
        return toUpdate;

    }

    @NotNull
    private static SparkServerlessClusterProvisionSettingsModel validateDataCompleteness(
            @NotNull SparkServerlessClusterProvisionSettingsModel toUpdate) {
        if (!StringUtils.isEmpty(toUpdate.getErrorMessage())) {
            return toUpdate;
        }

        // TODO: Check all of the necessary fields
        String clusterName = toUpdate.getClusterName();
        String adlAccount = toUpdate.getAdlAccount();
        String sparkEvents = toUpdate.getSparkEvents();
        int workerNumberOfContainers = toUpdate.getWorkerNumberOfContainers();

        if (StringHelper.isNullOrWhiteSpace(clusterName) ||
                StringHelper.isNullOrWhiteSpace(adlAccount) ||
                StringHelper.isNullOrWhiteSpace(sparkEvents) ||
                StringHelper.isNullOrWhiteSpace(String.valueOf(workerNumberOfContainers))) {
            String highlightPrefix = "* ";
            if (!toUpdate.getAdlAccountLabelTitle().startsWith(highlightPrefix)) {
                toUpdate.setAdlAccountLabelTitle(highlightPrefix + toUpdate.getAdlAccountLabelTitle());
            }
            if (!toUpdate.getClusterNameLabelTitle().startsWith(highlightPrefix)) {
                toUpdate.setClusterNameLabelTitle(highlightPrefix + toUpdate.getClusterNameLabelTitle());
            }
            if (!toUpdate.getSparkEventsLabelTitle().startsWith(highlightPrefix)) {
                toUpdate.setSparkEventsLabelTitle(
                        highlightPrefix + toUpdate.getSparkEventsLabelTitle());
            }
            if (!toUpdate.getWorkerNumberOfContainersLabelTitle().startsWith(highlightPrefix)) {
                toUpdate.setWorkerNumberOfContainersLabelTitle(
                        highlightPrefix + toUpdate.getWorkerNumberOfContainersLabelTitle());
            }
            return toUpdate.setErrorMessage("All (*) fields are required.");
        }

        return toUpdate;
    }

    @NotNull
    private static SparkServerlessClusterProvisionSettingsModel validateNumericField(
            @NotNull SparkServerlessClusterProvisionSettingsModel toUpdate) {
        if (!StringUtils.isEmpty(toUpdate.getErrorMessage())) {
            return toUpdate;
        }

        int masterCores = toUpdate.getMasterCores();
        int masterMemory = toUpdate.getMasterMemory();
        int workerCores = toUpdate.getWorkerCores();
        int workerMemory = toUpdate.getWorkerMemory();
        int workerNumberOfContainers = toUpdate.getWorkerNumberOfContainers();

        // TODO: Only workerNumberOfContainers field numeric check will be reserved finally
        // TODO: Determine whether workerNumberOfContainers is in legal range
        if (masterCores <= 0 ||
                masterMemory <= 0 ||
                workerCores <= 0 ||
                workerMemory <= 0 ||
                workerNumberOfContainers <= 0) {
            String highlightPrefix = "* ";
            if (!toUpdate.getWorkerNumberOfContainersLabelTitle().startsWith(highlightPrefix)) {
                toUpdate.setWorkerNumberOfContainersLabelTitle(
                        highlightPrefix + toUpdate.getWorkerNumberOfContainersLabelTitle());
            }
            return toUpdate.setErrorMessage("All (*) fields should be positive numbers.");
        }
        return toUpdate;
    }

    @NotNull
    private SparkServerlessClusterProvisionSettingsModel provisionCluster(
            @NotNull SparkServerlessClusterProvisionSettingsModel toUpdate) {
        if (!StringUtils.isEmpty(toUpdate.getErrorMessage())) {
            return toUpdate;
        }

        try {
            AzureSparkServerlessCluster cluster =
                    (AzureSparkServerlessCluster) new AzureSparkServerlessCluster.Builder(account)
                            .name(toUpdate.getClusterName())
                            .masterPerInstanceCores(toUpdate.getMasterCores())
                            .masterPerInstanceMemory(toUpdate.getMasterMemory())
                            .workerPerInstanceCores(toUpdate.getWorkerCores())
                            .workerPerInstanceMemory(toUpdate.getWorkerMemory())
                            .workerInstances(toUpdate.getWorkerNumberOfContainers())
                            .sparkEventsPath(toUpdate.getStorageRootPathLabelTitle() + toUpdate.getSparkEvents())
                            .userStorageAccount(account.getDetailResponse().defaultDataLakeStoreAccount())
                            .build().provision().toBlocking().single();
        } catch (Exception e) {
            return toUpdate.setErrorMessage("Provision failed: " + e.getMessage());
        }
        return toUpdate;
    }

    private static SparkServerlessClusterProvisionSettingsModel resetLabels(
            @NotNull SparkServerlessClusterProvisionSettingsModel toUpdate) {
        String highlightPrefix = "* ";
        if (toUpdate.getClusterNameLabelTitle().startsWith(highlightPrefix)) {
            toUpdate.setClusterNameLabelTitle(toUpdate.getClusterNameLabelTitle().substring(2));
        }
        if (toUpdate.getAdlAccountLabelTitle().startsWith(highlightPrefix)) {
            toUpdate.setAdlAccountLabelTitle(toUpdate.getAdlAccountLabelTitle().substring(2));
        }
        if (toUpdate.getSparkEventsLabelTitle().startsWith(highlightPrefix)) {
            toUpdate.setSparkEventsLabelTitle(toUpdate.getSparkEventsLabelTitle().substring(2));
        }
        if (toUpdate.getWorkerNumberOfContainersLabelTitle().startsWith(highlightPrefix)) {
            toUpdate.setWorkerNumberOfContainersLabelTitle(
                    toUpdate.getWorkerNumberOfContainersLabelTitle().substring(2));
        }
        return toUpdate;
    }

        public Observable<SparkServerlessClusterProvisionSettingsModel> validateAndProvision() {
        // TODO: AU adequation check
        return Observable.just(new SparkServerlessClusterProvisionSettingsModel())
                .doOnNext(controllableView::getData)
                .observeOn(ideSchedulers.processBarVisibleAsync("Validating the cluster settings..."))
                // Validation check one by one. If one check failed, we will stop other checks in the entry
                // of the validation function.
                .map(toUpdate -> toUpdate.setErrorMessage(null))
                .map(SparkServerlessClusterProvisionCtrlProvider::resetLabels)
                .map(SparkServerlessClusterProvisionCtrlProvider::validateDataCompleteness)
                .map(toUpdate -> validateClusterNameUniqueness(toUpdate))
                .map(SparkServerlessClusterProvisionCtrlProvider::validateNumericField)
                .map(toUpdate -> provisionCluster(toUpdate))
                .observeOn(ideSchedulers.dispatchUIThread())
                .doOnNext(controllableView::setData)
                .filter(data -> StringUtils.isEmpty(data.getErrorMessage()));
    }
}
