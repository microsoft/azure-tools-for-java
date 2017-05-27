package com.microsoft.azuretools.azurecommons.rediscacheprocessors;

import cucumber.api.junit.Cucumber;
import cucumber.api.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
		plugin = {"pretty", "html:target/cucumber"},
		name = "Test RedisCacheCreator"
)
public class RedisCacheCreatorTest {
}
