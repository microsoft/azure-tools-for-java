package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import java.util.List;

/**
 * This class represents the RuleConfig object
 * It contains the methods to check, the client name and the antipattern message
 */
class RuleConfig { // make this its own file
    private List<String> methodsToCheck;
    private String clientName;
    private String antipatternMessage;

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
    public String getAntipatternMessage() {
        return antipatternMessage;
    }

    // Setters
    /**
     * This method sets the list of methods to check
     *
     * @param methodsToCheck - List of methods to check
     */
    public void setMethodsToCheck(List<String> methodsToCheck) {
        this.methodsToCheck = methodsToCheck;
    }

    /**
     * This method sets the client name
     *
     * @param clientName - Client name
     */
    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    /**
     * This method sets the antipattern message
     *
     * @param antipatternMessage - Antipattern message
     */
    public void setAntipatternMessage(String antipatternMessage) {
        this.antipatternMessage = antipatternMessage;
    }
}