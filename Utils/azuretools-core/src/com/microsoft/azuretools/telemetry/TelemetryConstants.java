/*
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

package com.microsoft.azuretools.telemetry;

import java.util.HashMap;
import java.util.Map;

public class TelemetryConstants {

    // production name
    public static final String WEBAPP = "webapp";
    public static final String FUNCTION = "function";
    public static final String SPRING_CLOUD = "springcloud";
    public static final String ACR = "acr";
    public static final String ARM = "arm";
    public static final String DOCKER = "docker";
    public static final String VM = "vm";
    public static final String REDIS = "redis";
    public static final String STORAGE = "storage";
    public static final String ACCOUNT = "account";
    public static final String SYSTEM = "system";
    public static final String ACTION = "action";
    public static final String DIALOG = "dialog";
    public static final String HDINSIGHT = "hdinsight";
    public static final String SPARK_ON_COSMOS = "sparkOnCosmos";
    public static final String SPARK_ON_COSMOS_SERVERLESS = "sparkOnCosmosServerless";
    public static final String SPARK_ON_SQL_SERVER = "sparkOnSqlServer";
    public static final String SPARK_ON_ARCADIA = "sparkOnArcadia";
    public static final String SPARK_FAILURE_TASK_DEBUG = "sparkFailureTaskDebug";
    public static final String VFS = "virtualFileSystem";

    // operation value
    public static final String SURVEY = "survey";
    public static final String FEEDBACK = "feedback";
    public static final String AZURECODE_SAMPLES = "azure-code-samples";
    public static final String LIB_CONFIGURATION = "libraries-configuration";
    public static final String PLUGIN_INSTALL = "install-plugin";
    public static final String PLUGIN_LOAD = "load-plugin";
    public static final String PLUGIN_UPGRADE = "upgrade-plugin";
    public static final String PLUGIN_UNINSTALL = "uninstall-plugin";
    public static final String TELEMETRY_ALLOW = "allow-telemetry";
    public static final String TELEMETRY_DENY = "deny-telemetry";
    public static final String CREATE_WEBAPP = "create-webapp";
    public static final String DELETE_WEBAPP = "delete-webapp";
    public static final String DEPLOY_WEBAPP = "deploy-webapp";
    public static final String START_WEBAPP = "start-webapp";
    public static final String STOP_WEBAPP = "stop-webapp";
    public static final String RESTART_WEBAPP = "restart-webapp";
    public static final String WEBAPP_SHOWPROP = "showprop-webapp";
    public static final String WEBAPP_OPEN_INBROWSER = "open-inbrowser-webapp";
    public static final String TRIGGER_FUNCTION = "trigger-function";
    public static final String ENABLE_FUNCTION = "enable-function";
    public static final String DISABLE_FUNCTION = "disable-function";
    public static final String RUN_FUNCTION_APP = "run-function-app";
    public static final String CREATE_FUNCTION_TRIGGER = "create-function-trigger";
    public static final String CREATE_FUNCTION_PROJECT = "create-function-project";
    public static final String CREATE_FUNCTION_APP = "create-function-app";
    public static final String DEPLOY_FUNCTION_APP = "deploy-function-app";
    public static final String DELETE_FUNCTION_APP = "delete-function-app";
    public static final String START_FUNCTION_APP = "start-function-app";
    public static final String STOP_FUNCTION_APP = "stop-function-app";
    public static final String RESTART_FUNCTION_APP = "restart-function-app";
    public static final String SHOWPROP_FUNCTION_APP = "showprop-function-app";
    public static final String OPEN_INBROWSER_FUNCTION_APP = "open-inbrowser-function-app";

    public static final String CREATE_SPRING_CLOUD_APP = "create-springcloud-app";
    public static final String DEPLOY_SPRING_CLOUD_APP = "deploy-springcloud-app";
    public static final String DELETE_SPRING_CLOUD_APP = "delete-springcloud-app";
    public static final String START_SPRING_CLOUD_APP = "start-springcloud-app";
    public static final String STOP_SPRING_CLOUD_APP = "stop-springcloud-app";
    public static final String RESTART_SPRING_CLOUD_APP = "restart-springcloud-app";
    public static final String SHOWPROP_SPRING_CLOUD_APP = "showprop-springcloud-app";
    public static final String OPEN_IN_BROWSER_SPRING_CLOUD_APP = "open-inbrowser-springcloud-app";
    public static final String OPEN_IN_PORTAL_SPRING_CLOUD_APP = "open-inportal-springcloud-app";

    public static final String CREATE_DOCKER_FILE = "create-dockerfile";
    public static final String CREATE_DOCKER_HOST = "create-docker-host";
    public static final String DEPLOY_DOCKER_HOST = "deploy-docker-host";
    public static final String RESTART_DOCKER_HOST = "restart-docker-host";
    public static final String SHUTDOWN_DOCKER_HOST = "shutdown-docker-host";
    public static final String START_DOCKER_HOST = "start-docker-host";
    public static final String START_DOCKER_CONTAINER = "start-docker-container";
    public static final String RESTART_DOCKER_CONTAINER = "restart-docker-container";
    public static final String STOP_DOCKER_CONTAINER = "stop-docker-container";
    public static final String BROWSE_DOCKER_CONTAINER = "browser-docker-container";
    public static final String DELETE_DOCKER_CONTAINER = "delete-docker-container";
    public static final String DELETE_DOCKER_IMAGE = "delete-docker-image";
    public static final String DEPLOY_WEBAPP_DOCKERLOCAL = "deploy-webapp-dockerlocal";
    public static final String DEPLOY_WEBAPP_DOCKERHOST = "deploy-webapp-dockerhost";
    public static final String DEPLOY_WEBAPP_CONTAINER = "deploy-webapp-container";
    public static final String DEPLOY_WEBAPP_SLOT = "deploy-webapp-slot";
    public static final String CREATE_WEBAPP_SLOT = "create-webapp-slot";
    public static final String STOP_WEBAPP_SLOT = "stop-webapp-slot";
    public static final String START_WEBAPP_SLOT = "start-webapp-slot";
    public static final String RESTART_WEBAPP_SLOT = "restart-webapp-slot";
    public static final String DELETE_WEBAPP_SLOT = "delete-webapp-slot";
    public static final String SWAP_WEBAPP_SLOT = "swap-webapp-slot";
    public static final String SHOW_WEBAPP_SLOT_PROP = "show-webapp-slot-prop";
    public static final String OPERN_WEBAPP_SLOT_BROWSER = "open-webappslot-inbrowser";
    public static final String OPEN_CREATEWEBAPP_DIALOG = "open-create-webapp-dialog";
    public static final String REFRESH_METADATA = "refresh";
    public static final String SIGNIN = "signin";
    public static final String SIGNOUT = "signout";
    public static final String SELECT_SUBSCRIPTIONS = "select-subscriptions";
    public static final String GET_SUBSCRIPTIONS = "get-subscriptions";
    public static final String REPORT_ISSUES = "report-issues";
    public static final String CREATE_VM = "create-vm";
    public static final String DELETE_VM = "delete-vm";
    public static final String SHUTDOWN_VM = "shutdown-vm";
    public static final String START_VM = "start-vm";
    public static final String RESTART_VM = "restart-vm";
    public static final String CREATE_STORAGE_ACCOUNT = "create-storage-account";
    public static final String OPEN_STORAGE_IN_PORTAL = "open-storage-in-portal";
    public static final String DELETE_STORAGE_ACCOUNT = "delete-storage-account";
    public static final String DETACH_STORAGE_ACCOUNT = "detach-storage-account";
    public static final String DELETE_STORAGE_TABLE = "delete-storage-table";
    public static final String CREATE_BLOB_CONTAINER = "create-blob-container";
    public static final String UPLOAD_BLOB_FILE = "upload-blob-file";
    public static final String DELETE_BLOB_CONTAINER = "delete-blob-container";
    public static final String BLOB_COPYURL = "copyurl-blob";
    public static final String BLOB_DELETE = "delete-blob";
    public static final String BLOB_OPEN = "open-blob";
    public static final String BLOB_SAVEAS = "saveas-blob";
    public static final String BLOB_UPLOAD = "upload-blob";
    public static final String BLOB_SEARCH = "search-blob";
    public static final String CREATE_REDIS = "create-redis";
    public static final String DELETE_REDIS = "delete-redis";
    public static final String REDIS_SCAN = "scan-redis";
    public static final String REDIS_GET = "get-redis";
    public static final String REDIS_READPROP = "readprop-redis";
    public static final String REDIS_OPEN_EXPLORER = "open-explorer-redis";
    public static final String REDIS_OPEN_BROWSER = "open-browser-redis";
    public static final String ACR_PUSHIMAGE = "pushimage-acr";
    public static final String ACR_OPEN_INBROWSER = "open-inbrowser-acr";
    public static final String ACR_OPEN_EXPLORER = "open-explorer-acr";
    public static final String ACR_READPROP = "readprop-acr";
    public static final String SIGNIN_DC = "dc";
    public static final String SIGNIN_SP = "sp";
    public static final String CHOOSE_REFERENCE_JAR_GEN2 = "choose-reference-jar-gen2";
    public static final String CREATE_DEPLOYMENT = "create-deployment";
    public static final String UPDATE_DEPLOYMENT = "update-deployment";
    public static final String DELETE_DEPLOYMENT = "delete-deployment";
    public static final String DELETE_RESOURCE_GROUP = "delete-resource-group";
    public static final String EXPORT_TEMPALTE_FILE = "export-template-file";
    public static final String SHOW_DEPLOYMENT_PROPERTY = "show-deployment-property";
    public static final String VIEW_TEMPALTE_FILE = "view-template-file";
    public static final String UPDATE_DEPLOYMENT_SHORTCUT = "update-deployment-shortcut";
    public static final String BROWSE_TEMPLATE_SAMPLES = "browse-template-samples";
    public static final String ACTIVATE_TEMPLATE_DEITING = "activate-template-editing";
    public static final String RUN_LOCAL_SPARK_JOB = "run-local-spark-job";
    public static final String RUN_REMOTE_SPARK_JOB = "run-remote-spark-job";
    public static final String DEBUG_LOCAL_SPARK_JOB = "debug-local-spark-job";
    public static final String DEBUG_REMOTE_SPARK_JOB = "debug-remote-spark-job";
    public static final String SELECT_DEFAULT_SPARK_APPLICATION_TYPE = "select-default-spark-application-type";
    public static final String UNLINK_SPARK_CLUSTER = "unlink-spark-cluster";
    public static final String REFRESH_JOB_VIEW_TABLE = "refresh-job-view-table";
    public static final String CLOSE_JOB_VIEW_WINDOW = "close-job-view-window";
    public static final String SELECT_JOB_IN_JOB_VIEW_WINDOW = "select-job-in-job-view-window";
    public static final String CREATE_NEW_RUN_CONFIG = "create-new-run-config";
    public static final String RUN_SPARK_LIVY_INTERACTIVE_CONSOLE = "run-spark-livy-interactive-console";
    public static final String RUN_SPARK_LOCAL_CONSOLE = "run-spark-local-console";
    public static final String PROVISION_A_CLUSTER = "provision-a-cluster";
    public static final String DELETE_A_CLUSTER = "delete-a-cluster";
    public static final String UPDATE_A_CLUSTER = "update-a-cluster";

    // property name
    public static final String WEBAPP_DEPLOY_TO_SLOT = "webappDeployToSlot";
    public static final String RUNTIME = "runtime";
    public static final String CREATE_NEWASP = "createNewAsp";
    public static final String CREATE_NEWWEBAPP = "createNewWebapp";
    public static final String CREATE_NEWRG = "createNewRg";
    public static final String SUBSCRIPTIONID = "subscriptionId";
    public static final String FILETYPE = "fileType";
    public static final String ARTIFACT_UPLOAD_COUNT = "artifactUploadCount";
    public static final String JAVA_APPNAME = "javaAppName";
    public static final String SIGNIN_METHOD = "signinMethod";

    public static final Map<String, String> signInSPProp = new HashMap<>();
    public static final Map<String, String> signInDCProp = new HashMap<>();

    static {
        signInSPProp.put(SIGNIN_METHOD, SIGNIN_SP);
        signInDCProp.put(SIGNIN_METHOD, SIGNIN_DC);
    }
}
