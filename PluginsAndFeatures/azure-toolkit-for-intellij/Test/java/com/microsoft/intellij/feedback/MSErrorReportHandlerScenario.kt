package com.microsoft.intellij.feedback

import cucumber.api.java.en.Given
import cucumber.api.java.en.Then
import kotlin.test.assertEquals

class MSErrorReportHandlerScenario {
    private lateinit var fullStacks: String

    @Given("^the full call stacks to filter MSCallStacks$")
    fun checkFilterMSCallStacks(input: String) {
        this.fullStacks = input
    }

    @Then("^check the filtered MSCallStacks result$")
    fun checkFilterMSCallStackResult(expect: String) {
        assertEquals(expect, filterMSCallStacks(fullStacks))
    }
}
