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

package com.microsoft.intellij.maven;

import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.intellij.util.PomXmlUpdater;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpringCloudDependencyManager {
    public static final String POM_NAMESPACE = "http://maven.apache.org/POM/4.0.0";
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

    public void update(File file, List<DependencyArtifact> des) throws IOException, DocumentException {
        new PomXmlUpdater().updateDependencies(file, des);
    }

    public static List<DependencyArtifact> getCompatibleVersions(List<DependencyArtifact> dependencies, String springBootVer)
            throws AzureExecutionException, IOException, DocumentException {
        List<DependencyArtifact> res = new ArrayList<>();
        for (DependencyArtifact dependency : dependencies) {
            List<String> latestVersions = getMavenCentralVersions(dependency.getGroupId(), dependency.getArtifactId());
            String targetVer = "";
            if (springBootVer.startsWith("2.2.")) {
                targetVer = getCompatibleVersionWithBootVersion(latestVersions, "2.2.");
            } else if (springBootVer.startsWith("2.1.")) {
                targetVer = getCompatibleVersionWithBootVersion(latestVersions, "2.1.");
            } else {
                throw new AzureExecutionException("Unsupported spring-boot version: " + springBootVer);
            }
            if (StringUtils.isEmpty(targetVer)) {
                throw new AzureExecutionException(String.format("Cannot get compatible version for %s:%s with Spring Boot with version %s",
                        dependency.getGroupId(), dependency.getArtifactId(), springBootVer));
            }
            dependency.setCompatibleVersion(targetVer);
            if (!StringUtils.equals(dependency.getCurrentVersion(), targetVer)) {
                res.add(dependency);
            }
        }
        return res;
    }

    private static String getCompatibleVersionWithBootVersion(List<String> latestVersions, String bootVersionPrefix) {
        return IterableUtils.find(IterableUtils.reversedIterable(latestVersions), version -> version != null && version.startsWith(bootVersionPrefix));
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
