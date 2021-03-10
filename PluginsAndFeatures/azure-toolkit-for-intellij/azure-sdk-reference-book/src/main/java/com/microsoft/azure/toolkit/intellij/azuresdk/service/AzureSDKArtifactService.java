/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.service;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSDKArtifactEntity;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class AzureSDKArtifactService {
    private static final String SDK_METADATA_URL = "https://raw.githubusercontent.com/Azure/azure-sdk/master/_data/releases/latest/java-packages.csv";

    private final List<AzureSDKArtifactEntity> artifacts = new ArrayList<>();

    public static AzureSDKArtifactService getInstance() {
        return LazyHolder.INSTANCE;
    }

    public void reloadAzureSDKArtifacts() throws IOException {
        final URL destination = new URL(SDK_METADATA_URL);
        final CsvSchema schema = CsvSchema.emptySchema().withHeader();
        final ObjectMapper mapper = new CsvMapper();
        final MappingIterator<AzureSDKArtifactEntity> mappingIterator = mapper.readerFor(AzureSDKArtifactEntity.class).with(schema).readValues(destination);
        synchronized (artifacts) {
            artifacts.clear();
            mappingIterator.readAll(artifacts);
        }
    }

    public List<AzureSDKArtifactEntity> getAzureSDKArtifacts() {
        return artifacts;
    }

    private static class LazyHolder {
        static final AzureSDKArtifactService INSTANCE = new AzureSDKArtifactService();
    }
}
