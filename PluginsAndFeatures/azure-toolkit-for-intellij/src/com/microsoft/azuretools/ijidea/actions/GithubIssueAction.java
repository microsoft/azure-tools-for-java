package com.microsoft.azuretools.ijidea.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azuretools.telemetry.TelemetryConstants;
import com.microsoft.intellij.feedback.GithubIssue;
import com.microsoft.intellij.feedback.NewGithubIssueAction;
import com.microsoft.intellij.feedback.ReportableIssue;

public class GithubIssueAction extends NewGithubIssueAction {
    public GithubIssueAction() {
        super(new GithubIssue<>(new ReportableIssue("Customer Issues")), "Report Issues");
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
