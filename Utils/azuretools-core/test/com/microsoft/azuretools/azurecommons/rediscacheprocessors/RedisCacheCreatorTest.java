package com.microsoft.azuretools.azurecommons.rediscacheprocessors;

import junit.framework.TestCase;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.microsoft.rest.RestClient;
import com.microsoft.azure.AzureResponseBuilder;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.azure.serializer.AzureJacksonAdapter;
import com.microsoft.azure.management.redis.implementation.RedisManager;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;

public class RedisCacheCreatorTest extends TestCase {
	
	private static final String BASE_URL = "http://localhost/";
	private static final String SUBSCRIPTION = "00000000-0000-0000-0000-000000000000";
	private static final String DNS_NAME = "test-redis";
	private static final String REGION_NAME = Region.US_CENTRAL.name();
	private static final String GROUP_NAME = "test-group";
	private static final int TOTAL_KEY_NUM = 72;
	
	private RedisCacheCreator redisCacheCreator;
	
	@Before
	protected void setUp() throws Exception {
		ApplicationTokenCredentials credentials = new AzureTestCredentials();
        RestClient restClient = new RestClient.Builder()
                .withBaseUrl(BASE_URL)
                .withSerializerAdapter(new AzureJacksonAdapter())
                .withResponseBuilderFactory(new AzureResponseBuilder.Factory())
                .withCredentials(credentials).build();
        RedisManager redisManager = RedisManager
                .authenticate(restClient, SUBSCRIPTION);
        RedisCaches redisCaches = redisManager.redisCaches();
		redisCacheCreator = new RedisCacheCreator(redisCaches, DNS_NAME, REGION_NAME, GROUP_NAME);
	}
	
	@Test
	public void testRedisCacheCreator() {
		Map<String, ProcessingStrategy> creatorMap = redisCacheCreator.CreatorMap();
		assertEquals(TOTAL_KEY_NUM, creatorMap.keySet().size());
	}
}
