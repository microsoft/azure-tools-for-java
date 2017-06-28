package com.microsoft.azuretools.container.views;

import com.microsoft.azuretools.azurecommons.mvp.ui.base.MvpView;

public interface StepOnePageView extends MvpView{
    void showInfomation(String string);

    void fillRegistryInfo(String registryUrl, String username, String password);

    void setWidgetsEnabledStatus(boolean enableStatus);
    
    void setCompleteStatus(boolean flag);
    
}
