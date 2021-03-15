/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkServiceEntity;
import lombok.Getter;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AzureSdkLibraryService {
    private static final ObjectMapper mapper = new YAMLMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String SDK_METADATA_URL = "https://raw.githubusercontent.com/Azure/azure-sdk-for-java/master/sdk/spring/spring-reference.yml";
    private List<AzureSdkServiceEntity> services = new ArrayList<>();

    public static AzureSdkLibraryService getInstance() {
        return Holder.instance;
    }

    public List<AzureSdkServiceEntity> getServices() {
        return Collections.unmodifiableList(services);
    }

    public void reloadAzureSDKArtifacts() throws IOException {
        final URL destination = new URL(SDK_METADATA_URL);
        final ObjectReader reader = mapper.readerFor(RawData.class);
        final RawData rawData = reader.readValue(destination);
        synchronized (this) {
            this.services = rawData.getSpring();
        }
    }

    private static class Holder {
        private static final AzureSdkLibraryService instance = new AzureSdkLibraryService();
    }

    @Getter
    private static class RawData {
        private List<AzureSdkServiceEntity> spring;
    }
}
