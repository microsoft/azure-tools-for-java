package com.microsoft.azuretools.container.views;

import java.util.List;

import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azuretools.azurecommons.mvp.ui.base.MvpView;

public interface StepTwoPageView extends MvpView {
    public void finishLoading(List<SiteInner> wal);

    void setWidgetsEnabledStatus(boolean enableStatus);

    void showLoading();
}
