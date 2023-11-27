/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.keyvault;

import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionGroup;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.action.IActionGroup;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.keyvault.Credential;
import com.microsoft.azure.toolkit.lib.keyvault.CredentialVersion;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang3.BooleanUtils;

import java.util.Optional;

public class KeyVaultActionsContributor implements IActionsContributor {
    public static final int INITIALIZE_ORDER = ResourceCommonActionsContributor.INITIALIZE_ORDER + 1;
    public static final String SERVICE_ACTIONS = "actions.keyvault.service";
    public static final String KEY_VAULT_ACTIONS = "actions.keyvault.instance";
    public static final String MODULE_ACTIONS = "actions.keyvault.module";
    public static final String SECRET_ACTIONS = "actions.keyvault.secret";
    public static final String SECRET_VERSION_ACTIONS = "actions.keyvault.secret_version";
    public static final String CERTIFICATE_ACTIONS = "actions.keyvault.certificate";
    public static final String CERTIFICATE_VERSION_ACTIONS = "actions.keyvault.certificate_version";
    public static final String KEY_ACTIONS = "actions.keyvault.key";
    public static final String KEY_VERSION_ACTIONS = "actions.keyvault.key_version";
    public static final Action.Id<Credential> ENABLE_CREDENTIAL = Action.Id.of("user/keyvault.enable_credential.type|credential");
    public static final Action.Id<Credential> DISABLE_CREDENTIAL = Action.Id.of("user/keyvault.disable_credential.type|credential");
    public static final Action.Id<CredentialVersion> ENABLE_CREDENTIAL_VERSION = Action.Id.of("user/keyvault.enable_credential_version.type|version|credential");
    public static final Action.Id<CredentialVersion> DISABLE_CREDENTIAL_VERSION = Action.Id.of("user/keyvault.disable_credential_version.type|version|credential");
    public static final Action.Id<Credential> SHOW_CREDENTIAL = Action.Id.of("user/keyvault.show_credential.type|credential");
    public static final Action.Id<CredentialVersion> SHOW_CREDENTIAL_VERSION = Action.Id.of("user/keyvault.show_credential_version.type|version|credential");
    public static final Action.Id<Credential> DOWNLOAD_CREDENTIAL = Action.Id.of("user/keyvault.download_credential.type|credential");
    public static final Action.Id<CredentialVersion> DOWNLOAD_CREDENTIAL_VERSION = Action.Id.of("user/keyvault.download_credential_version.type|version|credential");
    public static final Action.Id<ResourceGroup> GROUP_CREATE_KEY_VAULT = Action.Id.of("user/keyvault.create_key_vault.group");

    @Override
    public void registerActions(AzureActionManager am) {
        new Action<>(ENABLE_CREDENTIAL)
                .withLabel("Enable")
                .withIdParam(AzResource::getResourceTypeName)
                .withIdParam(AzResource::getName)
                .enableWhen(s -> BooleanUtils.isFalse(s.isEnabled()))
                .withHandler((s, r) -> s.enable())
                .register(am);

        new Action<>(ENABLE_CREDENTIAL_VERSION)
                .withLabel("Enable")
                .withIdParam(AzResource::getResourceTypeName)
                .withIdParam(AzResource::getName)
                .withIdParam(version -> version.getCredential().getName())
                .enableWhen(s -> BooleanUtils.isFalse(s.isEnabled()))
                .withHandler((s, r) -> s.enable())
                .register(am);

        new Action<>(DISABLE_CREDENTIAL)
                .withLabel("Disable")
                .withIdParam(AzResource::getResourceTypeName)
                .withIdParam(AzResource::getName)
                .enableWhen(s -> BooleanUtils.isTrue(s.isEnabled()))
                .withHandler((s, r) -> s.disable())
                .register(am);

        new Action<>(DISABLE_CREDENTIAL_VERSION)
                .withLabel("Disable")
                .withIdParam(AzResource::getResourceTypeName)
                .withIdParam(AzResource::getName)
                .withIdParam(version -> version.getCredential().getName())
                .enableWhen(s -> BooleanUtils.isTrue(s.isEnabled()))
                .withHandler((s, r) -> s.disable())
                .register(am);

        new Action<>(DOWNLOAD_CREDENTIAL)
                .withLabel("Download")
                .withIdParam(AzResource::getResourceTypeName)
                .withIdParam(AzResource::getName)
                .withHandler((s, r) -> Optional.ofNullable(s.getCurrentVersion())
                        .ifPresent(version -> am.getAction(DOWNLOAD_CREDENTIAL_VERSION).handle(version, r)))
                .register(am);

        new Action<>(DOWNLOAD_CREDENTIAL_VERSION)
                .withLabel("Download")
                .withIdParam(AzResource::getResourceTypeName)
                .withIdParam(AzResource::getName)
                .withIdParam(version -> version.getCredential().getName())
                .register(am);

        new Action<>(SHOW_CREDENTIAL)
                .withLabel("Show")
                .withIdParam(AzResource::getResourceTypeName)
                .withIdParam(AzResource::getName)
                .withHandler((s, r) -> Optional.ofNullable(s.getCurrentVersion())
                        .ifPresent(version -> am.getAction(SHOW_CREDENTIAL_VERSION).handle(version, r)))
                .register(am);

        new Action<>(SHOW_CREDENTIAL_VERSION)
                .withLabel("Show")
                .withIdParam(AzResource::getResourceTypeName)
                .withIdParam(AzResource::getName)
                .withIdParam(version -> version.getCredential().getName())
                .register(am);

        new Action<>(GROUP_CREATE_KEY_VAULT)
                .withLabel("Key Vault")
                .withIdParam(AzResource::getName)
                .visibleWhen(s -> s instanceof ResourceGroup)
                .enableWhen(s -> s.getFormalStatus().isConnected())
                .register(am);
    }

