/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.link;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.link.LinkConfig;
import com.microsoft.azure.toolkit.intellij.link.ModuleResourceConfig;
import com.microsoft.azure.toolkit.intellij.link.base.LinkType;
import com.microsoft.azure.toolkit.intellij.link.base.ResourceType;
import com.microsoft.azure.toolkit.intellij.link.mysql.*;
import com.microsoft.azure.toolkit.intellij.link.po.BaseResourcePO;
import com.microsoft.azure.toolkit.intellij.link.po.LinkPO;
import com.microsoft.azure.toolkit.intellij.link.po.ModulePO;
import com.microsoft.azure.toolkit.intellij.link.po.MySQLResourcePO;
import com.microsoft.azuretools.ActionConstants;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.intellij.AzureLinkStorage;
import com.microsoft.intellij.AzureMySQLStorage;
import com.microsoft.intellij.util.PluginUtil;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class AzureLinkService {
    private static final AzureLinkService instance = new AzureLinkService();
    private static final int ACCESS_DENIED_ERROR_CODE = 1045;

    public static AzureLinkService getInstance() {
        return instance;
    }

    private AzureLinkService() {

    }

    public void link(Project project, LinkConfig<MySQLResourceConfig, ModuleResourceConfig> linkComposite, boolean storageResource) {
        final ModulePO modulePO = createModulePO(linkComposite.getModule());
        // create resource
        final MySQLResourcePO resource = createResourcePO(linkComposite.getResource());
        // create link
        final LinkPO linkPO = new LinkPO(resource.getId(), modulePO.getResourceId(), LinkType.SERVICE_WITH_MODULE, linkComposite.getEnvPrefix());
        // storage mysql
        if (storageResource) {
            AzureMySQLStorage.getStorage().addResource(resource);
        }
        // storage password
        if (ArrayUtils.isNotEmpty(linkComposite.getResource().getPasswordConfig().getPassword())) {
            final String inputPassword = String.valueOf(linkComposite.getResource().getPasswordConfig().getPassword());
            AzureMySQLStorage.getStorage().savePassword(resource, resource.getPasswordSave(), resource.getUsername(), inputPassword);
        }
        // storage link
        AzureLinkStorage.getProjectStorage(project).addLink(linkPO);
    }

    private ModulePO createModulePO(ModuleResourceConfig config) {
        return new ModulePO(config.getModule().getName());
    }

    private MySQLResourcePO createResourcePO(MySQLResourceConfig config) {
        final JdbcUrl jdbcUrl = JdbcUrl.from(config.getUrl());
        final String businessUniqueKey = MySQLResourcePO.getBusinessUniqueKey(config.getServer().id(), jdbcUrl.getDatabase());
        final MySQLResourcePO existedResourcePO = AzureMySQLStorage.getStorage().getResourceByBusinessUniqueKey(businessUniqueKey);
        final String id = Objects.nonNull(existedResourcePO) ? existedResourcePO.getId() : DigestUtils.md5Hex(businessUniqueKey);
        return MySQLResourcePO.builder()
                .id(id)
                .resourceId(config.getServer().id())
                .url(config.getUrl())
                .username(config.getUsername())
                .passwordSave(config.getPasswordConfig().getPasswordSaveType())
                .build();
    }

    public Map<String, String> retrieveLinkEnvsByModuleName(Project project, String moduleName) {
        final Map<String, String> linkedEnvMap = new LinkedHashMap<>();
        final List<LinkPO> moduleRelatedLinkList = AzureLinkStorage.getProjectStorage(project).getLinkByModuleId(moduleName)
                .stream()
                .filter(e -> LinkType.SERVICE_WITH_MODULE.equals(e.getType()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(moduleRelatedLinkList)) {
            return linkedEnvMap;
        }
        // services in application level
        final Set<? extends BaseResourcePO> serviceSet = AzureMySQLStorage.getStorage().getResources();
        for (final BaseResourcePO service : serviceSet) {
            for (final LinkPO link : moduleRelatedLinkList) {
                if (!StringUtils.equals(link.getResourceId(), service.getId())) {
                    continue;
                }
                final String envPrefix = link.getEnvPrefix();
                if (ResourceType.AZURE_DATABASE_FOR_MYSQL.equals(service.getType())) {
                    final MySQLResourcePO mysql = (MySQLResourcePO) service;
                    final String password = readPasswordCredentials(project, mysql);
                    linkedEnvMap.put(envPrefix + "URL", mysql.getUrl());
                    linkedEnvMap.put(envPrefix + "USERNAME", mysql.getUsername());
                    linkedEnvMap.put(envPrefix + "PASSWORD", password);
                }
            }
        }
        return linkedEnvMap;
    }

    private String readPasswordCredentials(Project project, MySQLResourcePO service) {
        final String storedPassword = AzureMySQLStorage.getStorage().loadPassword(service, service.getPasswordSave(), service.getUsername());
        if (StringUtils.isNotEmpty(storedPassword)) {
            final MySQLConnectionUtils.ConnectResult result = MySQLConnectionUtils.connectWithPing(service.getUrl(), service.getUsername(), storedPassword);
            if (result.isConnected()) {
                return storedPassword;
            }
            if (result.getErrorCode() != ACCESS_DENIED_ERROR_CODE) {
                PluginUtil.showWarnNotification("Failed to connect MySQL", result.getMessage());
                return StringUtils.EMPTY;
            }
        }
        // re-input password
        final AtomicReference<PasswordConfig> passwordConfigReference = new AtomicReference<>();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            final PasswordDialog dialog = new PasswordDialog(project, service.getUsername(), service.getUrl());
            dialog.setOkActionListener(data -> {
                dialog.close();
                final String inputPassword = String.valueOf(data.getPassword());
                if (MySQLConnectionUtils.connect(service.getUrl(), service.getUsername(), inputPassword)) {
                    AzureMySQLStorage.getStorage().savePassword(service, data.getPasswordSaveType(), service.getUsername(), inputPassword);
                    if (!Objects.equals(service.getPasswordSave(), data.getPasswordSaveType())) {
                        service.setPasswordSave(data.getPasswordSaveType());
                    }
                }
                passwordConfigReference.set(data);
            });
            dialog.show();
            EventUtil.logEvent(EventType.info, ActionConstants.parse(ActionConstants.MySQL.UPDATE_PASSWORD).getServiceName(),
                               ActionConstants.parse(ActionConstants.MySQL.UPDATE_PASSWORD).getOperationName(), null);
        });
        final PasswordConfig passwordConfig = passwordConfigReference.get();
        if (Objects.nonNull(passwordConfig)) {
            return String.valueOf(passwordConfig.getPassword());
        } else {
            return StringUtils.EMPTY;
        }
    }

}
