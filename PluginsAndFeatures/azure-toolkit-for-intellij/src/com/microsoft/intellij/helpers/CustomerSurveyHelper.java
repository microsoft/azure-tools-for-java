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

package com.microsoft.intellij.helpers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.joda.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.joda.ser.LocalDateTimeSerializer;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.ijidea.ui.SurveyPopUpDialog;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;
import com.microsoft.intellij.actions.QualtricsSurveyAction;
import org.apache.commons.io.IOUtils;
import org.joda.time.LocalDateTime;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.microsoft.azuretools.Constants.FILE_NAME_SURVEY_CONFIG;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SURVEY;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SYSTEM;

public enum CustomerSurveyHelper {

    INSTANCE;

    private static final int POP_UP_DELAY = 30;
    private static final int INIT_SURVEY_DELAY_BY_DAY = 10;
    private static final int PUT_OFF_DELAY_BY_DAY = 30;
    private static final int TAKE_SURVEY_DELAY_BY_DAY = 180;

    private static final String TELEMETRY_KEY_RESPONSE = "response";
    private static final String TELEMETRY_VALUE_NEVER_SHOW = "neverShowAgain";
    private static final String TELEMETRY_VALUE_PUT_OFF = "putOff";
    private static final String TELEMETRY_VALUE_ACCEPT = "accept";

    private boolean isShown = false;
    private SurveyConfig surveyConfig;
    private Operation operation;

    CustomerSurveyHelper() {
        loadConfiguration();
    }

    public void showFeedbackNotification(Project project) {
        if (isAbleToPopUpSurvey()) {
            Observable.timer(POP_UP_DELAY, TimeUnit.SECONDS).subscribeOn(Schedulers.io())
                    .take(1)
                    .subscribe(next -> {
                        SurveyPopUpDialog dialog = new SurveyPopUpDialog(CustomerSurveyHelper.this, project);
                        dialog.setVisible(true);
                        synchronized (CustomerSurveyHelper.class) {
                            if (operation != null) {
                                operation.complete();
                            }
                            operation = TelemetryManager.createOperation(SYSTEM, SURVEY);
                            operation.start();
                        }
                    });
        }
    }

    public void takeSurvey() {
        new QualtricsSurveyAction().actionPerformed(new AnActionEvent(null, DataManager.getInstance().getDataContext(),
                ActionPlaces.UNKNOWN, new Presentation(), ActionManager.getInstance(), 0));
        surveyConfig.surveyTimes++;
        surveyConfig.lastSurveyDate = LocalDateTime.now();
        surveyConfig.nextSurveyDate = LocalDateTime.now().plusDays(TAKE_SURVEY_DELAY_BY_DAY);
        saveConfiguration();
        sendTelemetry(TELEMETRY_VALUE_ACCEPT);
    }

    public void putOff() {
        surveyConfig.nextSurveyDate = LocalDateTime.now().plusDays(PUT_OFF_DELAY_BY_DAY);
        saveConfiguration();
        sendTelemetry(TELEMETRY_VALUE_PUT_OFF);
    }

    public void neverShowAgain() {
        surveyConfig.isAcceptSurvey = false;
        saveConfiguration();
        sendTelemetry(TELEMETRY_VALUE_NEVER_SHOW);
    }

    private synchronized boolean isAbleToPopUpSurvey() {
        if (isShown) {
            return false;
        }
        isShown = true;
        return surveyConfig.isAcceptSurvey && LocalDateTime.now().isAfter(surveyConfig.nextSurveyDate);
    }

    private void loadConfiguration() {
        try (final FileReader fileReader = new FileReader(getConfigFile())) {
            String configString = IOUtils.toString(fileReader);
            ObjectMapper mapper = new ObjectMapper();
            surveyConfig = mapper.readValue(configString, SurveyConfig.class);
        } catch (IOException e) {
            surveyConfig = new SurveyConfig();
            saveConfiguration();
        }
    }

    private void saveConfiguration() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                File configFile = getConfigFile();
                ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
                IOUtils.write(ow.writeValueAsString(surveyConfig), new FileOutputStream(configFile), Charset.defaultCharset());
            } catch (IOException e) {
                // swallow this exception as survey config should not bother user
            }
        });
    }

    private File getConfigFile() {
        return new File(CommonSettings.getSettingsBaseDir(), FILE_NAME_SURVEY_CONFIG);
    }

    private synchronized void sendTelemetry(String response) {
        if (operation == null) {
            return;
        }
        Map<String, String> properties = new HashMap<>();
        properties.put(TELEMETRY_KEY_RESPONSE, response);
        EventUtil.logEvent(EventType.info, operation, properties);
        operation.complete();
    }

    static class SurveyConfig {
        @JsonProperty("surveyTimes")
        private int surveyTimes = 0;
        @JsonProperty("isAcceptSurvey")
        private boolean isAcceptSurvey = true;
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        private LocalDateTime lastSurveyDate = null;
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        private LocalDateTime nextSurveyDate = LocalDateTime.now().plusDays(INIT_SURVEY_DELAY_BY_DAY);
    }
}
