package com.microsoft.azuretools.ijidea.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.intellij.feedback.GithubIssue;
import com.microsoft.intellij.feedback.NewGithubIssueAction;
import com.microsoft.intellij.feedback.ReportableFeatureRequest;

public class GithubFeatureRequestAction extends NewGithubIssueAction {
    public GithubFeatureRequestAction() {
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
