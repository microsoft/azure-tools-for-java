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
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureJavaSdkArtifactExampleEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureJavaSdkArtifactExamplesEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.cache.Preload;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.CustomLog;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CustomLog
public class AzureSdkExampleService {
    private static final ObjectMapper CSV_MAPPER = new CsvMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String JAVA_LIBRARY_EXAMPLE_LIST_CSV = "/java-library-example-list.csv";
    private static final String JAVA_LIBRARY_EXAMPLE_INDEX_CSV = "/java-library-example-index.csv";

    @Nullable
    @Cacheable(value = "sdk/examples/{url}", key = "${entity.getRawUrl()}")
    @AzureOperation(name = "boundary/sdk.load_artifact_example.url", params = "entity.getRawUrl()")
    public static String loadArtifactExample(@Nonnull final AzureJavaSdkArtifactExampleEntity entity) {
        try {
            return IOUtils.toString(new URL(entity.getRawUrl()), Charset.defaultCharset());
        } catch (IOException e) {
            log.warn("failed to load Azure SDK example", e);
            return null;
        }
    }

    @Nullable
    public static AzureJavaSdkArtifactExamplesEntity getArtifactExamples(@Nonnull AzureSdkArtifactEntity entity) {
        return loadAllExamples().stream()
                .filter(example -> StringUtils.equalsIgnoreCase(example.getGroupId(), entity.getGroupId())
                        && StringUtils.equalsIgnoreCase(example.getPackageName(), entity.getArtifactId()))
                .findFirst().orElse(null);
    }

    @Preload
    @Cacheable(value = "sdk/examples")
    @AzureOperation(name = "internal/sdk.load_all_examples")
    public static List<AzureJavaSdkArtifactExamplesEntity> loadAllExamples() {
        final Map<Integer, List<AzureJavaSdkArtifactExampleEntity>> exampleMap = loadAzureSDKExampleEntities().stream()
                .collect(Collectors.groupingBy(e -> e.getReleaseId(), Collectors.mapping(e -> e, Collectors.toList())));
        final List<AzureJavaSdkArtifactExamplesEntity> indexEntities = loadAzureSDKExamplesEntities().stream()
                .filter(entity -> StringUtils.equalsIgnoreCase(entity.getLanguage(), "java"))
                .collect(Collectors.toList());
        indexEntities.forEach(entity -> entity.setExamples(exampleMap.get(entity.getId())));
        return indexEntities;
    }

    @Cacheable(value = "java-library-examples")
    @AzureOperation(name = "boundary/sdk.load_library_examples_entities")
    private static List<AzureJavaSdkArtifactExamplesEntity> loadAzureSDKExamplesEntities() {
        final ObjectReader reader = CSV_MAPPER.readerFor(AzureJavaSdkArtifactExamplesEntity.class).with(CsvSchema.emptySchema().withHeader());
        try (final InputStream stream = AzureSdkLibraryService.class.getResourceAsStream(JAVA_LIBRARY_EXAMPLE_INDEX_CSV)) {
            final MappingIterator<AzureJavaSdkArtifactExamplesEntity> data = reader.readValues(stream);
            return data.readAll().stream().collect(Collectors.toList());
        } catch (final IOException e) {
            log.warn("failed to load Azure SDK examples", e);
        }
        return Collections.emptyList();
    }

    @Cacheable(value = "java-library-example")
    @AzureOperation(name = "boundary/sdk.load_library_example_entities")
    private static List<AzureJavaSdkArtifactExampleEntity> loadAzureSDKExampleEntities() {
        final ObjectReader reader = CSV_MAPPER.readerFor(AzureJavaSdkArtifactExampleEntity.class).with(CsvSchema.emptySchema().withHeader());
        try (final InputStream stream = AzureSdkLibraryService.class.getResourceAsStream(JAVA_LIBRARY_EXAMPLE_LIST_CSV)) {
            final MappingIterator<AzureJavaSdkArtifactExampleEntity> data = reader.readValues(stream);
            return data.readAll().stream().collect(Collectors.toList());
        } catch (final IOException e) {
            log.warn("failed to load Azure SDK example", e);
        }
        return Collections.emptyList();
    }
}
