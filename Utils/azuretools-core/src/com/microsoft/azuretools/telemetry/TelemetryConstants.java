package com.microsoft.azuretools.telemetry;

public class TelemetryConstants {
    // production name
    public static final String PRODUCTION_NAME_WEBAPP = "webapp";
    public static final String PRODUCTION_NAME_ACR = "acr";
    public static final String PRODUCTION_NAME_DOCKER = "docker";

    // operation name
    public static final String CREATE_WEB_APP = "create-webapp";
    public static final String DELETE_WEB_APP = "delete-webapp";
    public static final String DEPLOY_WEB_APP = "deploy-webapp";
    public static final String DEPLOY_DEPLOYMENT_SLOT = "deploy-to-slot";
    public static final String CREATE_DEPLOYMENT_SLOT = "create-deployment-slot";
    public static final String OPEN_CREATEWEBAPP_DIALOG = "open-create-webapp-dialog";
    public static final String REFRESH_METADATA = "refresh";
}
