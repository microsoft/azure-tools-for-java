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

package com.microsoft.azuretools.core.survey;

import static com.microsoft.azuretools.Constants.FILE_NAME_SURVEY_CONFIG;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SURVEY;
import static com.microsoft.azuretools.telemetry.TelemetryConstants.SYSTEM;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.joda.time.LocalDateTime;
import org.osgi.framework.Version;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.joda.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.joda.ser.LocalDateTimeSerializer;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.telemetry.AppInsightsClient;
import com.microsoft.azuretools.telemetrywrapper.EventType;
import com.microsoft.azuretools.telemetrywrapper.EventUtil;
import com.microsoft.azuretools.telemetrywrapper.Operation;
import com.microsoft.azuretools.telemetrywrapper.TelemetryManager;

import rx.Observable;
import rx.schedulers.Schedulers;

public class CustomerSurveyHelper {

    private static final int POP_UP_DELAY = 30;
    private static final int INIT_SURVEY_DELAY_BY_DAY = 10;
    private static final int PUT_OFF_DELAY_BY_DAY = 30;
    private static final int TAKE_SURVEY_DELAY_BY_DAY = 180;
    //  Eclipse pop up window can't reset dispost time, so use a longer value here which differs IntelliJ(10s)
    private static final int DISPOSE_TIME = 20;

    private static final String SURVEY_URL = "https://microsoft.qualtrics.com/jfe/form/SV_5nhMbnPVKPLu2pv?"
            + "toolkit=%s&ide=%s&os=%s&jdk=%s&id=%s";

    private static final String TELEMETRY_KEY_RESPONSE = "response";
    private static final String TELEMETRY_VALUE_NEVER_SHOW = "neverShowAgain";
    private static final String TELEMETRY_VALUE_PUT_OFF = "putOff";
    private static final String TELEMETRY_VALUE_ACCEPT = "accept";

    private boolean isShown = false;
    private SurveyConfig surveyConfig;
    private Operation operation;

    public CustomerSurveyHelper() {
        loadConfiguration();
    }

    public void showFeedbackNotification() {
        if (isAbleToPopUpSurvey()) {
            Observable.timer(POP_UP_DELAY, TimeUnit.SECONDS).subscribeOn(Schedulers.io()).take(1).subscribe(next -> {
                Display.getDefault().syncExec(() -> {
                    SurveyNotificationPopup dialog = new SurveyNotificationPopup(Display.getDefault(),
                            () -> {
                                takeSurvey();
                            },
                            () -> {
                                putOff();
                            },
                            () -> {
                                neverShowAgain();
                            });

                    dialog.setFadingEnabled(false);
                    dialog.setDelayClose(DISPOSE_TIME * 1000);
                    dialog.open();
                    synchronized (CustomerSurveyHelper.class) {
                        if (operation != null) {
                            operation.complete();
                        }
                        operation = TelemetryManager.createOperation(SYSTEM, SURVEY);
                        operation.start();
                    }
                });
            });
        }
    }

    private void takeSurvey() {
        Program.launch(getRequestUrl());
        surveyConfig.surveyTimes++;
        surveyConfig.lastSurveyDate = LocalDateTime.now();
        surveyConfig.nextSurveyDate = LocalDateTime.now().plusDays(TAKE_SURVEY_DELAY_BY_DAY);
        saveConfiguration();
        sendTelemetry(TELEMETRY_VALUE_ACCEPT);
    }

    private void neverShowAgain() {
        surveyConfig.isAcceptSurvey = false;
        saveConfiguration();
        sendTelemetry(TELEMETRY_VALUE_NEVER_SHOW);
    }

    private void putOff() {
        surveyConfig.nextSurveyDate = LocalDateTime.now().plusDays(PUT_OFF_DELAY_BY_DAY);
        saveConfiguration();
        sendTelemetry(TELEMETRY_VALUE_PUT_OFF);
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
        try {
            File configFile = getConfigFile();
            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            IOUtils.write(ow.writeValueAsString(surveyConfig), new FileOutputStream(configFile),
                    Charset.defaultCharset());
        } catch (IOException e) {
            // swallow this exception as survey config should not bother user
        }
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

    private synchronized boolean isAbleToPopUpSurvey() {
        if (isShown) {
            return false;
        }
        isShown = true;
        return surveyConfig.isAcceptSurvey && LocalDateTime.now().isAfter(surveyConfig.nextSurveyDate);
    }

    private static File getConfigFile() {
        return new File(CommonSettings.getSettingsBaseDir(), FILE_NAME_SURVEY_CONFIG);
    }

    private String getRequestUrl() {
        final Version version = Platform.getBundle("org.eclipse.platform").getVersion();
        String ide = String.format("%s %s", "eclipse", version.toString());
        String os = System.getProperty("os.name");
        String jdk = String.format("%s %s", System.getProperty("java.vendor"), System.getProperty("java.version"));
        String id = AppInsightsClient.getInstallationId();
        String toolkitVersion = Activator.getDefault().getBundle().getVersion().toString();
        return String.format(SURVEY_URL, toolkitVersion, ide, os, jdk, id).replace(' ', '+');
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
