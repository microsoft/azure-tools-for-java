package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

interface WebAppVirtualInterface {
    String getSubscriptionId();

    String getWebAppId();

    String getWebAppName();

    String getRunState();

    void setRunState(String runState);

    void stopWebApp();

    void startWebApp();

    void restartWebApp();
}
