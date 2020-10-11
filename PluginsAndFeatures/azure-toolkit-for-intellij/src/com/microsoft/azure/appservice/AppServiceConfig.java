package com.microsoft.azure.appservice;

import com.microsoft.azure.arm.resources.Region;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.resources.ResourceGroup;
import com.microsoft.azure.management.resources.Subscription;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;


@Data
@NoArgsConstructor
public class AppServiceConfig {
    protected Subscription subscription;
    protected ResourceGroup resourceGroup;

    protected String name;
    protected Platform platform;
    protected Region region;

    protected AppServicePlan servicePlan;

    protected Path application;
}
