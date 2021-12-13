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
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkCategoryEntity;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class AzureSdkCategoryService {
    private static final ObjectMapper CSV_MAPPER = new CsvMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String SERVICE_CATEGORY_CSV = "/service-category.csv";

    @Cacheable(value = "azure-sdk-category-entities")
    @AzureOperation(name = "sdk.load_category_data", type = AzureOperation.Type.TASK)
    public static Map<String, List<AzureSdkCategoryEntity>> loadAzureSDKCategories() {
        try (final InputStream stream = AzureSdkCategoryService.class.getResourceAsStream(SERVICE_CATEGORY_CSV)) {
            // read
            final ObjectReader reader = CSV_MAPPER.readerFor(AzureSdkCategoryEntity.class).with(CsvSchema.emptySchema().withHeader());
            final MappingIterator<AzureSdkCategoryEntity> data = reader.readValues(stream);
            final List<AzureSdkCategoryEntity> categories = data.readAll();
            // default category & description suffix.
            categories.stream().filter(c -> StringUtils.isNotBlank(c.getServiceName())).forEach(c -> {
                if (StringUtils.isBlank(c.getCategory())) {
                    c.setCategory("Others");
                }
                final String trimDescription = StringUtils.trim(c.getDescription());
                if (StringUtils.isNotBlank(trimDescription) && !StringUtils.endsWith(trimDescription, ".")) {
                    c.setDescription(trimDescription + ".");
                }
            });
            // unique
            final List<AzureSdkCategoryEntity> uniqueCategories = categories.stream().collect(Collectors.collectingAndThen(
                    Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o -> o.getCategory() + "," + o.getServiceName()))), ArrayList::new));
            // group
            return uniqueCategories.stream().collect(Collectors.groupingBy(AzureSdkCategoryEntity::getCategory));
        } catch (final IOException e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

}
