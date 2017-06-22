package com.microsoft.tooling.msservices.serviceexplorer.azure.rediscache;

import com.microsoft.azuretools.azurecommons.mvp.ui.base.MvpView;

public interface RedisExplorerMvpView extends MvpView {
    
    void onReadRedisDatabaseNum(String sid, String id);
    
    void renderDbCombo(int num);
}
