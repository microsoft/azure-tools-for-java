/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.containerregistry;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.Tag;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

public class ContainerRegistryActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;

    public static final String SERVICE_ACTIONS = "actions.registry.service";
    public static final String REGISTRY_ACTIONS = "actions.registry.registry";
    public static final String REPOSITORY_ACTIONS = "actions.registry.repository";
    public static final String TAG_ACTIONS = "actions.registry.tag";

    public static final Action.Id<ContainerRegistry> PUSH_IMAGE = Action.Id.of("user/acr.push_image.registry");
    public static final Action.Id<ContainerRegistry> ENABLE_ADMIN_USER = Action.Id.of("user/acr.enable_admin_user.registry");
    public static final Action.Id<ContainerRegistry> DISABLE_ADMIN_USER = Action.Id.of("user/acr.disable_admin_user.registry");
    public static final Action.Id<ContainerRegistry> COPY_REGISTRY_URL = Action.Id.of("user/acr.copy_registry_url.registry");
    public static final Action.Id<ContainerRegistry> COPY_LOGIN_COMMAND = Action.Id.of("user/acr.copy_login_command.registry");
    public static final Action.Id<ContainerRegistry> LOGIN = Action.Id.of("user/acr.login_registry.registry");
    public static final Action.Id<ContainerRegistry> LOGOUT = Action.Id.of("user/acr.logout_registry.registry");
    public static final Action.Id<ContainerRegistry> COPY_USERNAME = Action.Id.of("user/acr.copy_username.registry");
    public static final Action.Id<ContainerRegistry> COPY_PASSWORD = Action.Id.of("user/acr.copy_password.registry");
    public static final Action.Id<Tag> PULL_IMAGE = Action.Id.of("user/acr.pull_image.image");
    public static final Action.Id<Tag> COPY_DIGEST = Action.Id.of("user/acr.copy_digest.image");
    public static final Action.Id<Tag> COPY_PULL_COMMAND = Action.Id.of("user/acr.copy_pull_command.image");
    public static final Action.Id<Tag> COPY_RUN_COMMAND = Action.Id.of("user/acr.copy_run_command.image");
    public static final Action.Id<Tag> DEPLOY_IMAGE_ACA = Action.Id.of("user/acr.deploy_image_aca.image");
    public static final Action.Id<Tag> DEPLOY_IMAGE_WEBAPP = Action.Id.of("user/acr.deploy_image_webapp.image");
    public static final Action.Id<Tag> INSPECT_IMAGE = Action.Id.of("user/acr.inspect_image.image");
    public static final Action.Id<Tag> RUN_LOCALLY = Action.Id.of("user/acr.run_image.image");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_CONTAINER_REGISTRY = Action.Id.of("user/acr.create_registry.group");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(PUSH_IMAGE)
            .withLabel("Push Image")
            .withIcon(AzureIcons.DockerSupport.PUSH_IMAGE.getIconPath())
            .visibleWhen(s -> s instanceof ContainerRegistry)
            .enableWhen(s -> s.getFormalStatus().isRunning())
            .withIdParam(AzResource::getName)
            .register(am);

        new Action<>(ENABLE_ADMIN_USER)
            .withLabel("Enable Admin User")
            .visibleWhen(s -> s instanceof ContainerRegistry && !((ContainerRegistry) s).isAdminUserEnabled())
            .withIdParam(AzResource::getName)
            .withHandler(s -> {
                s.enableAdminUser();
                final AzureString message = AzureString.format("Admin user is enabled for Azure Container Registry %s. You can docker login to it now.", s.getName());
                final Action<ContainerRegistry> login = am.getAction(LOGIN).bind(s).withLabel("Login Now!");
                final Action<ContainerRegistry> copyPassword = am.getAction(COPY_PASSWORD).bind(s);
                final Action<ContainerRegistry> copyLoginCommand = am.getAction(COPY_LOGIN_COMMAND).bind(s);
                AzureMessager.getMessager().success(message, login, copyPassword, copyLoginCommand);
            })
            .register(am);

        new Action<>(DISABLE_ADMIN_USER)
            .withLabel("Disable Admin User")
            .visibleWhen(s -> s instanceof ContainerRegistry && ((ContainerRegistry) s).isAdminUserEnabled())
            .withIdParam(AzResource::getName)
            .withHandler(s -> {
                final String message = "You will not be able to docker login to this container registry if admin user is disabled, Azure you sure to disable it?";
                if (AzureMessager.getMessager().confirm(message)) {
                    s.disableAdminUser();
                    final Action<ContainerRegistry> logout = am.getAction(LOGOUT).bind(s);
                    AzureMessager.getMessager().success(AzureString.format("Admin user is disabled for Azure Container Registry %s", s.getName()), logout);
                }
            })
            .register(am);

        new Action<>(COPY_LOGIN_COMMAND)
            .withIcon(AzureIcons.Action.COPY.getIconPath())
            .withLabel("Copy Login Command")
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof ContainerRegistry)
            .enableWhen(ContainerRegistry::isAdminUserEnabled)
            .withHandler(s -> {
                final String command = String.format("docker login %s -u %s -p %s", s.getLoginServerUrl(), s.getUserName(), s.getPrimaryCredential());
                am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(command);
                AzureMessager.getMessager().success(AzureString.format("login command %s is copied into clipboard", command));
            })
            .register(am);

        new Action<>(LOGIN)
            .withLabel("Docker Login")
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof ContainerRegistry)
            .register(am);

        new Action<>(LOGOUT)
            .withLabel("Docker Logout")
            .withIdParam(AbstractAzResource::getName)
            .visibleWhen(s -> s instanceof ContainerRegistry)
            .register(am);

        new Action<>(COPY_REGISTRY_URL)
            .withIcon(AzureIcons.Action.COPY.getIconPath())
            .withLabel("Copy Registry URL")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ContainerRegistry)
            .withHandler(s -> {
                final Action<ContainerRegistry> login = am.getAction(LOGIN).bind(s);
                final Action<ContainerRegistry> copyPassword = am.getAction(COPY_PASSWORD).bind(s);
                am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(s.getLoginServerUrl());
                AzureMessager.getMessager().success(AzureString.format("Registry URL %s is copied into clipboard.", s.getLoginServerUrl()), login, copyPassword);
            })
            .register(am);

        new Action<>(COPY_USERNAME)
            .withIcon(AzureIcons.Action.COPY.getIconPath())
            .withLabel("Copy Admin Username")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ContainerRegistry)
            .withHandler(s -> {
                if (!s.isAdminUserEnabled()) {
                    final Action<ContainerRegistry> enableAdminUser = am.getAction(ContainerRegistryActionsContributor.ENABLE_ADMIN_USER).bind(s);
                    throw new AzureToolkitRuntimeException("Admin user is not enabled.", enableAdminUser);
                }
                final Action<ContainerRegistry> login = am.getAction(LOGIN).bind(s);
                final Action<ContainerRegistry> copyPassword = am.getAction(COPY_PASSWORD).bind(s);
                am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(s.getUserName());
                AzureMessager.getMessager().success(AzureString.format("Username %s is copied into clipboard.", s.getUserName()), login, copyPassword);
            })
            .register(am);

        new Action<>(COPY_PASSWORD)
            .withIcon(AzureIcons.Action.COPY.getIconPath())
            .withLabel("Copy Admin Password")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ContainerRegistry)
            .withHandler(s -> {
                if (!s.isAdminUserEnabled()) {
                    final Action<ContainerRegistry> enableAdminUser = am.getAction(ContainerRegistryActionsContributor.ENABLE_ADMIN_USER).bind(s);
                    throw new AzureToolkitRuntimeException("Admin user is not enabled.", enableAdminUser);
                }
                final Action<ContainerRegistry> login = am.getAction(LOGIN).bind(s);
                final Action<ContainerRegistry> copyRegistryUrl = am.getAction(COPY_REGISTRY_URL).bind(s);
                am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(s.getPrimaryCredential());
                AzureMessager.getMessager().success("Primary password is copied into clipboard.", login, copyRegistryUrl);
            })
            .register(am);

        new Action<>(COPY_DIGEST)
            .withIcon(AzureIcons.Action.COPY.getIconPath())
            .withLabel("Copy Digest")
            .withIdParam(Tag::getImageName)
            .visibleWhen(s -> s instanceof Tag)
            .withHandler(s -> {
                am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(s.getDigest());
                AzureMessager.getMessager().success(AzureString.format("Digest %s is copied into clipboard.", s.getDigest()));
            })
            .register(am);

        new Action<>(COPY_PULL_COMMAND)
            .withIcon(AzureIcons.Action.COPY.getIconPath())
            .withLabel("Copy Pull Command")
            .withIdParam(Tag::getImageName)
            .visibleWhen(s -> s instanceof Tag)
            .withHandler(s -> {
                final String command = String.format("docker pull %s", s.getFullName());
                am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(command);
                final Action<Tag> pull = am.getAction(ContainerRegistryActionsContributor.PULL_IMAGE).bind(s).withLabel("Pull Now!");
                final ContainerRegistry registry = s.getParent().getParent().getParent();
                if (!registry.isAdminUserEnabled()) {
                    final Action<ContainerRegistry> enableAdminUser = am.getAction(ContainerRegistryActionsContributor.ENABLE_ADMIN_USER).bind(registry);
                    final AzureString message = AzureString.format("Pull command %s is copied into clipboard. " +
                        "But it may fail because admin user is not enabled for Azure Container Registry %s", command, registry.getName());
                    AzureMessager.getMessager().success(message, enableAdminUser, pull);
                } else {
                    AzureMessager.getMessager().success(AzureString.format("Pull command %s is copied into clipboard", command), pull);
                }
            })
            .register(am);

        new Action<>(COPY_RUN_COMMAND)
            .withIcon(AzureIcons.Action.COPY.getIconPath())
            .withLabel("Copy Run Command")
            .withIdParam(Tag::getImageName)
            .visibleWhen(s -> s instanceof Tag)
            .withHandler(s -> {
                final String command = String.format("docker run -p 8080:8080 %s", s.getFullName());
                am.getAction(ResourceCommonActionsContributor.COPY_STRING).handle(command);
                AzureMessager.getMessager().success(AzureString.format("Run command %s is copied into clipboard", command));
            })
            .register(am);

        new Action<>(PULL_IMAGE)
            .withIcon(t -> AzureIcons.DockerSupport.PULL_IMAGE.getIconPath())
            .withLabel("Pull Image")
            .withIdParam(Tag::getImageName)
            .visibleWhen(s -> s instanceof Tag)
            .register(am);

        new Action<>(INSPECT_IMAGE)
            .withIcon(t -> AzureIcons.Action.SEARCH.getIconPath())
            .withLabel("Inspect")
            .withIdParam(Tag::getImageName)
            .visibleWhen(s -> s instanceof Tag)
            .register(am);

        new Action<>(RUN_LOCALLY)
            .withIcon(t -> AzureIcons.Action.START.getIconPath())
            .withLabel("Run as Container")
            .withIdParam(Tag::getImageName)
            .visibleWhen(s -> s instanceof Tag)
            .register(am);

        new Action<>(DEPLOY_IMAGE_ACA)
            .withIcon(t -> AzureIcons.Action.DEPLOY.getIconPath())
            .withLabel("Deploy to Container App")
            .withIdParam(Tag::getImageName)
            .visibleWhen(s -> s instanceof Tag)
            .register(am);

        new Action<>(DEPLOY_IMAGE_WEBAPP)
            .withIcon(t -> AzureIcons.Action.DEPLOY.getIconPath())
            .withLabel("Deploy to Web App")
            .withIdParam(Tag::getImageName)
            .visibleWhen(s -> s instanceof Tag)
            .register(am);

        new Action<>(GROUP_CREATE_CONTAINER_REGISTRY)
            .withLabel("Container Registry")
            .withIdParam(AzResource::getName)
            .visibleWhen(s -> s instanceof ResourceGroup)
            .enableWhen(s -> s.getFormalStatus().isConnected())
            .withHandler(s -> am.getAction(ResourceCommonActionsContributor.CREATE_IN_PORTAL).handle(Azure.az(AzureContainerRegistry.class)))
            .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.BROWSE_SERVICE_AZURE_SAMPLES,
            "---",
            ResourceCommonActionsContributor.CREATE_IN_PORTAL
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup registryActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
            ResourceCommonActionsContributor.BROWSE_SERVICE_AZURE_SAMPLES,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            ContainerRegistryActionsContributor.ENABLE_ADMIN_USER,
            ContainerRegistryActionsContributor.DISABLE_ADMIN_USER,
            "---",
            ContainerRegistryActionsContributor.COPY_REGISTRY_URL,
            ContainerRegistryActionsContributor.COPY_USERNAME,
            ContainerRegistryActionsContributor.COPY_PASSWORD,
            "---",
            ContainerRegistryActionsContributor.PUSH_IMAGE,
            "---",
            ResourceCommonActionsContributor.OPEN_IN_SERVICES_VIEW
        );
        am.registerGroup(REGISTRY_ACTIONS, registryActionGroup);

        final ActionGroup repositoryActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.PIN,
            "---",
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(REPOSITORY_ACTIONS, repositoryActionGroup);

        final ActionGroup tagActionGroup = new ActionGroup(
            ResourceCommonActionsContributor.REFRESH,
            ResourceCommonActionsContributor.OPEN_PORTAL_URL,
            "---",
            ContainerRegistryActionsContributor.DEPLOY_IMAGE_ACA,
            ContainerRegistryActionsContributor.DEPLOY_IMAGE_WEBAPP,
            "---",
            ContainerRegistryActionsContributor.PULL_IMAGE,
            ContainerRegistryActionsContributor.INSPECT_IMAGE,
            "---",
            ContainerRegistryActionsContributor.COPY_DIGEST,
            ContainerRegistryActionsContributor.COPY_PULL_COMMAND,
            "---",
            ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(TAG_ACTIONS, tagActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_CONTAINER_REGISTRY);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}

