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
import com.microsoft.intellij.AzureLinkStorage;
import com.microsoft.intellij.AzureMySQLStorage;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class AzureLinkService {
    private static final AzureLinkService instance = new AzureLinkService();

    public static AzureLinkService getInstance() {
        return instance;
    }

    private AzureLinkService() {

    }

    public void link(Project project, LinkConfig<MySQLResourceConfig, ModuleResourceConfig> linkComposite, boolean storageResource) {
        ModulePO modulePO = createModulePO(linkComposite.getModule());
        // create resource
        MySQLResourcePO resource = createResourcePO(linkComposite.getResource());
        // create link
        LinkPO linkPO = new LinkPO(resource.getId(), modulePO.getResourceId(), LinkType.SERVICE_WITH_MODULE, linkComposite.getEnvPrefix());
        // storage mysql
        if (storageResource) {
            AzureMySQLStorage.getStorage().addResource(resource);
        }
        // storage password
        if (ArrayUtils.isNotEmpty(linkComposite.getResource().getPasswordConfig().getPassword())) {
            String inputPassword = String.valueOf(linkComposite.getResource().getPasswordConfig().getPassword());
            AzureMySQLStorage.getStorage().savePassword(resource, resource.getPasswordSave(), resource.getUsername(), inputPassword);
        }
        // storage link
        AzureLinkStorage.getProjectStorage(project).addLink(linkPO);
    }

    private ModulePO createModulePO(ModuleResourceConfig config) {
        return new ModulePO(config.getModule().getName());
    }

    private MySQLResourcePO createResourcePO(MySQLResourceConfig config) {
        JdbcUrl jdbcUrl = JdbcUrl.from(config.getUrl());
        String businessUniqueKey = MySQLResourcePO.getBusinessUniqueKey(config.getServer().id(), jdbcUrl.getDatabase());
        MySQLResourcePO existedResourcePO = AzureMySQLStorage.getStorage().getResourceByBusinessUniqueKey(businessUniqueKey);
        String id = Objects.nonNull(existedResourcePO) ? existedResourcePO.getId() : DigestUtils.md5Hex(businessUniqueKey);
        MySQLResourcePO resourcePO = MySQLResourcePO.builder()
                .id(id)
                .resourceId(config.getServer().id())
                .url(config.getUrl())
                .username(config.getUsername())
                .passwordSave(config.getPasswordConfig().getPasswordSaveType())
                .build();
        return resourcePO;
    }

    public Map<String, String> retrieveLinkEnvsByModuleName(Project project, String moduleName) {
        Map<String, String> linkedEnvMap = new LinkedHashMap<>();
        List<LinkPO> moduleRelatedLinkList = AzureLinkStorage.getProjectStorage(project).getLinkByModuleId(moduleName)
                .stream()
                .filter(e -> LinkType.SERVICE_WITH_MODULE.equals(e.getType()))
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(moduleRelatedLinkList)) {
            return linkedEnvMap;
        }
        // services in application level
        Set<? extends BaseResourcePO> serviceSet = AzureMySQLStorage.getStorage().getResources();
        for (BaseResourcePO service : serviceSet) {
            for (LinkPO link : moduleRelatedLinkList) {
                if (!StringUtils.equals(link.getResourceId(), service.getId())) {
                    continue;
                }
                String envPrefix = link.getEnvPrefix();
                if (ResourceType.AZURE_DATABASE_FOR_MYSQL.equals(service.getType())) {
                    MySQLResourcePO mysql = (MySQLResourcePO) service;
                    String password = readPasswordCredentials(project, mysql);
                    linkedEnvMap.put(envPrefix + "URL", mysql.getUrl());
                    linkedEnvMap.put(envPrefix + "USERNAME", mysql.getUsername());
                    linkedEnvMap.put(envPrefix + "PASSWORD", password);
                }
            }
        }
        return linkedEnvMap;
    }

    private String readPasswordCredentials(Project project, MySQLResourcePO service) {
        String storagedPassword = AzureMySQLStorage.getStorage().loadPassword(service, service.getPasswordSave(), service.getUsername());
        if (StringUtils.isNotBlank(storagedPassword)) {
            if (MySQLConnectionUtils.connect(service.getUrl(), service.getUsername(), storagedPassword)) {
                return storagedPassword;
            }
        }
        // re-input password
        AtomicReference<PasswordConfig> passwordConfigReference = new AtomicReference<>();
        ApplicationManager.getApplication().invokeAndWait(() -> {
            PasswordDialog dialog = new PasswordDialog(project, service.getUsername(), service.getUrl());
            dialog.setOkActionListener(data -> {
                dialog.close();
                String inputPassword = String.valueOf(data.getPassword());
                if (MySQLConnectionUtils.connect(service.getUrl(), service.getUsername(), inputPassword)) {
                    AzureMySQLStorage.getStorage().savePassword(service, data.getPasswordSaveType(), service.getUsername(), inputPassword);
                    if (!Objects.equals(service.getPasswordSave(), data.getPasswordSaveType())) {
                        service.setPasswordSave(data.getPasswordSaveType());
                    }
                }
                passwordConfigReference.set(data);
            });
            dialog.show();
        });
        PasswordConfig passwordConfig = passwordConfigReference.get();
        if (Objects.nonNull(passwordConfig)) {
            return String.valueOf(passwordConfig.getPassword());
        } else {
            return StringUtils.EMPTY;
        }
    }

}
