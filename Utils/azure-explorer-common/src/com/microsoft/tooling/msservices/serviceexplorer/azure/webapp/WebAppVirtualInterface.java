package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

interface WebAppVirtualInterface {
    String getWebAppId();

    String getWebAppName();

    String getRunState();

    void stopWebApp();

    void startWebApp();

    void restartWebApp();
}
