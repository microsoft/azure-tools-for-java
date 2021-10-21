package com.microsoft.intellij.feedback

import io.cucumber.java.en.Then
import kotlin.test.assertEquals

class GithubIssueScenario{
    @Then("^check the following source and result rows of URL ending replacement")
    fun checkUrlEndingRegexReplacement(inputWithExpact: Map<String, String>) {
        inputWithExpact.forEach { input, expect ->
            assertEquals(expect, input.replace(GithubIssue.urlEncoderEndingRegex, ""))
        }
    }
}