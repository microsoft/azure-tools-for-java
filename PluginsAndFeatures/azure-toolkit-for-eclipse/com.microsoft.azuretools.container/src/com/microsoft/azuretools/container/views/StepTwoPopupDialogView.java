package com.microsoft.azuretools.container.views;

import java.util.List;

import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azuretools.authmanage.models.SubscriptionDetail;
import com.microsoft.azuretools.azurecommons.mvp.ui.base.MvpView;

public interface StepTwoPopupDialogView extends MvpView{

    void fillSubscriptions(List<SubscriptionDetail> sdl);

    void fillResourceGroups(List<ResourceGroup> rgl);
    
    void finishDeploy();

}
