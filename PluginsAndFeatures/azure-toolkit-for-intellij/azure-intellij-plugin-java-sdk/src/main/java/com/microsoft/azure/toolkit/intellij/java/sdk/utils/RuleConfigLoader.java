// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.azure.toolkit.intellij.java.sdk.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.toolkit.intellij.java.sdk.models.RuleConfig;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
public class RuleConfigLoader {
    private static final String CONFIG_FILE_PATH = "./ruleConfigs.json";
    private static volatile RuleConfigLoader INSTANCE = new RuleConfigLoader();
    private Map<String, RuleConfig> ruleConfigs;
    private volatile boolean initialized = false;

    private RuleConfigLoader() {
        this.ruleConfigs = new HashMap<>();
        this.initialize();
    }

    static {
        try {
            // Eagerly load configuration at class load time
            INSTANCE.initialize();
        } catch (Exception e) {
            // Never fail class loading; keep instance alive with empty configs
            log.warn("Failed to eagerly initialize RuleConfigLoader: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the singleton instance of RuleConfigLoader.
     *
     * @return The singleton instance of RuleConfigLoader (never null).
     */
    @Nonnull
    public static RuleConfigLoader getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Get the rule configurations.
     *
     * @return the rule configurations
     */
    public Map<String, RuleConfig> getRuleConfigs() {
        return Collections.unmodifiableMap(ruleConfigs);
    }

    private synchronized void initialize() {
        if (initialized) {
            return;
        }
        try {
            this.ruleConfigs.clear();
            this.ruleConfigs.putAll(this.loadRuleConfigurations());
            // INSTANCE is already assigned in the static initializer; avoid reassigning it here
            initialized = true;
        } catch (IOException e) {
            // Keep INSTANCE non-null, but proceed with empty configs
            log.warn("Failed to initialize RuleConfigLoader: " + e.getMessage(), e);
            initialized = true;
        }
    }

    private Map<String, RuleConfig> loadRuleConfigurations() throws IOException {
        InputStream configStream = getClass().getClassLoader().getResourceAsStream(RuleConfigLoader.CONFIG_FILE_PATH);
        if (configStream == null) {
            log.info("Rule configuration file not found: " + RuleConfigLoader.CONFIG_FILE_PATH);
            return new HashMap<>();
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(configStream);
        Map<String, RuleConfig> configs = new HashMap<>();

        rootNode.fields().forEachRemaining(entry -> {
            String ruleName = entry.getKey();
            JsonNode ruleNode = entry.getValue();

            if (ruleNode.path("hasDerivedRules").asBoolean(false)) {
                // Parse derived rules
                ruleNode.fields().forEachRemaining(derivedEntry -> {
                    if (!derivedEntry.getKey().equals("hasDerivedRules")) {
                        String derivedRuleName = derivedEntry.getKey();
                        JsonNode derivedRuleNode = derivedEntry.getValue();
                        RuleConfig derivedRuleConfig = parseRuleConfig(derivedRuleNode);
                        configs.put(derivedRuleName, derivedRuleConfig);
                    }
                });
            } else {
                RuleConfig ruleConfig = parseRuleConfig(ruleNode);
                configs.put(ruleName, ruleConfig);
            }
        });

        return configs;
    }

    private RuleConfig parseRuleConfig(JsonNode ruleNode) {
        List<String> usages = parseStringOrArray(ruleNode.path("usages"));
        List<String> scope = parseStringOrArray(ruleNode.path("scope"));
        String antiPatternMessage = ruleNode.path("antiPatternMessage").asText(null);
        Map<String, String> regexPatterns = parseRegexPatterns(ruleNode.path("regexPatterns"));
        boolean skipRuleCheck = ruleNode.path("skip").asBoolean(false);

        return new RuleConfig(usages, scope, antiPatternMessage, regexPatterns, skipRuleCheck);
    }

    private List<String> parseStringOrArray(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node.isTextual()) {
            values.add(node.asText());
        } else if (node.isArray()) {
            node.forEach(element -> values.add(element.asText()));
        }
        return values;
    }

    private Map<String, String> parseRegexPatterns(JsonNode regexPatternsNode) {
        Map<String, String> regexPatterns = new HashMap<>();
        if (regexPatternsNode != null && regexPatternsNode.isObject()) {
            regexPatternsNode.fields().forEachRemaining(entry -> regexPatterns.put(entry.getKey(), entry.getValue().asText()));
        }
        return regexPatterns;
    }

    private static class Holder {
        public static final RuleConfigLoader INSTANCE = new RuleConfigLoader();
    }
}
