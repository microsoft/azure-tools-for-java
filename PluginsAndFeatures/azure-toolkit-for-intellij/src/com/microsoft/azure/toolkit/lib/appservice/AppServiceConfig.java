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

package com.microsoft.azure.toolkit.lib.appservice;

import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.nio.file.Path;

@Data
@SuperBuilder
public class AppServiceConfig {
    public static final Region DEFAULT_REGION = Region.EUROPE_WEST;
    public static final Platform DEFAULT_PLATFORM = Platform.Linux.JAVA8_TOMCAT9;
    public static final PricingTier DEFAULT_PRICING_TIER = new PricingTier("Premium", "P1V2");

    private String name;
    private Path application;
    @Builder.Default
    private Platform platform = DEFAULT_PLATFORM;

    private Subscription subscription;
    private ResourceGroup resourceGroup;
    private AppServicePlan servicePlan;
    @Builder.Default
    private Region region = DEFAULT_REGION;
}
