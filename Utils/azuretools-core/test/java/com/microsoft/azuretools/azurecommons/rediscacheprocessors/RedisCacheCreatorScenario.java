package com.microsoft.azuretools.azurecommons.rediscacheprocessors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Map;
import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;

import cucumber.api.java.en.Given;
import cucumber.api.java.en.Then;


public class RedisCacheCreatorScenario {
	
	private static final String DNS_NAME = "test-redis";
	private static final String REGION_NAME = Region.US_CENTRAL.name();
	private static final String GROUP_NAME = "test-group";
	private static final int TOTAL_KEY_NUM = 72;
	
	private RedisCacheCreator redisCacheCreator;
	
	@Given("^initialize the RedisCacheCreator$")
	protected void setUp() throws Exception {
		RedisCaches redisCaches = mock(RedisCaches.class);
		redisCacheCreator = new RedisCacheCreator(redisCaches, DNS_NAME, REGION_NAME, GROUP_NAME);
	}
	
	@Then("^check the the key number of CreatorMap$")
	public void testRedisCacheCreator() {
		Map<String, ProcessingStrategy> creatorMap = redisCacheCreator.CreatorMap();
		assertEquals(TOTAL_KEY_NUM, creatorMap.keySet().size());
	}
}
