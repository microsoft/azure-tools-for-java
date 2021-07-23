/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.spark;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.microsoft.azure.hdinsight.sdk.rest.AttemptWithAppId;
import com.microsoft.azure.hdinsight.sdk.rest.IConvertible;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;

import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Application implements IConvertible {
    private String id;

    private List<Attempt> attempts;

    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Attempt> getAttempts() {
        return attempts;
    }

    public void setAttempts(List<Attempt> attempts) {
        this.attempts = attempts;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLastAttemptId() {
        return attempts.size();
    }

    public AttemptWithAppId getLastAttemptWithAppId(@NotNull String clusterName) {
        final int attemptTimes = attempts.size();
        Optional<Attempt> lastAttempt = attempts.stream()
                .filter(attempt -> Integer.valueOf(attempt.getAttemptId()) == attemptTimes)
                .findFirst();

        return lastAttempt.isPresent() ? new AttemptWithAppId(clusterName, getId(), lastAttempt.get()) : null;
    }
}
