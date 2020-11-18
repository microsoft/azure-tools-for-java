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

import com.microsoft.intellij.util.PomXmlUpdater;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.dom4j.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MavenDependencyManager {
    public static final String POM_NAMESPACE = "http://maven.apache.org/POM/4.0.0";
    private static final String MAVEN_ARTIFACT_ID = "artifactId";
    private static final String MAVEN_GROUP_ID = "groupId";
    private static final String MAVEN_VERSION = "version";

    protected Document doc;

    public MavenDependencyManager(String effectivePomXml) throws DocumentException {
        Map<String, String> nsContext = new HashMap<>();
        nsContext.put("ns", POM_NAMESPACE);
        DocumentFactory.getInstance().setXPathNamespaceURIs(nsContext);
        doc = DocumentHelper.parseText(effectivePomXml);
    }

    public String getPluginConfiguration(String groupId, String artifactId, String configurationName) {
        for (Node node : doc.selectNodes("//ns:project/ns:build/ns:plugins/ns:plugin")) {
            String myGroupId = ((Element) node).elementTextTrim(MAVEN_GROUP_ID);
            String myArtifactId = ((Element) node).elementTextTrim(MAVEN_ARTIFACT_ID);
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

    public static boolean isGreaterOrEqualVersion(String versionStr1, String versionStr2) {
        DefaultArtifactVersion version1 = new DefaultArtifactVersion(versionStr1);
        DefaultArtifactVersion version2 = new DefaultArtifactVersion(versionStr2);
        return version1.compareTo(version2) >= 0;
    }

    protected static void collectDependencyVersionsFromNodes(List<Node> nodes, Map<String, DependencyArtifact> versionMap) {
        for (Node node : nodes) {
            String groupId = ((Element) node).elementTextTrim(MAVEN_GROUP_ID);
            String artifactId = ((Element) node).elementTextTrim(MAVEN_ARTIFACT_ID);
            DependencyArtifact artifact = new DependencyArtifact(groupId, artifactId, ((Element) node).elementTextTrim(
                    MAVEN_VERSION));
            versionMap.put(artifact.getKey(), artifact);
        }
    }
}
