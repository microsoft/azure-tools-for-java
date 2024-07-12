package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import java.util.Collections;
import java.util.List;

/**
 * This class contains configuration options for code style rules.
 * It contains the methods to check, the client name and the antipattern message
 */
class RuleConfig {
    private final List<String> methodsToCheck;
    private final String clientName;
    private final String antiPatternMessage;

    static final RuleConfig EMPTY_RULE = new RuleConfig(Collections.emptyList(), "", "");

    /**
     * Constructor for RuleConfig.
     *
     * @param methodsToCheck     List of methods to check.
     * @param clientName         Client name.
     * @param antiPatternMessage Antipattern message.
     */
    public RuleConfig(List<String> methodsToCheck, String clientName, String antiPatternMessage) {
        this.methodsToCheck = methodsToCheck;
        this.clientName = clientName;
        this.antiPatternMessage = antiPatternMessage;
    }

    // Getters

    /**
     * This method returns the list of methods to check
     *
     * @return List of methods to check
     */
    public List<String> getMethodsToCheck() {
        return methodsToCheck;
    }

    /**
     * This method returns the client name
     *
     * @return Client name
     */
    public String getClientName() {
        return clientName;
    }

    /**
     * This method returns the antipattern message
     *
     * @return Antipattern message
     */
    public String getAntiPatternMessage() {
        return antiPatternMessage;
    }
}
