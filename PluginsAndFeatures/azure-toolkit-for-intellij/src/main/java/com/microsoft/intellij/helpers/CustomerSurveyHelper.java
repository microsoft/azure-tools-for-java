/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.helpers;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.survey.CustomerSurvey;
import com.microsoft.azure.toolkit.intellij.common.survey.CustomerSurveyManager;
import com.microsoft.intellij.rxjava.IdeaSchedulers;
import rx.Observable;

import java.util.concurrent.TimeUnit;

public enum CustomerSurveyHelper {

    INSTANCE;

    private static final int POP_UP_DELAY = 30;

    public void showFeedbackNotification(Project project) {
        Observable.timer(POP_UP_DELAY, TimeUnit.SECONDS)
                .subscribeOn(new IdeaSchedulers(project).dispatchUIThread())
                .take(1)
                .subscribe(next -> {
                    CustomerSurveyManager.getInstance().takeSurvey(project, CustomerSurvey.AZURE_INTELLIJ_TOOLKIT);
                });
    }
}
