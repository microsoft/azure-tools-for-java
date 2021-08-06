/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.dependency;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.intellij.springcloud.dependency.DependencyArtifact;
import com.microsoft.azure.toolkit.intellij.springcloud.dependency.PomXmlUpdater;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class SpringCloudDependencyManager {
    public static final String POM_NAMESPACE = "http://maven.apache.org/POM/4.0.0";
    private static final String LATEST_SPRING_BOOT_RELEASE = "2.3.0.RELEASE";
    private Document doc;

    public SpringCloudDependencyManager(String effectivePomXml) throws DocumentException {
        Map<String, String> nsContext = new HashMap<>();
        nsContext.put("ns", POM_NAMESPACE);
        DocumentFactory.getInstance().setXPathNamespaceURIs(nsContext);
        doc = DocumentHelper.parseText(effectivePomXml);
    }

    public String getPluginConfiguration(String groupId, String artifactId, String configurationName) {
        for (Node node : doc.selectNodes("//ns:project/ns:build/ns:plugins/ns:plugin")) {
            String myGroupId = ((Element) node).elementTextTrim("groupId");
            String myArtifactId = ((Element) node).elementTextTrim("artifactId");
            if (StringUtils.equals(groupId, myGroupId) && StringUtils.equals(artifactId, myArtifactId)) {
                Element configurationNode = ((Element) node).element("configuration");
                return configurationNode == null ? null : configurationNode.elementTextTrim(configurationName);
            }
        }
        return null;
    }

    public Map<String, DependencyArtifact> getDependencyVersions() {
        Map<String, DependencyArtifact> res = new HashMap<>();
        collectDependencyVersionsFromNodes(doc.selectNodes("//ns:project/ns:dependencies/ns:dependency"), res);
        return res;
    }

    public Map<String, DependencyArtifact> getDependencyManagementVersions() {
        Map<String, DependencyArtifact> res = new HashMap<>();
        collectDependencyVersionsFromNodes(doc.selectNodes("//ns:project/ns:dependencyManagement/ns:dependencies/ns:dependency"), res);
        return res;
    }

    public boolean update(File file, List<DependencyArtifact> des) throws IOException, DocumentException {
        return new PomXmlUpdater().updateDependencies(file, des);
    }

    public static List<DependencyArtifact> getCompatibleVersions(List<DependencyArtifact> dependencies, String springBootVersionStr)
            throws AzureExecutionException, IOException, DocumentException {
        List<DependencyArtifact> res = new ArrayList<>();
        DefaultArtifactVersion springBootVersion = new DefaultArtifactVersion(springBootVersionStr);
        for (DependencyArtifact dependency : dependencies) {
            if (StringUtils.isNotEmpty(dependency.getCurrentVersion()) && isCompatibleVersion(dependency.getCurrentVersion(), springBootVersionStr)) {
                continue;
            }
            List<String> latestVersions = getMavenCentralVersions(dependency.getGroupId(), dependency.getArtifactId());
            String targetVersionText = getCompatibleVersionWithBootVersion(latestVersions, springBootVersion);
            if (StringUtils.isEmpty(targetVersionText)) {
                if (isGreaterOrEqualVersion(springBootVersionStr, LATEST_SPRING_BOOT_RELEASE) && !latestVersions.isEmpty()) {
                    // to handle the ege case: spring-cloud-starter-config 2.2.5.RELEASE supports spring boot 2.3.0
                    // here for newest spring boot versions, use the latest versions
                    targetVersionText = latestVersions.get(latestVersions.size() - 1);
                } else {
                    throw new AzureExecutionException(String.format("Cannot get compatible version for %s:%s with Spring Boot with version %s",
                                                                    dependency.getGroupId(), dependency.getArtifactId(), springBootVersionStr));
                }

            }
            dependency.setCompatibleVersion(targetVersionText);
            if (!StringUtils.equals(dependency.getCurrentVersion(), targetVersionText)) {
                res.add(dependency);
            }
        }
        return res;
    }

    public static boolean isCompatibleVersion(String versionStr, String springBootVersionStr) {
        return isCompatibleVersion(versionStr, new DefaultArtifactVersion(springBootVersionStr));
    }

    public static boolean isGreaterOrEqualVersion(String versionStr1, String versionStr2) {
        DefaultArtifactVersion version1 = new DefaultArtifactVersion(versionStr1);
        DefaultArtifactVersion version2 = new DefaultArtifactVersion(versionStr2);
        return version1.compareTo(version2) >= 0;
    }

    private static boolean isCompatibleVersion(String versionStr, DefaultArtifactVersion springBootVersion) {
        DefaultArtifactVersion version = new DefaultArtifactVersion(versionStr);
        return springBootVersion.getMajorVersion() == version.getMajorVersion()
                && springBootVersion.getMinorVersion() == version.getMinorVersion();
    }

    private static String getCompatibleVersionWithBootVersion(List<String> latestVersions, DefaultArtifactVersion springBootVersion) {
        return IterableUtils.find(IterableUtils.reversedIterable(latestVersions), version -> isCompatibleVersion(version, springBootVersion));
    }

    private static void collectDependencyVersionsFromNodes(List<Node> nodes, Map<String, DependencyArtifact> versionMap) {
        for (Node node : nodes) {
            String groupId = ((Element) node).elementTextTrim("groupId");
            String artifactId = ((Element) node).elementTextTrim("artifactId");
            DependencyArtifact artifact = new DependencyArtifact(groupId, artifactId, ((Element) node).elementTextTrim("version"));
            versionMap.put(artifact.getKey(), artifact);
        }
    }

    private static List<String> getMavenCentralVersions(String groupId, String artifactId) throws IOException, DocumentException, AzureExecutionException {
        URLConnection conn = new URL(String.format("https://repo1.maven.org/maven2/%s/%s/maven-metadata.xml",
                StringUtils.replace(groupId, ".", "/"), artifactId)).openConnection();
        List<String> res = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String xml = IOUtils.toString(reader);
            Document doc = DocumentHelper.parseText(xml);
            List<Node> nodes = doc.selectNodes("//metadata/versioning/versions/version");
            for (Node node : nodes) {
                String version = node.getText();
                if (StringUtils.isNotEmpty(version)) {
                    res.add(version);
                }
            }
        }
        if (res.isEmpty()) {
            throw new AzureExecutionException((String.format("Cannot get version from maven central for: %s:%s.", groupId, artifactId)));
        }
        return res;
    }

}
