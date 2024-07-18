package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * This class contains configuration options for code style rules.
 * It contains the methods to check, the client name and the antipattern message
 */
class RuleConfig {
    private final List<String> methodsToCheck;
    private final List<String> clientsToCheck;
    private final List<String> servicesToCheck;
    private final Map<String, String> antiPatternMessageMap;

    static final String AZURE_PACKAGE_NAME = "com.azure";

    static final RuleConfig EMPTY_RULE = new RuleConfig(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyMap());

    /**
     * Constructor for RuleConfig.
     *
     * @param methodsToCheck            List of methods to check.
     * @param clientsToCheck            List of clients to check.
     * @param servicesToCheck           List of services to check.
     * @param antiPatternMessageMap     Map of antipattern messages to display.
     */
    RuleConfig(List<String> methodsToCheck, List<String> clientsToCheck, List<String> servicesToCheck, Map<String, String> antiPatternMessageMap) {
        this.methodsToCheck = methodsToCheck;
        this.clientsToCheck = clientsToCheck;
        this.servicesToCheck = servicesToCheck;
        this.antiPatternMessageMap = antiPatternMessageMap;
    }

    /**
     * This method checks if the rule should be skipped.
     *
     * @return True if the rule should be skipped, false otherwise.
     */
    boolean skipRuleCheck() {
        return this == RuleConfig.EMPTY_RULE;
    }

    // Getters
    /**
     * This method returns the list of methods to check
     *
     * @return List of methods to check
     */
    List<String> getMethodsToCheck() {
        return methodsToCheck;
    }

    /**
     * This method returns the list of clients to check
     *
     * @return List of clients to check
     */
    List<String> getClientsToCheck() {
        return clientsToCheck;
    }

    /**
     * This method returns the list of services to check
     *
     * @return List of services to check
     */
    List<String> getServicesToCheck() {
        return servicesToCheck;
    }

    /**
     * This method returns a map of antipattern messages.
     * The key is the antipattern message key and the value is the antipattern message.
     * Generally, most rules have an antipattern message key of "anti_pattern_message".
     * Discouraged identifiers have an antipattern message key of the discouraged client or API being used.
     * "UpdateCheckpointAsync" rule has antipattern message keys of "with_subscribe" and "no_block".
     * @return Map of antipattern messages
     */
    Map<String, String> getAntiPatternMessageMap() {
        return antiPatternMessageMap;
    }
}
