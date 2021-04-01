/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.link.base.LinkType;
import com.microsoft.azure.toolkit.intellij.link.po.LinkPO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Content;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

public class AzureLinkStorage {

    private Set<LinkPO> links = new LinkedHashSet<>();

    private AzureLinkStorage() {
    }

    public static AzureLinkStorage getStorage() {
        return ServiceManager.getService(AzureLinkStorage.App.class);
    }

    public static AzureLinkStorage getProjectStorage(Project project) {
        return ServiceManager.getService(project, AzureLinkStorage.Prj.class);
    }

    public synchronized boolean addLink(LinkPO link) {
        Iterator<LinkPO> iterator = links.iterator();
        while (iterator.hasNext()) {
            LinkPO element = iterator.next();
            if (Objects.equals(element.getType(), link.getType())
                    && StringUtils.equals(element.getModuleId(), link.getModuleId())
                    && StringUtils.equals(element.getEnvPrefix(), link.getEnvPrefix())) {
                iterator.remove();
            }
        }
        return links.add(link);
    }

    public Set<LinkPO> getLinks() {
        return this.links;
    }

    public List<LinkPO> getLinksByType(LinkType type) {
        return links.stream().filter(e -> Objects.equals(e.getType(), type)).collect(Collectors.toList());
    }

    /**
     * For type = SERVICE_TO_PROJECT: sourceId is the id of the service, and targetId is the id of project or module.
     * For type = SERVICE_TO_SERVICE: sourceId is the id of the first service, and targetId is the id of the second service.
     */
    public List<LinkPO> getLinksByServiceId(String serviceId) {
        return links.stream().filter(e -> StringUtils.equals(serviceId, e.getResourceId())).collect(Collectors.toList());
    }

    /**
     * For type = SERVICE_TO_PROJECT: targetId is the id of the service, and targetId is the id of project or module.
     * For type = SERVICE_TO_SERVICE: targetId is the id of the first service, and targetId is the id of the second service.
     */
    public List<LinkPO> getLinkByModuleId(String moduleId) {
        return links.stream().filter(e -> StringUtils.equals(moduleId, e.getModuleId())).collect(Collectors.toList());
    }

    public static class AzureLinkStorageStateComponent extends AzureLinkStorage {

        public Element getState() {
            Element rootElement = new Element("azureLinks");
            this.writeState(rootElement);
            return rootElement;
        }

        private void writeState(Element linksElement) {
            for (LinkPO link : super.getLinks()) {
                Element linkElement = new Element("link");
                linkElement.setAttribute("type", link.getType().name());
                if (StringUtils.isNotBlank(link.getEnvPrefix())) {
                    linkElement.setAttribute("envPrefix", link.getEnvPrefix());
                }
                linkElement.addContent(new Element("resourceId").setText(link.getResourceId()));
                linkElement.addContent(new Element("moduleId").setText(link.getModuleId()));
                linksElement.addContent(linkElement);
            }
        }

        public void loadState(@NotNull Element state) {
            this.readState(state);
        }

        private void readState(Element linksElement) {
            if (CollectionUtils.isEmpty(linksElement.getContent())) {
                return;
            }
            for (Content content : linksElement.getContent()) {
                if (!(content instanceof Element)) {
                    continue;
                }
                Element linkElement = (Element) content;
                String envPrefix = linkElement.getAttributeValue("envPrefix");
                String linkTypeName = linkElement.getAttributeValue("type");
                LinkType linkType = LinkType.valueOf(linkTypeName);
                if (CollectionUtils.size(linkElement.getContent()) != 2) {
                    continue;
                }
                String resourceId = null;
                String moduleId = null;
                for (Content innerContent : linkElement.getContent()) {
                    if (!(content instanceof Element)) {
                        continue;
                    }
                    Element innerElement = (Element) innerContent;
                    if ("resourceId".equals(innerElement.getName())) {
                        resourceId = innerElement.getText();
                    } else if ("moduleId".equals(innerElement.getName())) {
                        moduleId = innerElement.getText();
                    }
                }
                if (StringUtils.isBlank(resourceId) || StringUtils.isBlank(moduleId)) {
                    continue;
                }
                final String finalResourceId = resourceId;
                final String finalModuleId = moduleId;
                if (super.getLinks().stream()
                        .filter(e -> StringUtils.equals(e.getResourceId(), finalResourceId) && StringUtils.equals(e.getModuleId(), finalModuleId))
                        .count() <= 0L) {
                    LinkPO linkPO = new LinkPO(finalResourceId, finalModuleId, linkType, envPrefix);
                    super.getLinks().add(linkPO);
                }
            }
        }
    }

    @State(
            name = "azureLinks",
            storages = {@Storage("azure/azureLinks.xml")}
    )
    public static class App extends AzureLinkStorageStateComponent implements PersistentStateComponent<Element> {
    }

    @State(
            name = "azureLinks",
            storages = {@Storage("azure/azureLinks.xml")}
    )
    public static class Prj extends AzureLinkStorageStateComponent implements PersistentStateComponent<Element> {
    }

}
