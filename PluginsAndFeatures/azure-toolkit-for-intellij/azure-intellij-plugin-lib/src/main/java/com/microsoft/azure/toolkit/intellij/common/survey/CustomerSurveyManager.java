/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.common.survey;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class CustomerSurveyManager {
    private static final String AZURE_TOOLS_FOLDER = ".AzureToolsForIntelliJ";
    private static final String BASE_FOLDER = Paths.get(System.getProperty("user.home"), AZURE_TOOLS_FOLDER).toString();
    private static final String FILE_NAME_SURVEY_CONFIG = "SurveyConfig.json";
    private static final String OPERATION_NAME = "operationName";
    private static final String SYSTEM = "system";
    private static final String SERVICE_NAME = "serviceName";
    private static final String SURVEY = "survey";
    private static final String RESPONSE = "response";
    private static final String SURVEY_TYPE = "type";

    private static final int INIT_SURVEY_DELAY_BY_DAY = 10;
    private static final int PUT_OFF_DELAY_BY_DAY = 30;
    private static final int TAKE_SURVEY_DELAY_BY_DAY = 180;

    private final CustomerSurveyConfiguration customerSurveyConfiguration;
    private final AtomicBoolean surveyLock = new AtomicBoolean(true);

    public static CustomerSurveyManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    private CustomerSurveyManager(CustomerSurveyConfiguration customerSurveyConfiguration) {
        this.customerSurveyConfiguration = customerSurveyConfiguration;
    }

    public synchronized void takeSurvey(final Project project, final ICustomerSurvey survey) {
        if (!customerSurveyConfiguration.isAcceptSurvey()) {
            return;
        }
        final CustomerSurveyConfiguration.CustomerSurveyStatus status = getSurveyStatus(survey);
        if (status == null) {
            // init status for new survey
            customerSurveyConfiguration.getSurveyStatus().add(createNewStatus(survey));
            saveSurveyStatus();
            return;
        }
        if (LocalDateTime.now().isAfter(status.getNextSurveyDate()) && surveyLock.get()) {
            // show survey pop up
            surveyLock.set(false);
            ApplicationManager.getApplication().invokeLater(() -> showSurveyPopup(project, survey));
        }
    }

    private void showSurveyPopup(final Project project, final ICustomerSurvey survey) {
        final SurveyPopUpDialog popUpDialog = new SurveyPopUpDialog(project, survey, response -> {
            try {
                if (response == CustomerSurveyResponse.ACCEPT) {
                    // redirect to browser
                    BrowserUtil.browse(survey.getLink());
                }
                trackCustomerSurvey(survey, response);
            } finally {
                surveyLock.set(true);
            }
        });
        popUpDialog.setVisible(true);
    }

    private void trackCustomerSurvey(final ICustomerSurvey survey, final CustomerSurveyResponse response) {
        final Map<String, String> properties = new HashMap<>();
        properties.put(SERVICE_NAME, SYSTEM);
        properties.put(OPERATION_NAME, SURVEY);
        properties.put(SURVEY_TYPE, survey.getType());
        properties.put(RESPONSE, response.name());
        AzureTelemeter.log(AzureTelemetry.Type.INFO, properties);
        updateSurveyStatus(survey, response);
    }

    private CustomerSurveyConfiguration.CustomerSurveyStatus getSurveyStatus(ICustomerSurvey survey) {
        return customerSurveyConfiguration.getSurveyStatus().stream()
                .filter(status -> StringUtils.equalsIgnoreCase(status.getType(), survey.getType()))
                .findFirst().orElse(null);
    }

    private CustomerSurveyConfiguration.CustomerSurveyStatus createNewStatus(ICustomerSurvey customerSurvey) {
        return CustomerSurveyConfiguration.CustomerSurveyStatus.builder()
                .type(customerSurvey.getType())
                .surveyTimes(0)
                .nextSurveyDate(LocalDateTime.now().plusDays(INIT_SURVEY_DELAY_BY_DAY)).build();
    }

    private void updateSurveyStatus(ICustomerSurvey survey, CustomerSurveyResponse response) {
        if (response == CustomerSurveyResponse.NEVER_SHOW_AGAIN) {
            customerSurveyConfiguration.setAcceptSurvey(false);
        } else {
            final CustomerSurveyConfiguration.CustomerSurveyStatus surveyStatus = getSurveyStatus(survey);
            if (response == CustomerSurveyResponse.ACCEPT) {
                surveyStatus.setSurveyTimes(surveyStatus.getSurveyTimes() + 1);
                surveyStatus.setLastSurveyDate(LocalDateTime.now());
            }
            final int delay = response == CustomerSurveyResponse.ACCEPT ? TAKE_SURVEY_DELAY_BY_DAY : PUT_OFF_DELAY_BY_DAY;
            surveyStatus.setNextSurveyDate(LocalDateTime.now().plusDays(delay * 2));
            customerSurveyConfiguration.getSurveyStatus().stream()
                    .filter(status -> !StringUtils.equalsIgnoreCase(status.getType(), survey.getType()))
                    .forEach(customerSurveyStatus -> customerSurveyStatus.setNextSurveyDate(LocalDateTime.now().plusDays(delay)));
        }
        saveSurveyStatus();
    }

    private void saveSurveyStatus() {
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
        final File file = getConfigurationFile();
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            writer.writeValue(file, customerSurveyConfiguration);
        } catch (final IOException e) {
            // swallow exceptions for survey
        }
    }

    private static File getConfigurationFile() {
        return new File(BASE_FOLDER, FILE_NAME_SURVEY_CONFIG);
    }

    private static final class LazyHolder {
        private static final CustomerSurveyManager INSTANCE;

        static {
            INSTANCE = new CustomerSurveyManager(readSurveyStatus());
        }

        private static CustomerSurveyConfiguration readSurveyStatus() {
            final ObjectMapper mapper = new ObjectMapper();
            final File configurationFile = getConfigurationFile();
            try {
                return mapper.readValue(configurationFile, CustomerSurveyConfiguration.class);
            } catch (final JsonProcessingException jsonException) {
                return readToolkitSurveyStatusFromOldConfiguration(configurationFile, mapper);
            } catch (final IOException e) {
                return new CustomerSurveyConfiguration();
            }
        }

        private static CustomerSurveyConfiguration readToolkitSurveyStatusFromOldConfiguration(final File configurationFile, final ObjectMapper mapper) {
            try {
                final SurveyConfig config = mapper.readValue(configurationFile, SurveyConfig.class);
                final CustomerSurveyConfiguration.CustomerSurveyStatus surveyStatus = CustomerSurveyConfiguration.CustomerSurveyStatus.builder()
                        .type(CustomerSurvey.AZURE_INTELLIJ_TOOLKIT.name())
                        .surveyTimes(config.surveyTimes)
                        .lastSurveyDate(config.lastSurveyDate)
                        .nextSurveyDate(config.nextSurveyDate).build();
                final CustomerSurveyConfiguration surveyConfiguration = new CustomerSurveyConfiguration();
                surveyConfiguration.getSurveyStatus().add(surveyStatus);
                return surveyConfiguration;
            } catch (final IOException e) {
                return new CustomerSurveyConfiguration();
            }
        }
    }

    @Deprecated
    static class SurveyConfig {
        @JsonProperty("surveyTimes")
        private final int surveyTimes = 0;
        @JsonProperty("isAcceptSurvey")
        private final boolean isAcceptSurvey = true;
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        private final LocalDateTime lastSurveyDate = null;
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        private final LocalDateTime nextSurveyDate = LocalDateTime.now().plusDays(INIT_SURVEY_DELAY_BY_DAY);
    }
}
