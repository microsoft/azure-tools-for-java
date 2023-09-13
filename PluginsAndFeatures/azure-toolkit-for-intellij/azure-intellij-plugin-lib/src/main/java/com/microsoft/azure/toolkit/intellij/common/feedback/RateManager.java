/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.feedback;

import com.azure.core.exception.HttpResponseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.registry.Registry;
import com.microsoft.azure.toolkit.ide.common.store.AzureStoreManager;
import com.microsoft.azure.toolkit.ide.common.store.IIdeStore;
import com.microsoft.azure.toolkit.lib.common.operation.*;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import lombok.Data;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RateManager {
    public static final String SERVICE = "rate";
    public static final String RATED_AT = "rated_at";
    public static final String RATED_SCORE = "rated_score";
    public static final String POPPED_AT = "popped_at";
    public static final String POPPED_TIMES = "popped_times";
    public static final String NEXT_POP_AFTER = "next_pop_after";
    public static final String TOTAL_SCORE = "action_total_score";
    public static final String NEXT_REWIND_DATE = "next_rewind_date";

    private static final String SCORES_YML = "/com/microsoft/azure/toolkit/intellij/common/feedback/operation-scores.yml";
    private final Map<String, ScoreConfig> scores;
    private final AtomicInteger score = new AtomicInteger(0);

    private RateManager() {
        scores = loadScores();
        final IIdeStore store = AzureStoreManager.getInstance().getIdeStore();
        final int totalScore = Integer.parseInt(Objects.requireNonNull(store.getProperty(SERVICE, TOTAL_SCORE, "0")));
        score.set(totalScore);
    }

    public static RateManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private static Map<String, ScoreConfig> loadScores() {
        final ObjectMapper YML_MAPPER = new YAMLMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (final InputStream inputStream = RateManager.class.getResourceAsStream(SCORES_YML)) {
            return YML_MAPPER.readValue(inputStream, new TypeReference<>() {
            });
        } catch (final IOException ignored) {
        }
        return Collections.emptyMap();
    }

    @AzureOperation(name = "internal/feedback.add_operation_score")
    public void addScore(Operation causeOperation, int score) {
        final int total = this.score.addAndGet(score);
        OperationContext.current().setTelemetryProperty("addScore", String.valueOf(score));
        OperationContext.current().setTelemetryProperty("totalScore", String.valueOf(total));
        OperationContext.current().setTelemetryProperty("causeOperation", causeOperation.getId());
        OperationContext.current().setTelemetryProperty("causeOperationId", causeOperation.getExecutionId());
        final int threshold = Registry.intValue("azure.toolkit.feedback.score.threshold", 20);
        if (total >= threshold) {
            if (RatePopup.tryPopup(null)) {
                this.score.set(threshold / 2);
            } else {
                this.score.set(threshold);
            }
        }
        final IIdeStore store = AzureStoreManager.getInstance().getIdeStore();
        if (score > 0) {
            store.setProperty(SERVICE, NEXT_REWIND_DATE, String.valueOf(System.currentTimeMillis() + 15 * DateUtils.MILLIS_PER_DAY));
        }
        store.setProperty(SERVICE, TOTAL_SCORE, "" + this.score.get());
    }

    public synchronized int getScore() {
        return score.get();
    }

    @AzureOperation(name = "internal/feedback.rewind_operation_score_on_error")
    public void rewindScore(Operation causeOperation) {
        OperationContext.current().setTelemetryProperty("causeOperation", causeOperation.getId());
        OperationContext.current().setTelemetryProperty("causeOperationId", causeOperation.getExecutionId());
        score.set(score.get() / 2);
        Optional.ofNullable(AzureStoreManager.getInstance()).map(AzureStoreManager::getIdeStore).ifPresent(s -> s.setProperty(SERVICE, TOTAL_SCORE, "" + score.get()));
    }

    public static class WhenToPopup implements StartupActivity.DumbAware, OperationListener {
        @Override
        public void runActivity(@NotNull Project project) {
            final String hashMac = InstallationIdUtils.getHashMac();
            final char c = StringUtils.isBlank(hashMac) ? '0' : hashMac.toLowerCase().charAt(0);
            final boolean testMode = Registry.is("azure.toolkit.test.mode.enabled", false);
            final IIdeStore store = AzureStoreManager.getInstance().getIdeStore();
            if (Objects.isNull(store)) {
                return;
            }
            final String nextPopAfter = store.getProperty(SERVICE, NEXT_POP_AFTER);
            if (testMode || (!StringUtils.equals(nextPopAfter, "-1") && Character.digit(c, 16) % 4 == 0)) { // enabled for only 1/4
                final String nextRewindDate = store.getProperty(SERVICE, NEXT_REWIND_DATE);
                if (StringUtils.isBlank(nextRewindDate)) {
                    store.setProperty(SERVICE, NEXT_REWIND_DATE, String.valueOf(System.currentTimeMillis() + 15 * DateUtils.MILLIS_PER_DAY));
                } else if (Long.parseLong(nextRewindDate) > System.currentTimeMillis()) {
                    final int totalScore = Integer.parseInt(Objects.requireNonNull(store.getProperty(SERVICE, TOTAL_SCORE, "0")));
                    store.setProperty(SERVICE, TOTAL_SCORE, totalScore / 2 + "");
                }
                OperationManager.getInstance().addListener(this);
            }
        }

        @Override
        public void afterReturning(Operation operation, Object source) {
            final RateManager manager = RateManager.getInstance();
            if (StringUtils.equalsAnyIgnoreCase(operation.getType(), Operation.Type.AZURE, Operation.Type.BOUNDARY)) {
                final ScoreConfig config = manager.scores.get(operation.getId());
                if (config != null) {
                    final String actionId = Optional.ofNullable(operation.getActionParent()).map(Operation::getId).orElse(null);
                    if (ArrayUtils.isEmpty(config.getActions()) || Arrays.asList(config.getActions()).contains(actionId)) {
                        manager.addScore(operation, config.getSuccess());
                    }
                }
            }
        }

        @Override
        public void afterThrowing(Throwable e, Operation operation, Object source) {
            if (e instanceof OperationException) {
                return;
            }
            final RateManager manager = RateManager.getInstance();
            final Throwable cause = ExceptionUtils.getRootCause(e);
            if (!(cause instanceof HttpResponseException || cause.getClass().getPackageName().contains("java.net") || cause instanceof InterruptedException)) {
                manager.rewindScore(operation);
            }
        }
    }

    @Data
    public static class ScoreConfig {
        private String[] actions;
        private int success;
        private int failure;
    }

    private static class SingletonHolder {
        public static final RateManager INSTANCE = new RateManager();
    }
}
