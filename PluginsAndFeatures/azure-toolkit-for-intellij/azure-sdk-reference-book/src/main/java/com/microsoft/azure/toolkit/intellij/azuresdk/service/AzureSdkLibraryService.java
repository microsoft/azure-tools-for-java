/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureJavaSdkEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkAllowListEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkFeatureEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkServiceEntity;
import com.microsoft.azure.toolkit.lib.common.cache.CacheEvict;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.cache.Preload;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class AzureSdkLibraryService {
    private static final ObjectMapper YML_MAPPER = new YAMLMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final ObjectMapper CSV_MAPPER = new CsvMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String SDK_ALLOW_LIST_CSV = "/sdk-allow-list.csv";
    private static final String SDK_JAVA_PACKAGES_BACKUP = "/java-packages.csv";
    private static final String SDK_SPRING_SERVICES_BACKUP = "/spring-reference.yml";
    private static final String SPRING_SDK_METADATA_URL = "https://raw.githubusercontent.com/Azure/azure-sdk-for-java/master/sdk/spring/spring-reference.yml";
    private static final String CLIENT_MGMT_SDK_METADATA_URL = "https://raw.githubusercontent.com/Azure/azure-sdk/master/_data/releases/latest/java-packages.csv";

    @Preload
    @Cacheable(value = "sdk/services")
    public static List<AzureSdkServiceEntity> loadAzureSdkServices() throws IOException {
        final Map<String, AzureSdkServiceEntity> services = loadSpringSDKEntities().stream()
                .collect(Collectors.toMap(e -> trim(e.getName()), e -> e));
        AzureSdkLibraryService.addClientLibs(services);
        AzureSdkLibraryService.addManagementLibs(services);
        return services.values().stream().sorted(Comparator.comparing(AzureSdkServiceEntity::getName)).collect(Collectors.toList());
    }

    private static void addClientLibs(Map<String, AzureSdkServiceEntity> services) {
        getClientLibs().stream()
                .sorted(Comparator.comparing(AzureJavaSdkEntity::getServiceName))
                .forEachOrdered(raw -> {
                    final AzureSdkServiceEntity service = getOrCreateService(services, raw);
                    for (final AzureSdkFeatureEntity feature : service.getContent()) {
                        final boolean specified = Optional.ofNullable(feature.getClientSource())
                                .filter(s -> s.getGroupId().equals(raw.getGroupId()) && s.getArtifactId().equals(raw.getArtifactId()))
                                .isPresent();
                        final boolean sameFeatureName = trim(feature.getName()).equals(trim(raw.getDisplayName()));
                        if (specified || sameFeatureName) {
                            feature.getArtifacts().add(toSdkArtifactEntity(raw));
                            return;
                        }
                    }
                    // if no mapping feature found.
                    final AzureSdkFeatureEntity feature = AzureSdkFeatureEntity.builder()
                            .name(raw.getDisplayName())
                            .msdocs(raw.getMsdocsUrl())
                            .artifacts(new ArrayList<>(Collections.singletonList(toSdkArtifactEntity(raw))))
                            .build();
                    service.getContent().add(feature);
                });
    }

    private static void addManagementLibs(Map<String, AzureSdkServiceEntity> services) {
        getManagementLibs().stream()
                .sorted(Comparator.comparing(AzureJavaSdkEntity::getServiceName))
                .forEachOrdered(raw -> {
                    final AzureSdkServiceEntity service = getOrCreateService(services, raw);
                    for (final AzureSdkFeatureEntity feature : service.getContent()) {
                        feature.setName(feature.getName().replace("Resource Management - ", "").trim());
                        feature.getArtifacts().add(toSdkArtifactEntity(raw));
                    }
                });
    }

    private static AzureSdkServiceEntity getOrCreateService(Map<String, AzureSdkServiceEntity> services, AzureJavaSdkEntity raw) {
        final Function<String, AzureSdkServiceEntity> serviceComputer = (key) -> {
            final AzureSdkFeatureEntity feature = AzureSdkFeatureEntity.builder().name(raw.getDisplayName()).msdocs(raw.getMsdocsUrl()).build();
            return AzureSdkServiceEntity.builder()
                    .name(raw.getServiceName())
                    .content(new ArrayList<>(Collections.singletonList(feature)))
                    .build();
        };
        return services.computeIfAbsent(trim(raw.getServiceName()), serviceComputer);
    }

    private static String trim(String name) {
        return name.toLowerCase().replaceAll("\\W+", "");
    }

    @Nonnull
    private static AzureSdkArtifactEntity toSdkArtifactEntity(@Nonnull AzureJavaSdkEntity entity) {
        final AzureSdkArtifactEntity artifact = new AzureSdkArtifactEntity();
        artifact.setGroupId(entity.getGroupId());
        artifact.setArtifactId(entity.getArtifactId());
        artifact.setType(entity.getType());
        artifact.setVersionGA(entity.getVersionGA());
        artifact.setVersionPreview(entity.getVersionPreview());
        artifact.setLinks(buildLinks(entity));
        return artifact;
    }

    /**
     * @see <a href=https://github.com/Azure/azure-sdk/blob/master/eng/README.md#link-templates>Link templates</a>
     */
    private static Map<String, String> buildLinks(AzureJavaSdkEntity entity) {
        final String mavenUrl = entity.getMavenArtifactUrl();
        final String msdocsUrl = entity.getMsdocsUrl();
        final String javadocUrl = entity.getJavadocUrl();
        final String githubUrl = entity.getGitHubSourceUrl();
        return ImmutableMap.of("github", githubUrl, "repopath", mavenUrl, "msdocs", msdocsUrl, "javadoc", javadocUrl);
    }

    @Nonnull
    private static List<AzureJavaSdkEntity> getClientLibs() {
        final Set<String> whitelist = AzureSdkLibraryService.loadAzureSDKWhitelist();
        return loadAzureSDKEntities().stream()
                .filter(raw -> AzureSdkArtifactEntity.Type.CLIENT.equals(raw.getType()))
                .filter(raw -> !Boolean.TRUE.equals(raw.getIsHide()))
                .filter(raw -> "com.azure".equals(raw.getGroupId()) || whitelist.contains(raw.getPackageName()))
                .collect(Collectors.toList());
    }

    @Nonnull
    private static List<AzureJavaSdkEntity> getManagementLibs() {
        final Set<String> whitelist = AzureSdkLibraryService.loadAzureSDKWhitelist();
        return loadAzureSDKEntities().stream()
                .filter(raw -> !Boolean.TRUE.equals(raw.getIsHide()))
                .filter(raw -> AzureSdkArtifactEntity.Type.MANAGEMENT.equals(raw.getType()))
                .filter(raw -> "com.azure.resourcemanager".equals(raw.getGroupId()) || whitelist.contains(raw.getPackageName()))
                .collect(Collectors.toList());
    }

    /**
     * get deprecated Azure SDK libs.
     * refer https://github.com/Azure/azure-sdk/blob/master/eng/README.md
     */
    public static List<AzureJavaSdkEntity> getDeprecatedAzureSDKEntities() {
        final List<AzureJavaSdkEntity> entities = loadAzureSDKEntities();
        // refer https://github.com/Azure/azure-sdk/blob/master/eng/README.md
        // > Hide - This field will determine whether we hide this package from various places like the package index, docs, as well as automated updates.
        // > The value is either true to hide or empty to not hide. This is useful to filter older packages that are still on the package managers,
        // > but we don't want to promote or display anywhere.
        return entities.stream()
                .filter(e -> StringUtils.isNotBlank(e.getReplace()) && !"active".equals(e.getSupport()))
                .collect(Collectors.toList());
    }

    @SneakyThrows(MalformedURLException.class)
    @Cacheable("sdk/packages/spring")
    @AzureOperation(name = "boundary/sdk.load_spring_libs_metadata")
    private static List<AzureSdkServiceEntity> loadSpringSDKEntities() {
        final List<AzureSdkServiceEntity> remote = loadSpringSDKEntities(new URL(SPRING_SDK_METADATA_URL));
        if (CollectionUtils.isEmpty(remote)) {
            return loadSpringSDKEntities(AzureSdkLibraryService.class.getResource(SDK_SPRING_SERVICES_BACKUP));
        }
        return remote;
    }

    @SneakyThrows(MalformedURLException.class)
    @Cacheable("sdk/packages")
    @AzureOperation(name = "boundary/sdk.load_java_libs_meta_data")
    public static List<AzureJavaSdkEntity> loadAzureSDKEntities() {
        final List<AzureJavaSdkEntity> remote = loadAzureSDKEntities(new URL(CLIENT_MGMT_SDK_METADATA_URL));
        if (CollectionUtils.isEmpty(remote)) {
            return loadAzureSDKEntities(AzureSdkLibraryService.class.getResource(SDK_JAVA_PACKAGES_BACKUP));
        }
        return remote;
    }

    public static List<AzureSdkServiceEntity> loadSpringSDKEntities(final URL destination) {
        try {
            final ObjectReader reader = YML_MAPPER.readerFor(AzureSdkServiceEntity.class);
            final MappingIterator<AzureSdkServiceEntity> data = reader.readValues(destination);
            return data.readAll();
        } catch (final IOException e) {
            log.warn(String.format("failed to load Azure SDK list from \"%s\"", destination.toString()), e);
        }
        return Collections.emptyList();
    }

    public static List<AzureJavaSdkEntity> loadAzureSDKEntities(final URL destination) {
        try {
            final ObjectReader reader = CSV_MAPPER.readerFor(AzureJavaSdkEntity.class).with(CsvSchema.emptySchema().withHeader());
            final MappingIterator<AzureJavaSdkEntity> data = reader.readValues(destination);
            return data.readAll().stream()
                    .filter(e -> StringUtils.isNoneBlank(e.getArtifactId(), e.getGroupId()))
                    .collect(Collectors.toList());
        } catch (final IOException e) {
            log.warn(String.format("failed to load Azure SDK list from \"%s\"", destination.toString()), e);
        }
        return Collections.emptyList();
    }

    @Cacheable("sdk/packages/whitelist")
    @AzureOperation(name = "boundary/sdk.load_whitelist_metadata")
    private static Set<String> loadAzureSDKWhitelist() {
        try {
            final URL destination = AzureSdkLibraryService.class.getResource(SDK_ALLOW_LIST_CSV);
            final ObjectReader reader = CSV_MAPPER.readerFor(AzureSdkAllowListEntity.class).with(CsvSchema.emptySchema().withHeader());
            final MappingIterator<AzureSdkAllowListEntity> data = reader.readValues(destination);
            return data.readAll().stream()
                    .filter(e -> StringUtils.isNoneBlank(e.getArtifactId(), e.getGroupId()))
                    .map(AzureSdkAllowListEntity::getPackageName)
                    .collect(Collectors.toSet());
        } catch (final IOException e) {
            log.warn(String.format("failed to load Azure SDK allow list from \"%s\"", SDK_ALLOW_LIST_CSV), e);
        }
        return Collections.emptySet();
    }

    public static void refresh() {
        try {
            CacheManager.evictCache("sdk/services", CacheEvict.ALL);
            CacheManager.evictCache("sdk/packages", CacheEvict.ALL);
            CacheManager.evictCache("sdk/packages/spring", CacheEvict.ALL);
            CacheManager.evictCache("sdk/packages/whitelist", CacheEvict.ALL);
        } catch (final ExecutionException e) {
            log.warn("failed to evict cache", e);
        }
    }
}
