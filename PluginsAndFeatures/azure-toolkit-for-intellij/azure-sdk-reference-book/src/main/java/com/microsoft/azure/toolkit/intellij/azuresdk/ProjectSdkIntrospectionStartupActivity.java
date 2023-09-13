/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.microsoft.azure.toolkit.intellij.azuresdk.enforcer.AzureSdkEnforcer;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.ProjectLibraryService;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.WorkspaceTaggingService;
import com.microsoft.azure.toolkit.intellij.common.survey.CustomerSurvey;
import com.microsoft.azure.toolkit.intellij.common.survey.CustomerSurveyManager;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemetry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class ProjectSdkIntrospectionStartupActivity implements StartupActivity {

    private static final String WORKSPACE_TAGGING = "workspace-tagging";
    private static final String WORKSPACE_TAGGING_FAILURE = "workspace-tagging-failure";
    private static final String OPERATION_NAME = "operationName";
    private static final String SERVICE_NAME = "serviceName";
    private static final String SYSTEM = "system";
    private static final String TAG = "tag";
    private static final String CLIENT = "client";
    private static final String MGMT = "mgmt";
    private static final String SPRING = "spring";

    @Nullable
    @Override
    public void runActivity(@Nonnull Project project) {
        Mono.delay(Duration.ofSeconds(30)).subscribe(next -> {
            if (project.isDisposed()) {
                return;
            }
            AzureSdkEnforcer.enforce(project);
            ProjectSdkIntrospectionStartupActivity.doRunActivity(project);
        }, error -> log.warn("error occurs in WorkspaceTaggingActivity.", error));
    }

    public static void doRunActivity(@Nonnull final Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final Set<String> workspaceTags = getWorkspaceTags(project);
                trackWorkspaceTagging(workspaceTags);
                showCustomerSurvey(project, workspaceTags);
            } catch (final Exception e) {
                // swallow exception for workspace tagging
                log.warn(e.getMessage());
            }
        });
    }

    private static void showCustomerSurvey(final @Nonnull Project project, final Set<String> workspaceTags) {
        if (workspaceTags.containsAll(Arrays.asList(CLIENT, MGMT))) {
            CustomerSurveyManager.getInstance().takeSurvey(project, CustomerSurvey.AZURE_SDK);
        }
        // Need to execute mgmt or client survey even if mgmt&client survey has been invoked in order to initialize survey status
        if (workspaceTags.contains(MGMT)) {
            CustomerSurveyManager.getInstance().takeSurvey(project, CustomerSurvey.AZURE_MGMT_SDK);
        }
        if (workspaceTags.contains(SPRING)) {
            CustomerSurveyManager.getInstance().takeSurvey(project, CustomerSurvey.AZURE_SPRING_SDK);
        }
        if (workspaceTags.contains(CLIENT)) {
            CustomerSurveyManager.getInstance().takeSurvey(project, CustomerSurvey.AZURE_CLIENT_SDK);
        }
        CustomerSurveyManager.getInstance().takeSurvey(project, CustomerSurvey.AZURE_INTELLIJ_TOOLKIT);
    }

    private static Set<String> getWorkspaceTags(@Nonnull final Project project) {
        return ProjectLibraryService.getProjectLibraries(project).stream()
                .map(l -> WorkspaceTaggingService.getWorkspaceTag(l.getGroupId(), l.getArtifactId()))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }

    private static void trackWorkspaceTagging(final Set<String> tagSet) {
        final Map<String, String> properties = new HashMap<>();
        properties.put(SERVICE_NAME, SYSTEM);
        properties.put(OPERATION_NAME, WORKSPACE_TAGGING);
        properties.put(TAG, StringUtils.join(tagSet, ","));
        AzureTelemeter.log(AzureTelemetry.Type.INFO, properties);
    }
}
