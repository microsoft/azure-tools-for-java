package com.microsoft.azure.sparkserverless.serverexplore;

import com.microsoft.azure.hdinsight.common.mvc.IdeSchedulers;
import com.microsoft.azure.hdinsight.common.mvc.SettableControl;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.StringHelper;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;

public class SparkServerlessClusterProvisionCtrlProvider {

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

    public Observable<SparkServerlessClusterProvisionSettingsModel> validateAndProvision() {
        return Observable.just(new SparkServerlessClusterProvisionSettingsModel())
                .doOnNext(controllableView::getData)
                .observeOn(ideSchedulers.processBarVisibleAsync("Validating the cluster settings..."))
                .map(toUpdate -> {
                    String clusterName = toUpdate.getClusterName();
                    String adlAccount = toUpdate.getAdlAccount();
                    // TODO: Check all of the necessary fields
                    // Incomplete data check
                    if (StringHelper.isNullOrWhiteSpace(clusterName) ||
                            StringHelper.isNullOrWhiteSpace(adlAccount)) {
                        String highlightPrefix = "* ";

                        if (!toUpdate.getAdlAccountLabelTitle().startsWith(highlightPrefix)) {
                            toUpdate.setAdlAccountLabelTitle(highlightPrefix + toUpdate.getAdlAccountLabelTitle());
                        }

                        if (!toUpdate.getClusterNameLabelTitle().startsWith(highlightPrefix)) {
                            toUpdate.setClusterNameLabelTitle(highlightPrefix + toUpdate.getClusterNameLabelTitle());
                        }

                        return toUpdate.setErrorMessage("All (*) fields are required.");
                    }

                    // TODO: lots of check

                    // No issue
                    // TODO: create a spark serverless cluster here

                    return toUpdate.setErrorMessage(null);
                })
                .observeOn(ideSchedulers.dispatchUIThread())
                .doOnNext(controllableView::setData)
                .filter(data -> StringUtils.isEmpty(data.getErrorMessage()));
    }
}
