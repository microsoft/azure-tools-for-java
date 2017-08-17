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

import com.microsoft.azure.management.appservice.WebApp;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.adauth.AuthException;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.container.DockerRuntime;
import com.microsoft.azuretools.container.utils.RequestPayload;
import com.microsoft.azuretools.container.utils.WebAppOnLinuxUtil;
import com.microsoft.azuretools.container.views.StepTwoPageView;
import com.microsoft.azuretools.core.mvp.ui.base.MvpPresenter;
import com.microsoft.azuretools.utils.AzureModel;
import com.microsoft.azuretools.utils.AzureUIRefreshCore;
import com.microsoft.azuretools.utils.AzureUIRefreshEvent;
import com.microsoft.azuretools.utils.CanceledByUserException;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import rx.Observable;

public class StepTwoPagePresenter<V extends StepTwoPageView> extends MvpPresenter<V> {
    private static final String TEXT_LISTING_AEB_APP_ON_LINUX = "List Web App on Linux";
    private static final String TEXT_DEPLOYING_TO_NEW_WEB_APP = "Deploy to new Web App on Linux";
    private static final String TEXT_DEPLOYING_TO_EXISTING_WEB_APP = "Deploy to existing Web App on Linux";
    private final List<SubscriptionDetail> binderSubscriptionDetails = new ArrayList<SubscriptionDetail>();
    private final List<ResourceGroup> binderResourceGroup = new ArrayList<ResourceGroup>();
    private final List<SiteInner> binderWebAppOnLinux = new ArrayList<>();

    /**
     * Action on subscription change.
     * 
     * @param index
     *            subscription index on selection
     */
    public void onChangeSubscription(int index) {
        doFetchResourceGroup(binderSubscriptionDetails.get(index));
        getMvpView().fillResourceGroups(binderResourceGroup);
    }

    /**
     * used for refreshing list.
     */
    public void onRefreshWebAppsOnLinux() {
        getMvpView().onRequestPending(TEXT_LISTING_AEB_APP_ON_LINUX);
        Observable.fromCallable(() -> {
            updateWebAppOnLinuxList();
            doFetchSubscriptions();
            if (binderSubscriptionDetails.size() > 0) {
                doFetchResourceGroup(binderSubscriptionDetails.get(0));
            }
            return null;
        }).subscribeOn(getSchedulerProvider().io()).subscribe(res -> {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                V v = getMvpView();
                v.fillWebApps(binderWebAppOnLinux);
                v.fillSubscriptions(binderSubscriptionDetails);
                v.fillResourceGroups(binderResourceGroup);
                v.onRequestSucceed(new RequestPayload(TEXT_LISTING_AEB_APP_ON_LINUX, null));
            });
        }, err -> {
            System.err.println("onRefreshWebAppsOnLinux@StepTwoPagePresenter ");
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                V v = getMvpView();
                v.onRequestFail(TEXT_LISTING_AEB_APP_ON_LINUX);
                v.onErrorWithException(TEXT_LISTING_AEB_APP_ON_LINUX, (Exception) err);
            });
        });
    }

    /**
     * used for first time retrieving list.
     */
    public void onListWebAppsOnLinux() {
        Observable.fromCallable(() -> {
            updateWebAppOnLinuxList();
            doFetchSubscriptions();
            if (binderSubscriptionDetails.size() > 0) {
                doFetchResourceGroup(binderSubscriptionDetails.get(0));
            }
            return null;
        }).subscribeOn(getSchedulerProvider().io()).subscribe(res -> {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                V v = getMvpView();
                v.fillWebApps(binderWebAppOnLinux);
                v.fillSubscriptions(binderSubscriptionDetails);
                v.fillResourceGroups(binderResourceGroup);
            });
        }, err -> {
            System.err.println("onListWebAppsOnLinux@StepTwoPagePresenter ");
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                V v = getMvpView();
                v.onRequestFail(TEXT_LISTING_AEB_APP_ON_LINUX);
                v.onErrorWithException(TEXT_LISTING_AEB_APP_ON_LINUX, (Exception) err);
            });
        });
    }

    /**
     * Deploy to existing Web App on Linux.
     * 
     * @param selectionIndex
     *            Web App index on selection
     */
    public void onDeployToExisitingWebApp(int selectionIndex) {
        getMvpView().onRequestPending(TEXT_DEPLOYING_TO_EXISTING_WEB_APP);
        Observable.fromCallable(() -> {
            SiteInner si = binderWebAppOnLinux.get(selectionIndex);
            WebApp app = WebAppOnLinuxUtil.deployToExisting(si);
            DockerRuntime.getInstance().setLatestWebAppName(app.name());
            return app;
        }).subscribeOn(getSchedulerProvider().io()).subscribe(empty -> {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                V v = getMvpView();
                v.onRequestSucceed(new RequestPayload(TEXT_DEPLOYING_TO_EXISTING_WEB_APP,
                        String.format("http://%s.azurewebsites.net/%s",
                                DockerRuntime.getInstance().getLatestWebAppName(),
                                DockerRuntime.getInstance().getLatestArtifactName())));
                v.finishDeploy();
            });
        }, err -> {
            System.err.println("onDeployToExisitingWebApp@StepTwoPopupDialogPresenter");
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                V v = getMvpView();
                v.onRequestFail(TEXT_DEPLOYING_TO_EXISTING_WEB_APP);
                v.onErrorWithException(TEXT_DEPLOYING_TO_EXISTING_WEB_APP, (Exception) err);
            });
        });
    }

    /**
     * Create new Web App on Linux and deploy on it.
     * 
     * @param appName
     * @param subscriptionSelectionIndex
     * @param resourceGroupName
     * @param createNewRg
     *            flag indicating whether resource group should be created.
     * @throws IOException
     */
    public void onDeployToNewWebApp(String appName, int subscriptionSelectionIndex, String resourceGroupName,
            boolean createNewRg) throws IOException {
        getMvpView().onRequestPending(TEXT_DEPLOYING_TO_NEW_WEB_APP);
        Observable.fromCallable(() -> {
            WebApp app = WebAppOnLinuxUtil.deployToNew(
                    binderSubscriptionDetails.get(subscriptionSelectionIndex).getSubscriptionId(), resourceGroupName,
                    appName, createNewRg);
            DockerRuntime.getInstance().setLatestWebAppName(app.name());
            AzureUIRefreshCore.execute(new AzureUIRefreshEvent(AzureUIRefreshEvent.EventType.REFRESH, null));
            return app;
        }).subscribeOn(getSchedulerProvider().io()).subscribe(app -> {
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                V v = getMvpView();
                v.onRequestSucceed(new RequestPayload(TEXT_DEPLOYING_TO_NEW_WEB_APP,
                        String.format("http://%s.azurewebsites.net/%s",
                                DockerRuntime.getInstance().getLatestWebAppName(),
                                DockerRuntime.getInstance().getLatestArtifactName())));
                v.finishDeploy();
            });
        }, err -> {
            System.err.println("onDeployNew@StepTwoPopupDialogPresenter ");
            DefaultLoader.getIdeHelper().invokeLater(() -> {
                if (isViewDetached()) {
                    return;
                }
                V v = getMvpView();
                v.onRequestFail(TEXT_DEPLOYING_TO_NEW_WEB_APP);
                v.onErrorWithException(TEXT_DEPLOYING_TO_NEW_WEB_APP, (Exception) err);
            });
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
