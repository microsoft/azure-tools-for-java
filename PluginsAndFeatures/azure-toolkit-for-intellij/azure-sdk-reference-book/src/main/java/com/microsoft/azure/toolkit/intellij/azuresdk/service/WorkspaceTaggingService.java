/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureJavaSdkEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.WorkspaceTagEntity;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.cache.Preload;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class WorkspaceTaggingService {

    private static final ObjectMapper JSON_MAPPER = new JsonMapper().configure(JsonParser.Feature.ALLOW_COMMENTS, true);
    private static final String WORKSPACE_TAG_JSON = "/workspaceTag.json";

    @Nonnull
    public static Set<String> getWorkspaceTags(@Nonnull String groupId, @Nonnull final String artifactId) {
        final HashSet<String> tags = new HashSet<>();
        if (StringUtils.isAnyEmpty(groupId, artifactId)) {
            return tags;
        }
        tags.addAll(getExternalDependencyTags(groupId, artifactId));
        tags.addAll(getAzureDependencyTags(groupId, artifactId));
        return tags;
    }

    private static Set<String> getAzureDependencyTags(final String groupId, final String artifactId) {
        return AzureSdkLibraryService.loadAzureSDKEntities()
            .stream()
            .filter(entity -> StringUtils.isNotEmpty(entity.getType())
                && StringUtils.equalsIgnoreCase(entity.getGroupId(), groupId)
                && StringUtils.equalsIgnoreCase(entity.getArtifactId(), artifactId))
            .map(AzureJavaSdkEntity::getType)
            .collect(Collectors.toSet());
    }

    private static Set<String> getExternalDependencyTags(final String groupId, final String artifactId) {
        return getWorkspaceTagEntities()
            .stream()
            .filter(entity -> (StringUtils.isEmpty(entity.getGroupId()) || StringUtils.equalsIgnoreCase(entity.getGroupId(), groupId))
                && (StringUtils.isEmpty(entity.getArtifactId()) || StringUtils.equalsIgnoreCase(entity.getArtifactId(), artifactId)))
            .map(WorkspaceTagEntity::getTag)
            .collect(Collectors.toSet());
    }

    @Preload
    @Cacheable(value = "workspace-tag")
    public static List<WorkspaceTagEntity> getWorkspaceTagEntities() {
        try (final InputStream stream = WorkspaceTaggingService.class.getResourceAsStream(WORKSPACE_TAG_JSON)) {
            final MappingIterator<WorkspaceTagEntity> iterator = JSON_MAPPER.readerFor(WorkspaceTagEntity.class).readValues(stream);
            return iterator.readAll();
        } catch (final IOException exception) {
            return Collections.emptyList();
        }
    }
}
