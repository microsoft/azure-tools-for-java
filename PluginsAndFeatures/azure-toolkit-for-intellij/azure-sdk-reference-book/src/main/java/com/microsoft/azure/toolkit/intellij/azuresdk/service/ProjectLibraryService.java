/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.service;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ProjectLibraryService {
    private static final Pattern PATTERN = Pattern.compile("(Gradle|Maven): (.+):(.+):(.+)");

    @Nonnull
    public static List<ProjectLibEntity> getProjectLibraries(@Nonnull Project project) {
        if (project.isDisposed()) {
            return Collections.emptyList();
        }
        final List<String> libs = new ArrayList<>();
        OrderEnumerator.orderEntries(project).forEachLibrary(library -> libs.add(library.getName()));
        return libs.stream().filter(StringUtils::isNotBlank)
            .map(PATTERN::matcher)
            .filter(Matcher::matches)
            .map(m -> new ProjectLibEntity(m.group(2).trim(), m.group(3).trim(), m.group(4).trim()))
            .collect(Collectors.toList());
    }

    @Getter
    @RequiredArgsConstructor
    public static class ProjectLibEntity {
        private final String groupId;
        private final String artifactId;
        private final String version;

        public String getPackageName() {
            return String.format("%s/%s", groupId, artifactId);
        }
    }
}
