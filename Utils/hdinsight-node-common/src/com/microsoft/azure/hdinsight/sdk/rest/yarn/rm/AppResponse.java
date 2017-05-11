package com.microsoft.azure.hdinsight.sdk.rest.yarn.rm;

import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;

public class AppResponse implements IConvertible {
    private App app;

    public App getApp() {
        return app;
    }

    public void setApp(App app) {
        this.app = app;
    }
}
