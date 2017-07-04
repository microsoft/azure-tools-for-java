/**
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

package com.microsoft.azuretools.container.presenters;

import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.container.DockerRuntime;
import com.microsoft.azuretools.container.utils.WebAppOnLinuxUtil;
import com.microsoft.azuretools.container.views.StepTwoPageView;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.CanceledByUserException;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import rx.Observable;
import rx.schedulers.Schedulers;

public class StepTwoPagePresenter<V extends StepTwoPageView> extends MvpPresenter<V> {
    final private List<SubscriptionDetail> binderSubscriptionDetails = new ArrayList<SubscriptionDetail>();
    final private List<ResourceGroup> binderResourceGroup = new ArrayList<ResourceGroup>();
    final private List<SiteInner> binderWebAppOnLinux = new ArrayList<>();

    public void onChangeSubscription(int index) {
        doFetchResourceGroup(binderSubscriptionDetails.get(index));
        getMvpView().fillResourceGroups(binderResourceGroup);
    }

    public void onLoadSubsAndRGs() {
        try {
            doFetchSubscriptions();
            if (binderSubscriptionDetails.size() > 0) {
                doFetchResourceGroup(binderSubscriptionDetails.get(0));
            }
            getMvpView().fillSubscriptions(binderSubscriptionDetails);
            getMvpView().fillResourceGroups(binderResourceGroup);
        } catch (Exception ex) {
            ex.printStackTrace();
            getMvpView().onErrorWithException(ex.getMessage(), ex);
        }
    }

    public void onRefreshWebAppsOnLinux() {
        getMvpView().onRequestPending();
        Observable.fromCallable(() -> {
            updateWebAppOnLinuxList();
            return null;
        }).subscribeOn(Schedulers.io()).subscribe(res -> {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                doFetchSubscriptions();
                if (binderSubscriptionDetails.size() > 0) {
                    doFetchResourceGroup(binderSubscriptionDetails.get(0));
                }
                V v = getMvpView();
                if (v != null) {
                    v.fillWebApps(binderWebAppOnLinux);
                    v.fillSubscriptions(binderSubscriptionDetails);
                    v.fillResourceGroups(binderResourceGroup);
                    v.onRequestSucceed();
                }
            });
        }, e -> {
            V v = getMvpView();
            if (v != null) {
                v.onRequestFail("onRefreshWebAppsOnLinux@StepTwoPagePresenter");
            }
        });
    }

    // used for 1st-time retrieving list
    public void onListWebAppsOnLinux() {
        Observable.fromCallable(() -> {
            updateWebAppOnLinuxList();
            return null;
        }).subscribeOn(Schedulers.io()).subscribe(res -> {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                doFetchSubscriptions();
                if (binderSubscriptionDetails.size() > 0) {
                    doFetchResourceGroup(binderSubscriptionDetails.get(0));
                }
                V v = getMvpView();
                if (v != null) {
                    v.fillWebApps(binderWebAppOnLinux);
                    v.fillSubscriptions(binderSubscriptionDetails);
                    v.fillResourceGroups(binderResourceGroup);
                }
            });
        }, e -> {
            V v = getMvpView();
            if (v != null) {
                v.onRequestFail("onListWebAppsOnLinux@StepTwoPagePresenter");
            }
        });
    }

    public void onDeployToExisitingWebApp(int selectionIndex) {
        WebApp app = (WebApp) this.binderWebAppOnLinux.get(selectionIndex);
        WebAppOnLinuxUtil.deployToExisting(app);
    }

    public void onDeployToNewWebApp(String appName, int subscriptionSelectionIndex, String resourceGroupName,
            boolean createNewRg) throws IOException {
        getMvpView().onRequestPending();
        Observable.fromCallable(() -> {
            return WebAppOnLinuxUtil.deployToNew(
                    binderSubscriptionDetails.get(subscriptionSelectionIndex).getSubscriptionId(), resourceGroupName,
                    appName, createNewRg);
        }).subscribeOn(Schedulers.io()).subscribe(app -> {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                DockerRuntime.getInstance().setLatestWebAppName(appName);
                V v = getMvpView();
                if (v != null) {
                    v.onRequestSucceed();
                    v.finishDeploy();
                }
            });
        }, e -> {
            V v = getMvpView();
            if (v != null) {
                v.onRequestFail("onDeployNew@StepTwoPopupDialogPresenter");
            }
        });
    }

    // private helpers
    private void updateWebAppOnLinuxList() throws AuthException, IOException, CanceledByUserException {

        binderWebAppOnLinux.clear();
        for (SiteInner si : WebAppOnLinuxUtil.listAllWebAppOnLinux(true)) {
            binderWebAppOnLinux.add(si);
        }
    }

    private void doFetchResourceGroup(SubscriptionDetail subs) {
        binderResourceGroup.clear();
        for (ResourceGroup rg : AzureModel.getInstance().getSubscriptionToResourceGroupMap().get(subs)) {
            binderResourceGroup.add(rg);
        }
    }

    private void doFetchSubscriptions() {
        if (AzureModel.getInstance().getSubscriptionToResourceGroupMap() == null) {
            return;
        }
        Set<SubscriptionDetail> sdl = AzureModel.getInstance().getSubscriptionToResourceGroupMap().keySet();
        if (sdl == null) {
            System.out.println("sdl is null");
            return;
        }
        binderSubscriptionDetails.clear();
        for (SubscriptionDetail sd : sdl) {
            if (sd.isSelected()) {
                binderSubscriptionDetails.add(sd);
            }
        }
    }
}
