package com.microsoft.azure.appservice;

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

    private Subscription subscription;
    private ResourceGroup resourceGroup;

    private String name;
    @Builder.Default
    private Platform platform = DEFAULT_PLATFORM;
    @Builder.Default
    private Region region = DEFAULT_REGION;

    private AppServicePlan servicePlan;
    @Builder.Default
    private PricingTier pricingTier = DEFAULT_PRICING_TIER;

    private Path application;
}
