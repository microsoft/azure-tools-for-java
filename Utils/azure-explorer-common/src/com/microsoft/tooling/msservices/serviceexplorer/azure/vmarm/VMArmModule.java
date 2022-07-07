/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm;

import com.microsoft.azure.management.compute.VirtualMachine;
import com.microsoft.azure.management.compute.implementation.ComputeManager;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcon;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azuretools.authmanage.IdeAzureAccount;
import com.microsoft.azuretools.azurecommons.helpers.AzureCmdException;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import com.microsoft.tooling.msservices.components.DefaultLoader;
import com.microsoft.tooling.msservices.serviceexplorer.AzureRefreshableNode;
import com.microsoft.tooling.msservices.serviceexplorer.Node;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VMArmModule extends AzureRefreshableNode {
    private static final String VM_SERVICE_MODULE_ID = com.microsoft.tooling.msservices.serviceexplorer.azure.vmarm.VMArmModule.class.getName();
    private static final String ICON_PATH = "VirtualMachine_16.png";
    private static final String BASE_MODULE_NAME = "Virtual Machines";
    public static final String MODULE_NAME = "Virtual Machine";

    public VMArmModule(Node parent) {
        super(VM_SERVICE_MODULE_ID, BASE_MODULE_NAME, parent, ICON_PATH);
    }

    @Override
    public @Nullable AzureIcon getIconSymbol() {
        return AzureIcons.VirtualMachine.MODULE;
    }

    @Override
    protected void refreshItems() throws AzureCmdException {
        final List<Pair<String, String>> failedSubscriptions = new ArrayList<>();
        try {
            final Account account = com.microsoft.azure.toolkit.lib.Azure.az(AzureAccount.class).account();
            final Set<String> sidList = account.getSelectedSubscriptions().stream()
                .map(Subscription::getId)
                .collect(Collectors.toSet());
            for (final String sid : sidList) {
                try {
                    final ComputeManager.Configurable configurable = ComputeManager.configure();
                    final ComputeManager azure = IdeAzureAccount.getInstance().authenticateForTrack1(sid, configurable, (t, c) -> c.authenticate(t, sid));
                    final List<VirtualMachine> virtualMachines = azure.virtualMachines().list();

                    for (final VirtualMachine vm : virtualMachines) {
                        addChildNode(new VMNode(this, sid, vm));
                    }

                } catch (final Exception ex) {
                    failedSubscriptions.add(new ImmutablePair<>(sid, ex.getMessage()));
                }
            }
        } catch (final Exception ex) {
            DefaultLoader.getUIHelper().logError("An error occurred when trying to load Virtual Machines\n\n" + ex.getMessage(), ex);
        }
        if (!failedSubscriptions.isEmpty()) {
            final StringBuilder errorMessage = new StringBuilder("An error occurred when trying to load Storage Accounts for the subscriptions:\n\n");
            for (final Pair<String, String> error : failedSubscriptions) {
                errorMessage.append(error.getKey()).append(": ").append(error.getValue()).append("\n");
            }
            DefaultLoader.getUIHelper().logError("An error occurred when trying to load Storage Accounts\n\n" + errorMessage.toString(), null);
        }
    }
}
