/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azuretools.azurecommons.rediscacheprocessors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.microsoft.azure.management.redis.RedisCaches;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;

public class RedisCacheCreatorTest {

    private static final String DNS_NAME = "test-redis";
    private static final String REGION_NAME = Region.US_CENTRAL.name();
    private static final String GROUP_NAME = "test-group";
    private static final int CAPACITY = 0;
    private static final int CAPACITY_FOR_PREMIUM = 1;
    private static final int TOTAL_KEY_NUM = 72;

    private static final String BASIC_WITH_EXIST_RES_GRP = "BASIC0Existing";
    private static final String BASIC_WITH_EXIST_RES_GRP_NO_SSL = "BASIC0ExistingNoSSL";
    private static final String BASIC_WITH_NEW_RES_GRP = "BASIC0New";
    private static final String BASIC_WITH_NEW_RES_GRP_NO_SSL = "BASIC0NewNoSSL";
    private static final String STD_WITH_EXIST_RES_GRP = "STD0Existing";
    private static final String STD_WITH_EXIST_RES_GRP_NO_SSL = "STD0ExistingNoSSL";
    private static final String STD_WITH_NEW_RES_GRP = "STD0New";
    private static final String STD_WITH_NEW_RES_GRP_NO_SSL = "STD0NewNoSSL";
    private static final String PREM_WITH_EXIST_RES_GRP = "PREMIUM1Existing";
    private static final String PREM_WITH_EXIST_RES_GRP_NO_SSL = "PREMIUM1ExistingNoSSL";
    private static final String PREM_WITH_NEW_RES_GRP = "PREMIUM1New";
    private static final String PREM_WITH_NEW_RES_GRP_NO_SSL = "PREMIUM1NewNoSSL";

    private RedisCacheCreator redisCacheCreator;
    private Map<String, ProcessingStrategy> creatorMap;

    @Before
    public void setUp() throws Exception {
        RedisCaches redisCaches = mock(RedisCaches.class);
        redisCacheCreator = new RedisCacheCreator(redisCaches, DNS_NAME, REGION_NAME, GROUP_NAME);
        creatorMap = redisCacheCreator.CreatorMap();
    }

    @Test
    public void testRedisCacheCreator() {
        assertEquals(TOTAL_KEY_NUM, creatorMap.keySet().size());
    }

    @Test
    public void testBasicWithExistResGrp() {
        BasicWithExistResGrp basicWithExistResGrp = (BasicWithExistResGrp) creatorMap.get(BASIC_WITH_EXIST_RES_GRP);
        assertNotNull(basicWithExistResGrp);
        assertEquals(DNS_NAME, basicWithExistResGrp.DNSName());
        assertEquals(REGION_NAME, basicWithExistResGrp.RegionName());
        assertEquals(GROUP_NAME, basicWithExistResGrp.ResourceGroupName());
        assertEquals(CAPACITY, basicWithExistResGrp.Capacity());
    }

    @Test
    public void testBasicWithExistResGrpNonSsl() {
        BasicWithExistResGrpNonSsl basicWithExistResGrpNonSsl = (BasicWithExistResGrpNonSsl) creatorMap.get(BASIC_WITH_EXIST_RES_GRP_NO_SSL);
        assertNotNull(basicWithExistResGrpNonSsl);
        assertEquals(DNS_NAME, basicWithExistResGrpNonSsl.DNSName());
        assertEquals(REGION_NAME, basicWithExistResGrpNonSsl.RegionName());
        assertEquals(GROUP_NAME, basicWithExistResGrpNonSsl.ResourceGroupName());
        assertEquals(CAPACITY, basicWithExistResGrpNonSsl.Capacity());
    }

    @Test
    public void testBasicWithNewResGrp() {
        BasicWithNewResGrp basicWithNewResGrp = (BasicWithNewResGrp) creatorMap.get(BASIC_WITH_NEW_RES_GRP);
        assertNotNull(basicWithNewResGrp);
        assertEquals(DNS_NAME, basicWithNewResGrp.DNSName());
        assertEquals(REGION_NAME, basicWithNewResGrp.RegionName());
        assertEquals(GROUP_NAME, basicWithNewResGrp.ResourceGroupName());
        assertEquals(CAPACITY, basicWithNewResGrp.Capacity());
    }

    @Test
    public void testBasicWithNewResGrpNonSsl() {
        BasicWithNewResGrpNonSsl basicWithNewResGrpNonSsl = (BasicWithNewResGrpNonSsl) creatorMap.get(BASIC_WITH_NEW_RES_GRP_NO_SSL);
        assertNotNull(basicWithNewResGrpNonSsl);
        assertEquals(DNS_NAME, basicWithNewResGrpNonSsl.DNSName());
        assertEquals(REGION_NAME, basicWithNewResGrpNonSsl.RegionName());
        assertEquals(GROUP_NAME, basicWithNewResGrpNonSsl.ResourceGroupName());
        assertEquals(CAPACITY, basicWithNewResGrpNonSsl.Capacity());
    }

