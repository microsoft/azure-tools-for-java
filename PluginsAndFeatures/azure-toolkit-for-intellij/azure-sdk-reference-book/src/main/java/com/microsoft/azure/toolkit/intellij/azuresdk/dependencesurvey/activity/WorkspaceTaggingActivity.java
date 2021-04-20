/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.dependencesurvey.activity;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.startup.StartupActivity;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.WorkspaceTaggingService;
import com.microsoft.azure.toolkit.lib.common.telemetry.AzureTelemeter;
import com.microsoft.azure.toolkit.lib.common.telemetry.Telemetry;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WorkspaceTaggingActivity implements StartupActivity.DumbAware {
    private static final Logger logger = Logger.getLogger(WorkspaceTaggingActivity.class.getName());

    private static final Pattern PATTERN = Pattern.compile("(Gradle|Maven): (.+):(.+):(.+)");
    private static final String WORKSPACE_TAGGING = "workspace-tagging";
    private static final String WORKSPACE_TAGGING_FAILURE = "workspace-tagging-failure";
    private static final String OPERATION_NAME = "operationName";
    private static final String SERVICE_NAME = "serviceName";
    private static final String SYSTEM = "system";
    private static final String TAG = "tag";

    @Override
    public void runActivity(@NotNull final Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                trackWorkspaceTagging(getWorkspaceTags(project));
            } catch (Exception e) {
                // swallow exception for workspace tagging
                logger.warning(e.getMessage());
            }
        });
    }

    private Set<String> getWorkspaceTags(@NotNull final Project project) {
        final Set<String> tagSet = new HashSet<>();
        OrderEnumerator.orderEntries(project).forEachLibrary(library -> {
            if (StringUtils.isNotEmpty(library.getName())) {
                final Matcher matcher = PATTERN.matcher(library.getName());
                if (matcher.matches()) {
                    final String tag = WorkspaceTaggingService.getWorkspaceTag(matcher.group(2), matcher.group(3));
                    if (StringUtils.isNotEmpty(tag)) {
                        tagSet.add(tag);
                    }
                }
            }
            return true;
        });
        return tagSet;
    }

    private void trackWorkspaceTagging(final Set<String> tagSet) {
        final Map<String, String> properties = new HashMap<>();
        properties.put(SERVICE_NAME, SYSTEM);
        properties.put(OPERATION_NAME, WORKSPACE_TAGGING);
        properties.put(TAG, StringUtils.join(tagSet, ","));
        AzureTelemeter.log(Telemetry.Type.INFO, properties);
    }
}
