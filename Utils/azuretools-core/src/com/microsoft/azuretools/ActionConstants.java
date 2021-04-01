/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools;

import com.google.common.base.Preconditions;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public final class ActionConstants {

    private static final String SEPARATOR = ".";
    private static final String OPERATION_SEPARATOR = "-";

    public static ActionEntity parse(String action) {
        String[] params = StringUtils.split(action, SEPARATOR);
        Preconditions.checkArgument(Objects.nonNull(params) && params.length == 2);
        return new ActionEntity(params[0], params[1]);
    }

    public interface WebApp {
        String MODULE = "webapp";
        String OPERATION_PREFIX = MODULE + SEPARATOR;

        String CREATE = OPERATION_PREFIX + "create-webapp";
        String DELETE = OPERATION_PREFIX + "delete-webapp";
        String DEPLOY = OPERATION_PREFIX + "deploy-webapp";
        String START = OPERATION_PREFIX + "start-webapp";
        String STOP = OPERATION_PREFIX + "stop-webapp";
        String RESTART = OPERATION_PREFIX + "restart-webapp";
        String SHOW_PROPERTIES = OPERATION_PREFIX + "showprop-webapp";
        String SHINTO = OPERATION_PREFIX + "sshinto-webapp";
        String OPEN_IN_PORTAL = OPERATION_PREFIX + "webapp-open-in-portal";
        String OPEN_IN_BROWSER = OPERATION_PREFIX + "open-inbrowser-webapp";
        String START_STREAMING_LOG = OPERATION_PREFIX + "start-streaming-log-webapp";
        String STOP_STREAMING_LOG = OPERATION_PREFIX + "stop-streaming-log-webapp";

        String DEPLOY_DOCKERLOCAL = OPERATION_PREFIX + "deploy-webapp-dockerlocal";
        String DEPLOY_DOCKERHOST = OPERATION_PREFIX + "deploy-webapp-dockerhost";
        String DEPLOY_CONTAINER = OPERATION_PREFIX + "deploy-webapp-container";
        // deployment slot
        String OPEN_CREATEWEBAPP_DIALOG = OPERATION_PREFIX + "open-create-webapp-dialog";

        String DEPLOY_TO_SLOT = OPERATION_PREFIX + "webappDeployToSlot";
        String CREATE_NEWWEBAPP = OPERATION_PREFIX + "createNewWebapp";

        interface DeploymentSlot {
            String DEPLOY = OPERATION_PREFIX + "deploy-webapp-slot";
            String CREATE = OPERATION_PREFIX + "create-webapp-slot";
            String STOP = OPERATION_PREFIX + "stop-webapp-slot";
            String START = OPERATION_PREFIX + "start-webapp-slot";
            String RESTART = OPERATION_PREFIX + "restart-webapp-slot";
            String DELETE = OPERATION_PREFIX + "delete-webapp-slot";
            String SWAP = OPERATION_PREFIX + "swap-webapp-slot";
            String SHOW_PROPERTIES = OPERATION_PREFIX + "show-webapp-slot-prop";
            String OPEN_IN_BROWSER = OPERATION_PREFIX + "open-webappslot-inbrowser";
            String OPEN_IN_PORTAL = OPERATION_PREFIX + "open-webappslot-in-portal";
            String START_STREAMING_LOG = OPERATION_PREFIX + "start-streaming-log-webapp-slot";
            String STOP_STREAMING_LOG = OPERATION_PREFIX + "stop-streaming-log-webapp-slot";
        }
    }

    public interface FunctionApp {
        String MODULE = "function";
        String OPERATION_PREFIX = MODULE + SEPARATOR;

        String TRIGGER = OPERATION_PREFIX + "trigger-function";
        String ENABLE = OPERATION_PREFIX + "enable-function";
        String DISABLE = OPERATION_PREFIX + "disable-function";
        String RUN = OPERATION_PREFIX + "run-function-app";
        String CREATE_TRIGGER = OPERATION_PREFIX + "create-function-trigger";
        String CREATE_PROJECT = OPERATION_PREFIX + "create-function-project";
        String CREATE = OPERATION_PREFIX + "create-function-app";
        String DEPLOY = OPERATION_PREFIX + "deploy-function-app";
        String DELETE = OPERATION_PREFIX + "delete-function-app";
        String START = OPERATION_PREFIX + "start-function-app";
        String STOP = OPERATION_PREFIX + "stop-function-app";
        String RESTART = OPERATION_PREFIX + "restart-function-app";
        String SHOW_PROPERTIES = OPERATION_PREFIX + "showprop-function-app";
        String START_STREAMING_LOG = OPERATION_PREFIX + "start-streaming-log-function-app";
        String STOP_STREAMING_LOG = OPERATION_PREFIX + "stop-streaming-log-function-app";
        String OPEN_IN_PORTAL = OPERATION_PREFIX + "open-inbrowser-function-app";
    }

    public interface MySQL {
        String MODULE = "azure-mysql";
        String OPERATION_PREFIX = MODULE + SEPARATOR + MODULE + OPERATION_SEPARATOR;

        String CREATE = OPERATION_PREFIX + "create";
        String DELETE = OPERATION_PREFIX + "delete";
        String START = OPERATION_PREFIX + "start";
        String STOP = OPERATION_PREFIX + "stop";
        String RESTART = OPERATION_PREFIX + "restart";
        String OPEN_IN_PORTAL = OPERATION_PREFIX + "open-in-portal";
        String SHOW_PROPERTIES = OPERATION_PREFIX + "show-properties";
        String CONNECT_TO_SERVER = OPERATION_PREFIX + "connect-to-server";
        String BIND_INTO = OPERATION_PREFIX + "bind-into";
        String LINK_TO_MODULE = OPERATION_PREFIX + "link-to-module";
    }

    public interface SpringCloud {
        String MODULE = "springcloud";
        String OPERATION_PREFIX = MODULE + SEPARATOR;

        String CREATE = OPERATION_PREFIX + "create-springcloud-app";
        String DELETE = OPERATION_PREFIX + "delete-springcloud-app";
        String START = OPERATION_PREFIX + "start-springcloud-app";
        String STOP = OPERATION_PREFIX + "stop-springcloud-app";
        String RESTART = OPERATION_PREFIX + "restart-springcloud-app";
        String OPEN_IN_PORTAL = OPERATION_PREFIX + "open-inportal-springcloud-app";
        String OPEN_IN_BROWSER = OPERATION_PREFIX + "open-inbrowser-springcloud-app";
        String SHOW_PROPERTIES = OPERATION_PREFIX + "showprop-springcloud-app";

        String SAVE = OPERATION_PREFIX + "save-springcloud-app";
        String REFRESH = OPERATION_PREFIX + "refresh-springcloud-app";
        String DEPLOY = OPERATION_PREFIX + "deploy-springcloud-app";
        String ADD_DEPENDENCY = OPERATION_PREFIX + "add-dependency-springcloud-app";
        String START_STREAMING_LOG = OPERATION_PREFIX + "start-streaming-log-springcloud-app";
        String STOP_STREAMING_LOG = OPERATION_PREFIX + "stop-streaming-log-springcloud-app";
    }

    public interface RedisCache {
        String MODULE = "redis";
        String OPERATION_PREFIX = MODULE + SEPARATOR;

        String CREATE = OPERATION_PREFIX + "create-redis";
        String DELETE = OPERATION_PREFIX + "delete-redis";
        String SCAN = OPERATION_PREFIX + "scan-redis";
        String GET = OPERATION_PREFIX + "get-redis";
        String OPEN_IN_PORTAL = OPERATION_PREFIX + "open-browser-redis";
        String SHOW_PROPERTIES = OPERATION_PREFIX + "readprop-redis";
        String OPEN_EXPLORER = OPERATION_PREFIX + "open-explorer-redis";
    }

    public interface VirtualMachine {
        String MODULE = "vm";
        String OPERATION_PREFIX = MODULE + SEPARATOR;

        String CREATE = OPERATION_PREFIX + "create-vm";
        String DELETE = OPERATION_PREFIX + "delete-vm";
        String START = OPERATION_PREFIX + "start-vm";
        String RESTART = OPERATION_PREFIX + "restart-vm";
        String STOP = OPERATION_PREFIX + "shutdown-vm";
    }

    public interface StorageAccount {
        String MODULE = "storage";
        String OPERATION_PREFIX = MODULE + SEPARATOR;

        String CREATE = OPERATION_PREFIX + "create-storage-account";
        String DELETE = OPERATION_PREFIX + "delete-storage-account";
        String OPEN_IN_PORTAL = OPERATION_PREFIX + "open-storage-in-portal";
        String DETACH = OPERATION_PREFIX + "detach-storage-account";
        String DELETE_STORAGE_TABLE = OPERATION_PREFIX + "delete-storage-table";
    }

    public interface ResourceManagement {
        String MODULE = "resource";
        String OPERATION_PREFIX = MODULE + SEPARATOR + MODULE + OPERATION_SEPARATOR;

        String DELETE = OPERATION_PREFIX + "delete";

        interface Deployment {
            String DELETE = OPERATION_PREFIX + "deployment-delete";
        }
    }

    public interface ContainerRegister {
        String MODULE = "acr";
        String OPERATION_PREFIX = MODULE + SEPARATOR;

        String PUSHIMAGE = OPERATION_PREFIX + "pushimage-acr";
        String OPEN_IN_PORTAL = OPERATION_PREFIX + "open-inbrowser-acr";
        String SHOW_PROPERTIES = OPERATION_PREFIX + "open-explorer-acr";

    }

    @AllArgsConstructor
    @Getter
    public static class ActionEntity {
        private final String serviceName;
        private final String operationName;
    }

}
