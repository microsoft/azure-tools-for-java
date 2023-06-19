package com.microsoft.azure.toolkit.intellij.applicationinsights.task;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.guidance.ComponentContext;
import com.microsoft.azure.toolkit.ide.guidance.Task;
import com.microsoft.azure.toolkit.intellij.applicationinsights.connection.ApplicationInsightsResourceDefinition;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.Resource;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.ResourceManager;
import com.microsoft.azure.toolkit.intellij.facet.AzureProjectFacet;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.AzureApplicationInsights;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import javax.annotation.Nonnull;
import java.util.Objects;

// todo: add create resource connection task instead of ai only
// todo: remove duplicate codes with connector dialog
public class CreateApplicationInsightsResourceConnectionTask implements Task {

    private final Project project;
    private final ComponentContext context;

    public CreateApplicationInsightsResourceConnectionTask(@Nonnull final ComponentContext context) {
        this.context = context;
        this.project = context.getProject();
    }

    @Override
    @AzureOperation(name = "internal/guidance.create_ai_resource_connection")
    public void execute() {
        final Resource resource = getResource();
        final Module module = getModule();
        final Resource consumer = getModuleConsumer(module);
        final Profile profile = AzureProjectFacet.getAzureModule(module).map(AzureModule::getDefaultProfile).orElse(null);
        if (Objects.isNull(profile)) {
            return;
        }
        final ConnectionManager connectionManager = profile.getConnectionManager();
        final ResourceManager resourceManager = profile.getResourceManager();
        final Connection connection = ConnectionManager.getDefinitionOrDefault(resource.getDefinition(),
                consumer.getDefinition()).define(resource, consumer);
        if (connection.validate(this.project)) {
            Objects.requireNonNull(resourceManager).addResource(resource);
            resourceManager.addResource(consumer);
            Objects.requireNonNull(connectionManager).addConnection(connection);
            final String message = String.format("The connection between %s and %s has been successfully created.",
                    resource.getName(), consumer.getName());
            AzureMessager.getMessager().success(message);
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "task.ai.create_connection";
    }

    private Resource<ApplicationInsight> getResource() {
        final String applicationInsightsId = (String) Objects.requireNonNull(context.getParameter("applicationInsightsId"),
                "`applicationInsightsId` should not be null to create a resource connection");
        final ApplicationInsight applicationInsight = Azure.az(AzureApplicationInsights.class).getById(applicationInsightsId);
        return ApplicationInsightsResourceDefinition.INSTANCE.define(applicationInsight);
    }

    private Resource<String> getModuleConsumer(@Nonnull final Module module) {
        return ModuleResource.Definition.IJ_MODULE.define(module.getName());
    }

    // todo: @hanli refactor to pass it from configuration
    private Module getModule() {
        return ModuleManager.getInstance(project).getModules()[0];
    }
}