    @Test
    public void testStdWithExistResGrp() {
        StdWithExistResGrp stdWithExistResGrp = (StdWithExistResGrp) creatorMap.get(STD_WITH_EXIST_RES_GRP);
        assertNotNull(stdWithExistResGrp);
        assertEquals(DNS_NAME, stdWithExistResGrp.DNSName());
        assertEquals(REGION_NAME, stdWithExistResGrp.RegionName());
        assertEquals(GROUP_NAME, stdWithExistResGrp.ResourceGroupName());
        assertEquals(CAPACITY, stdWithExistResGrp.Capacity());
    }

    @Test
    public void testStdWithExistResGrpNonSsl() {
        StdWithExistResGrpNonSsl stdWithExistResGrpNonSsl = (StdWithExistResGrpNonSsl) creatorMap.get(STD_WITH_EXIST_RES_GRP_NO_SSL);
        assertNotNull(stdWithExistResGrpNonSsl);
        assertEquals(DNS_NAME, stdWithExistResGrpNonSsl.DNSName());
        assertEquals(REGION_NAME, stdWithExistResGrpNonSsl.RegionName());
        assertEquals(GROUP_NAME, stdWithExistResGrpNonSsl.ResourceGroupName());
        assertEquals(CAPACITY, stdWithExistResGrpNonSsl.Capacity());
    }

    @Test
    public void testStdWithNewResGrp() {
        StdWithNewResGrp stdWithNewResGrp = (StdWithNewResGrp) creatorMap.get(STD_WITH_NEW_RES_GRP);
        assertNotNull(stdWithNewResGrp);
        assertEquals(DNS_NAME, stdWithNewResGrp.DNSName());
        assertEquals(REGION_NAME, stdWithNewResGrp.RegionName());
        assertEquals(GROUP_NAME, stdWithNewResGrp.ResourceGroupName());
        assertEquals(CAPACITY, stdWithNewResGrp.Capacity());
    }

    @Test
    public void testStdWithNewResGrpNonSsl() {
        StdWithNewResGrpNonSsl stdWithNewResGrpNonSsl = (StdWithNewResGrpNonSsl) creatorMap.get(STD_WITH_NEW_RES_GRP_NO_SSL);
        assertNotNull(stdWithNewResGrpNonSsl);
        assertEquals(DNS_NAME, stdWithNewResGrpNonSsl.DNSName());
        assertEquals(REGION_NAME, stdWithNewResGrpNonSsl.RegionName());
        assertEquals(GROUP_NAME, stdWithNewResGrpNonSsl.ResourceGroupName());
        assertEquals(CAPACITY, stdWithNewResGrpNonSsl.Capacity());
    }

    @Test
    public void testPremWithExistResGrp() {
        PremWithExistResGrp premWithExistResGrp = (PremWithExistResGrp) creatorMap.get(PREM_WITH_EXIST_RES_GRP);
        assertNotNull(premWithExistResGrp);
        assertEquals(DNS_NAME, premWithExistResGrp.DNSName());
        assertEquals(REGION_NAME, premWithExistResGrp.RegionName());
        assertEquals(GROUP_NAME, premWithExistResGrp.ResourceGroupName());
        assertEquals(CAPACITY_FOR_PREMIUM, premWithExistResGrp.Capacity());
    }

    @Test
    public void testPremWithExistResGrpNonSsl() {
        PremWithExistResGrpNonSsl premWithExistResGrpNonSsl = (PremWithExistResGrpNonSsl) creatorMap.get(PREM_WITH_EXIST_RES_GRP_NO_SSL);
        assertNotNull(premWithExistResGrpNonSsl);
        assertEquals(DNS_NAME, premWithExistResGrpNonSsl.DNSName());
        assertEquals(REGION_NAME, premWithExistResGrpNonSsl.RegionName());
        assertEquals(GROUP_NAME, premWithExistResGrpNonSsl.ResourceGroupName());
        assertEquals(CAPACITY_FOR_PREMIUM, premWithExistResGrpNonSsl.Capacity());
    }

    @Test
    public void testPremWithNewResGrp() {
        PremWithNewResGrp premWithNewResGrp = (PremWithNewResGrp) creatorMap.get(PREM_WITH_NEW_RES_GRP);
        assertNotNull(premWithNewResGrp);
        assertEquals(DNS_NAME, premWithNewResGrp.DNSName());
        assertEquals(REGION_NAME, premWithNewResGrp.RegionName());
        assertEquals(GROUP_NAME, premWithNewResGrp.ResourceGroupName());
        assertEquals(CAPACITY_FOR_PREMIUM, premWithNewResGrp.Capacity());
    }

    @Test
    public void testPremWithNewResGrpNonSsl() {
        PremWithNewResGrpNonSsl premWithNewResGrpNonSsl = (PremWithNewResGrpNonSsl) creatorMap.get(PREM_WITH_NEW_RES_GRP_NO_SSL);
        assertNotNull(premWithNewResGrpNonSsl);
        assertEquals(DNS_NAME, premWithNewResGrpNonSsl.DNSName());
        assertEquals(REGION_NAME, premWithNewResGrpNonSsl.RegionName());
        assertEquals(GROUP_NAME, premWithNewResGrpNonSsl.ResourceGroupName());
        assertEquals(CAPACITY_FOR_PREMIUM, premWithNewResGrpNonSsl.Capacity());
    }
}
