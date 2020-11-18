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
import com.microsoft.azure.toolkit.lib.common.utils.MavenDependencyUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.dom4j.DocumentException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class SpringCloudDependencyManager extends MavenDependencyManager {
    private static final String LATEST_SPRING_BOOT_RELEASE = "2.3.0.RELEASE";

    public SpringCloudDependencyManager(String effectivePomXml) throws DocumentException {
        super(effectivePomXml);
    }

    public static List<DependencyArtifact> getCompatibleVersions(List<DependencyArtifact> dependencies, String springBootVersionStr)
            throws AzureExecutionException, IOException, DocumentException {
        List<DependencyArtifact> res = new ArrayList<>();
        DefaultArtifactVersion springBootVersion = new DefaultArtifactVersion(springBootVersionStr);
        for (DependencyArtifact dependency : dependencies) {
            if (StringUtils.isNotEmpty(dependency.getCurrentVersion()) && isCompatibleVersion(dependency.getCurrentVersion(), springBootVersionStr)) {
                continue;
            }
            List<String> latestVersions = MavenDependencyUtils.getMavenCentralVersions(dependency.getGroupId(), dependency.getArtifactId());
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

    private static boolean isCompatibleVersion(String versionStr, DefaultArtifactVersion springBootVersion) {
        DefaultArtifactVersion version = new DefaultArtifactVersion(versionStr);
        return springBootVersion.getMajorVersion() == version.getMajorVersion()
                && springBootVersion.getMinorVersion() == version.getMinorVersion();
    }

    private static String getCompatibleVersionWithBootVersion(List<String> latestVersions, DefaultArtifactVersion springBootVersion) {
        return IterableUtils.find(IterableUtils.reversedIterable(latestVersions), version -> isCompatibleVersion(version, springBootVersion));
    }
}
