/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.common.experiment;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.Getter;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ExperimentationClient {
    private static final String ASSIGNMENT_UNIT_ID = "clientId";
    private static final String AUDIENCE_FILTER_ID = "userstype";
    private static final String AUDIENCE_FILTER_VALUE = "intellij";
    private static final String END_POINT = "https://aka.ms/azure-ij-ab-exp";
    private static ExperimentationService experimentationService;

    @Nullable
    public static ExperimentationService getExperimentationService() {
        if (Objects.isNull(experimentationService)) {
            init();
        }
        return experimentationService;
    }

    @AzureOperation(name = "internal/exp.assignment")
    private static void init() {
        try {
            final Map<String, String> audienceFilters = new HashMap<>();
            final Map<String, String> assignmentIds = new HashMap<>();
            final AzureConfiguration config = Azure.az().config();
            assignmentIds.put(ASSIGNMENT_UNIT_ID, config.getMachineId());
            experimentationService = new ExperimentationService()
                    .withEndPoint(END_POINT)
                    .withAudienceFilters(audienceFilters)
                    .withAssignmentIds(assignmentIds)
                    .create();
        } catch (final Throwable ignored) {
        }
    }

    public enum FeatureFlag {
        GETTING_STARTED_UI("showNewUI");
        @Getter
        private final String flagName;

        FeatureFlag(String flagName) {
            this.flagName = flagName;
        }
    }

}
