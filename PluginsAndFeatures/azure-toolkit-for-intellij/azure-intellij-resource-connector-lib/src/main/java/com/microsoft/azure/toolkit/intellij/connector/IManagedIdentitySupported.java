/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.connector;

import com.azure.resourcemanager.authorization.models.BuiltInRole;
import com.azure.resourcemanager.authorization.models.RoleAssignment;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Interfaced to indicate that the resource supports managed identity
 * <p>
 */
public interface IManagedIdentitySupported<T extends AzResource> {
    String FAILED_TO_ASSIGN_MESSAGE = "Failed to grant permission to identity <a href=\"%s\">%s</a>, %s, please try assign correct role to it in portal";

    Map<String, String> initIdentityEnv(Connection<T, ?> data, Project project);

    /**
     * Get required permissions to access the target resource
     *
     * @return List of required permissions
     */
    List<String> getRequiredPermissions();

    /**
     * Get built-in roles that could be assigned to the identity to get access to target resource
     *
     * @return Map (role id, BuildInRole), refers https://learn.microsoft.com/en-us/azure/role-based-access-control/built-in-roles
     */
    @Nullable Map<String, BuiltInRole> getBuiltInRoles();

    /**
     * Check whether identity has required permissions to access resource
     */
    public static boolean checkPermission(@Nonnull AzureServiceResource<?> data, @Nonnull String identity) {
        final AzureServiceResource.Definition<?> d = data.getDefinition();
        final AzResource r = data.getData();
        if (d instanceof IManagedIdentitySupported<?> definition && r instanceof AbstractAzResource<?, ?, ?> resource) {
            final List<String> permissions = resource.getPermissions(identity);
            final List<String> requiredPermissions = definition.getRequiredPermissions();
            return CollectionUtils.containsAll(permissions, requiredPermissions);
        }
        return true;
    }

    @Nonnull
    public static Action<?> getOpenIdentityConfigurationAction(@Nonnull AzureServiceResource<?> data) {
        final String url = Objects.requireNonNull(data.getData()).getPortalUrl() + "/iamAccessControl";
        return AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).bind(url).withLabel("Open IAM Configuration");
    }

    @AzureOperation(name = "user/common.assign_role.identity", params = {"identity"})
    public static boolean grantPermission(@Nonnull AzureServiceResource<?> data, @Nonnull String identity) {
        final AzureServiceResource.Definition<?> d = data.getDefinition();
        final AzResource r = data.getData();
        if (d instanceof IManagedIdentitySupported<?> definition && MapUtils.isNotEmpty(definition.getBuiltInRoles()) && r instanceof AbstractAzResource<?, ?, ?> resource) {
            final Map<String, BuiltInRole> builtInRoles = definition.getBuiltInRoles();
            final List<String> roles = resource.getRoleAssignments(identity).stream().map(RoleAssignment::roleDefinitionId).toList();
            final String rolesStr = builtInRoles.values().stream().map(BuiltInRole::toString).collect(Collectors.joining(","));
            final boolean assignRole = AzureMessager.getDefaultMessager().confirm(String.format("Do you want to assign roles (%s) to identity (%s)?", rolesStr, identity), "Assign Required Roles");
            if (assignRole) {
                try {
                    Objects.requireNonNull(definition.getBuiltInRoles()).forEach((id, role) -> {
                        if (roles.stream().noneMatch(ro -> ro.endsWith(id))) {
                            AzureMessager.getMessager().info(String.format("Assigning role (%s) to identity (%s)...", role, identity));
                            resource.grantPermissionToIdentity(identity, role);
                        }
                    });
                    AzureMessager.getMessager().info(String.format("Roles (%s) have been assigned to identity (%s).", rolesStr, identity));
                    return true;
                } catch (final RuntimeException e) {
                    final String errorMessage = String.format(FAILED_TO_ASSIGN_MESSAGE, resource.getPortalUrl(), identity, e.getMessage());
                    AzureMessager.getMessager().warning(errorMessage, getOpenIdentityConfigurationAction(data));
                }
            }
        }
        return false;
    }

    @Nullable
    public static Action<?> getGrantPermissionAction(@Nonnull AzureServiceResource<?> data, @Nonnull String identity) {
        final AzureServiceResource.Definition<?> d = data.getDefinition();
        final AzResource r = data.getData();
        if (d instanceof IManagedIdentitySupported<?> definition && MapUtils.isNotEmpty(definition.getBuiltInRoles()) && r instanceof AbstractAzResource<?, ?, ?> resource) {
            return new Action<>(Action.Id.of("user/common.assign_role.identity"))
                    .withLabel("Assign Required Roles")
                    .withIdParam(identity)
                    .withHandler(ignore -> grantPermission(data, identity));
        }
        return null;
    }
}
