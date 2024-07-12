package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import java.util.List;

/**
 * This class contains configuration options for code style rules.
 * It contains the methods to check, the client name and the antipattern message
 */
class RuleConfig {
    private final List<String> methodsToCheck;
    private final String clientName;
    private final String antipatternMessage;

    /**
     * Constructor for RuleConfig.
     *
     * @param methodsToCheck     List of methods to check.
     * @param clientName         Client name.
     * @param antipatternMessage Antipattern message.
     */
    public RuleConfig(List<String> methodsToCheck, String clientName, String antipatternMessage) {
        this.methodsToCheck = methodsToCheck;
        this.clientName = clientName;
        this.antipatternMessage = antipatternMessage;
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
        return antipatternMessage;
    }
}