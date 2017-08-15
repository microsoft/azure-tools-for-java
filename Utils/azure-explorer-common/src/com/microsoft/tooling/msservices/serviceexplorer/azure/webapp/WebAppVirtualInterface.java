package com.microsoft.tooling.msservices.serviceexplorer.azure.webapp;

public interface WebAppVirtualInterface {
    default String getWebAppId(){
        return null;
    }
    default String getWebAppName(){
        return null;
    }
    default void stopWebApp(){}
    default void startWebApp(){}
    default void restartWebApp(){}
}