    @Override
    public void registerGroups(AzureActionManager am) {
        final ActionGroup serviceActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                "---",
                ResourceCommonActionsContributor.CREATE
        );
        am.registerGroup(SERVICE_ACTIONS, serviceActionGroup);

        final ActionGroup keyVaultActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK,
                ResourceCommonActionsContributor.OPEN_PORTAL_URL,
                "---",
                ResourceCommonActionsContributor.CONNECT,
                "---",
                ResourceCommonActionsContributor.DELETE
        );
        am.registerGroup(KEY_VAULT_ACTIONS, keyVaultActionGroup);

        final ActionGroup keyVaultSubModuleActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.REFRESH,
                "---",
                ResourceCommonActionsContributor.CREATE,
                "---",
                ResourceCommonActionsContributor.OPEN_AZURE_REFERENCE_BOOK
        );
        am.registerGroup(MODULE_ACTIONS, keyVaultSubModuleActionGroup);

        final ActionGroup secretActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                "---",
                am.getAction(ResourceCommonActionsContributor.CREATE).bind(null).withLabel("Create New Version"),
                ResourceCommonActionsContributor.DELETE,
                "---",
                ResourceCommonActionsContributor.SHOW_PROPERTIES,
                am.getAction(SHOW_CREDENTIAL).bind(null).withLabel("Show Secret").enableWhen(s -> s.isEnabled()),
                am.getAction(DOWNLOAD_CREDENTIAL).bind(null).withLabel("Download Secret").enableWhen(s -> s.isEnabled())
        );
        am.registerGroup(SECRET_ACTIONS, secretActionGroup);

        final ActionGroup certificateActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                "---",
                am.getAction(ResourceCommonActionsContributor.CREATE).bind(null).withLabel("Create New Version"),
                ResourceCommonActionsContributor.DELETE,
                "---",
                ResourceCommonActionsContributor.SHOW_PROPERTIES,
                am.getAction(SHOW_CREDENTIAL).bind(null).withLabel("Show Certificate"),
                am.getAction(DOWNLOAD_CREDENTIAL).bind(null).withLabel("Download Certificate")
        );
        am.registerGroup(CERTIFICATE_ACTIONS, certificateActionGroup);

        final ActionGroup keyActionGroup = new ActionGroup(
                ResourceCommonActionsContributor.PIN,
                "---",
                ResourceCommonActionsContributor.REFRESH,
                "---",
                am.getAction(ResourceCommonActionsContributor.CREATE).bind(null).withLabel("Create New Key"),
                ResourceCommonActionsContributor.DELETE,
                "---",
                ResourceCommonActionsContributor.SHOW_PROPERTIES,
                am.getAction(SHOW_CREDENTIAL).bind(null).withLabel("Show Key"),
                am.getAction(DOWNLOAD_CREDENTIAL).bind(null).withLabel("Download Key")
        );
        am.registerGroup(KEY_ACTIONS, keyActionGroup);

        final ActionGroup secretVersionActionGroup = new ActionGroup(
                ENABLE_CREDENTIAL_VERSION,
                DISABLE_CREDENTIAL_VERSION,
                ResourceCommonActionsContributor.SHOW_PROPERTIES,
                am.getAction(SHOW_CREDENTIAL_VERSION).bind(null).withLabel("Show Secret").enableWhen(s -> s.isEnabled()),
                am.getAction(DOWNLOAD_CREDENTIAL_VERSION).bind(null).withLabel("Download Secret").enableWhen(s -> s.isEnabled())
        );
        am.registerGroup(SECRET_VERSION_ACTIONS, secretVersionActionGroup);
        final ActionGroup certificateVersionActionGroup = new ActionGroup(
                ENABLE_CREDENTIAL_VERSION,
                DISABLE_CREDENTIAL_VERSION,
                ResourceCommonActionsContributor.SHOW_PROPERTIES,
                am.getAction(SHOW_CREDENTIAL_VERSION).bind(null).withLabel("Show Certificate"),
                am.getAction(DOWNLOAD_CREDENTIAL_VERSION).bind(null).withLabel("Download Certificate")
        );
        am.registerGroup(CERTIFICATE_VERSION_ACTIONS, certificateVersionActionGroup);
        final ActionGroup keyVersionActionGroup = new ActionGroup(
                ENABLE_CREDENTIAL_VERSION,
                DISABLE_CREDENTIAL_VERSION,
                ResourceCommonActionsContributor.SHOW_PROPERTIES,
                am.getAction(SHOW_CREDENTIAL_VERSION).bind(null).withLabel("Show Key"),
                am.getAction(DOWNLOAD_CREDENTIAL_VERSION).bind(null).withLabel("Download Key")
        );
        am.registerGroup(KEY_VERSION_ACTIONS, keyVersionActionGroup);

        final IActionGroup group = am.getGroup(ResourceCommonActionsContributor.RESOURCE_GROUP_CREATE_ACTIONS);
        group.addAction(GROUP_CREATE_KEY_VAULT);
    }

    @Override
    public int getOrder() {
        return INITIALIZE_ORDER;
    }
}
