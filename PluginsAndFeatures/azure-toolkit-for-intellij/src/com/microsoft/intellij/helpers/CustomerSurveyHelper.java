package com.microsoft.intellij.helpers;

import com.google.gson.Gson;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azuretools.ijidea.ui.SurveyPopUpDialog;
import com.microsoft.intellij.actions.QualtricsSurveyAction;
import org.apache.commons.io.IOUtils;
import rx.Observable;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.temporal.TemporalUnit;
import java.util.concurrent.TimeUnit;

public enum CustomerSurveyHelper {

    INSTANCE;

    private static final int POP_UP_DELAY = 30;
    private static final int INIT_SURVEY_DELAY_BY_DAY = 10;
    private static final int PUT_OFF_DELAY_BY_DAY = 30;
    private static final int TAKE_SURVEY_DELAY_BY_DAY = 180;

    private static final String PLUGIN_FOLDER_NAME = "AzureToolsForIntelliJ";
    private static final String SURVEY_CONFIG_FILE = "SurveyConfig.json";

    private SurveyConfig surveyConfig;

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
    }

    public void putOff() {
        surveyConfig.nextSurveyDate = surveyConfig.nextSurveyDate.plusDays(PUT_OFF_DELAY_BY_DAY);
        saveConfiguration();
    }

    public void putOff(long amountToAdd, TemporalUnit unit) {
        surveyConfig.nextSurveyDate = surveyConfig.nextSurveyDate.plus(amountToAdd, unit);
        saveConfiguration();
    }

    public void neverShowAgain() {
        surveyConfig.isAcceptSurvey = false;
        saveConfiguration();
    }

    private boolean isAbleToPopUpSurvey() {
        return surveyConfig.isAcceptSurvey && LocalDateTime.now().isAfter(surveyConfig.nextSurveyDate);
    }

    private void loadConfiguration() {
        try (final FileReader fileReader = new FileReader(getConfigFile())) {
            String configString = IOUtils.toString(fileReader);
            surveyConfig = new Gson().fromJson(configString, SurveyConfig.class);
        } catch (IOException e) {
            surveyConfig = new SurveyConfig();
            saveConfiguration();
        }
    }

    private void saveConfiguration() {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                File configFile = getConfigFile();
                IOUtils.write(new Gson().toJson(surveyConfig), new FileOutputStream(configFile), Charset.defaultCharset());
            } catch (IOException e) {
                // swallow this exception as survey config should not bother user
            }
        });
    }

    private File getConfigFile() {
        File pluginFolder = new File(System.getProperty("user.home"), PLUGIN_FOLDER_NAME);
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }
        return new File(pluginFolder, SURVEY_CONFIG_FILE);
    }

    static class SurveyConfig {
        private int surveyTimes = 0;
        private boolean isAcceptSurvey = true;
        private LocalDateTime lastSurveyDate = null;
        private LocalDateTime nextSurveyDate = LocalDateTime.now().plusDays(INIT_SURVEY_DELAY_BY_DAY);
    }
}
