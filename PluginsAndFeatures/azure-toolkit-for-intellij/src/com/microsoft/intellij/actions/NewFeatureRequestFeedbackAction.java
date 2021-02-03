/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.intellij.feedback.GithubIssue;
import com.microsoft.intellij.feedback.NewGithubIssueAction;
import com.microsoft.intellij.feedback.ReportableFeatureRequest;

public class NewFeatureRequestFeedbackAction extends NewGithubIssueAction {
    public NewFeatureRequestFeedbackAction() {
        super(new GithubIssue<>(new ReportableFeatureRequest("Feature Request"))
                .withLabel("feature-request"), "Request Features");
    }

    @Override
    protected String getServiceName(AnActionEvent event) {
        return TelemetryConstants.SYSTEM;
    }

    @Override
    protected String getOperationName(AnActionEvent event) {
        return TelemetryConstants.FEEDBACK;
    }
}
