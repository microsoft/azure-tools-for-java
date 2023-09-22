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
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.registry.Registry;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.Operation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.operation.OperationException;
import com.microsoft.azure.toolkit.lib.common.operation.OperationListener;
import com.microsoft.azure.toolkit.lib.common.operation.OperationManager;
import com.microsoft.azure.toolkit.lib.common.utils.InstallationIdUtils;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import lombok.Data;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class RateManager {
    public static final AzureConfiguration.Key<Long> RATED_AT = AzureConfiguration.Key.of("rate.rate_at");
    public static final AzureConfiguration.Key<Integer> RATED_SCORE = AzureConfiguration.Key.of("rate.rated_score");
    public static final AzureConfiguration.Key<Long> POPPED_AT = AzureConfiguration.Key.of("rate.popped_at");
    public static final AzureConfiguration.Key<Integer> POPPED_TIMES = AzureConfiguration.Key.of("rate.popped_times");
    public static final AzureConfiguration.Key<Long> NEXT_POP_AFTER = AzureConfiguration.Key.of("rate.next_pop_after");
    public static final AzureConfiguration.Key<Integer> TOTAL_SCORE = AzureConfiguration.Key.of("rate.action_total_score");
    public static final AzureConfiguration.Key<Long> NEXT_REWIND_DATE = AzureConfiguration.Key.of("rate.next_rewind_date");

    private static final String SCORES_YML = "/com/microsoft/azure/toolkit/intellij/common/feedback/operation-scores.yml";
    private final Map<String, ScoreConfig> scores;
    private final AtomicInteger score = new AtomicInteger(0);
    private final AzureConfiguration config;

    private RateManager() {
        scores = loadScores();
        this.config = Azure.az().config();
        final int totalScore = this.config.get(TOTAL_SCORE, 0);
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
        if (score > 0) {
            this.config.set(NEXT_REWIND_DATE, System.currentTimeMillis() + 15 * DateUtils.MILLIS_PER_DAY);
        }
        this.config.set(TOTAL_SCORE, this.score.get());
    }

    public synchronized int getScore() {
        return score.get();
    }

    @AzureOperation(name = "internal/feedback.rewind_operation_score_on_error")
    public void rewindScore(Operation causeOperation) {
        OperationContext.current().setTelemetryProperty("causeOperation", causeOperation.getId());
        OperationContext.current().setTelemetryProperty("causeOperationId", causeOperation.getExecutionId());
        score.set(score.get() / 2);
        this.config.set(TOTAL_SCORE, score.get());
    }

    public static class WhenToPopup implements ProjectActivity, OperationListener {
        @Override
        public Object execute(@Nonnull Project project, @Nonnull Continuation<? super Unit> continuation) {
            final String hashMac = InstallationIdUtils.getHashMac();
            final char c = StringUtils.isBlank(hashMac) ? '0' : hashMac.toLowerCase().charAt(0);
            final boolean testMode = Registry.is("azure.toolkit.test.mode.enabled", false);

            final AzureConfiguration config = Azure.az().config();
            final Long nextPopAfter = config.get(NEXT_POP_AFTER, 0L);
            if (testMode || (nextPopAfter != -1 && Character.digit(c, 16) % 4 == 0)) { // enabled for only 1/4
                final long nextRewindDate = config.get(NEXT_REWIND_DATE, 0L);
                if (nextRewindDate == 0) {
                    config.set(NEXT_REWIND_DATE, System.currentTimeMillis() + 15 * DateUtils.MILLIS_PER_DAY);
                } else if (nextRewindDate > System.currentTimeMillis()) {
                    final int totalScore = config.get(TOTAL_SCORE, 0);
                    config.set(TOTAL_SCORE, totalScore / 2);
                }
                OperationManager.getInstance().addListener(this);
            }
            return null;
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
