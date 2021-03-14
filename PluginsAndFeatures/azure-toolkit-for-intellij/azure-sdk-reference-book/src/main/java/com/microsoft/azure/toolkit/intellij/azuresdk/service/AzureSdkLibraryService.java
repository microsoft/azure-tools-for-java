/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkFeatureEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkPackageEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkServiceEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class AzureSdkLibraryService {
    private static final ObjectMapper mapper = new CsvMapper();
    private static final String SDK_METADATA_URL = "https://raw.githubusercontent.com/Azure/azure-sdk/master/_data/releases/latest/java-packages.csv";

    private List<AzureSdkArtifactEntity> artifacts = new ArrayList<>();

    public static AzureSdkLibraryService getInstance() {
        return Holder.instance;
    }

    public List<AzureSdkServiceEntity> getServices() {
        return toServices(this.artifacts);
    }

    public List<AzureSdkArtifactEntity> getArtifacts() {
        return new ArrayList<>(artifacts);
    }

    private List<AzureSdkServiceEntity> toServices(List<? extends AzureSdkArtifactEntity> artifacts) {
        final List<AzureSdkPackageEntity> packages = artifacts.stream()
            .filter(a -> StringUtils.isNoneBlank(a.getPackageName()))
            .map(this::convertArtifactToPackageEntity)
            .collect(Collectors.toList());
        final List<AzureSdkFeatureEntity> features = packages.stream()
            // convert to (service, feature) -> List<package> map
            .collect(Collectors.groupingBy(entity -> Pair.of(entity.getService(), entity.getFeature()), Collectors.toList()))
            .entrySet().stream()
            .map(entry -> createFeatureEntityFromPackages(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
        return features.stream()
            .collect(Collectors.groupingBy(AzureSdkFeatureEntity::getService, Collectors.toList()))
            .entrySet().stream()
            .map(entry -> AzureSdkServiceEntity.builder().name(StringUtils.firstNonBlank(entry.getKey(), "Other")).features(entry.getValue()).build())
            .collect(Collectors.toList());
    }

    public void reloadAzureSDKArtifacts() throws IOException {
        final URL destination = new URL(SDK_METADATA_URL);
        final CsvSchema schema = CsvSchema.emptySchema().withHeader();
        final MappingIterator<AzureSdkArtifactEntity> mappingIterator = mapper.readerFor(AzureSdkArtifactEntity.class).with(schema).readValues(destination);
        final List<AzureSdkArtifactEntity> artifacts = mappingIterator.readAll();
        synchronized (this) {
            this.artifacts = artifacts;
        }
    }

    private AzureSdkFeatureEntity createFeatureEntityFromPackages(Pair<String, String> feature, List<AzureSdkPackageEntity> packageEntities) {
        final List<AzureSdkPackageEntity> mgmtPackages =
            packageEntities.stream()
                .filter(entity -> StringUtils.equalsIgnoreCase(entity.getType(), "mgmt"))
                .collect(Collectors.toList());
        final List<AzureSdkPackageEntity> clientPackages =
            packageEntities.stream()
                .filter(entity -> StringUtils.equalsAnyIgnoreCase(entity.getType(), "client", "spring"))
                .collect(Collectors.toList());
        return AzureSdkFeatureEntity.builder()
            .name(feature.getValue())
            .service(feature.getKey())
            .clientPackages(clientPackages)
            .managementPackages(mgmtPackages)
            .description(StringUtils.EMPTY) // todo: find correct value for feature description
            .build();
    }

    private AzureSdkPackageEntity convertArtifactToPackageEntity(AzureSdkArtifactEntity artifactEntity) {
        // todo: get exact service and feature name from `Notes` of `AzureSDKArtifactEntity`
        final Map<String, String> links = new HashMap<>();
        if (StringUtils.isNotBlank(artifactEntity.getMsDocs())) {
            links.put("msdoc", artifactEntity.getMsDocs());
        }
        if (StringUtils.isNotBlank(artifactEntity.getGhDocs())) {
            links.put("ghdoc", artifactEntity.getGhDocs());
        }
        return AzureSdkPackageEntity.builder()
            .service(artifactEntity.getServiceName())
            .feature(artifactEntity.getDisplayName())
            .group(artifactEntity.getGroupId())
            .artifact(artifactEntity.getPackageName())
            .type(artifactEntity.getType())
            .versionGA(artifactEntity.getVersionGA())
            .versionPreview(artifactEntity.getVersionPreview())
            .mavenPath(artifactEntity.getRepoPath())
            .links(links)
            .build();
    }

    private static class Holder {
        private static final AzureSdkLibraryService instance = new AzureSdkLibraryService();
    }
}
