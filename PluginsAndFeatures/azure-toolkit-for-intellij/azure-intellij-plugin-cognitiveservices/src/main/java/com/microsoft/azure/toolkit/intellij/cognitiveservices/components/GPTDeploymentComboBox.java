/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.cognitiveservices.components;

import com.azure.resourcemanager.cognitiveservices.models.Deployment;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeploymentDraft;
import com.microsoft.azure.toolkit.lib.cognitiveservices.model.DeploymentModel;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import org.apache.commons.collections.ListUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

public class GPTDeploymentComboBox extends AzureComboBox<CognitiveDeployment> {
    private CognitiveAccount account;
    private final List<CognitiveDeployment> draftItems = new LinkedList<>();

    public GPTDeploymentComboBox() {
        super(false);
    }

    public void setAccount(@Nullable final CognitiveAccount account) {
        if (Objects.equals(account, this.account)) {
            return;
        }
        this.account = account;
        if (account == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Override
    public void setValue(@Nullable CognitiveDeployment deployment, Boolean fixed) {
        if (Objects.nonNull(deployment) && deployment.isDraftForCreating()) {
            this.draftItems.remove(deployment);
            this.draftItems.add(0, deployment);
            this.reloadItems();
        }
        super.setValue(deployment, fixed);
    }

    @Nonnull
    @Override
    protected List<? extends CognitiveDeployment> loadItems() {
        final List<CognitiveDeployment> cognitiveDeployments = Optional.ofNullable(account)
                .map(s -> s.deployments().list().stream()
                        .filter(deployment -> deployment.getModel().isGPTModel()).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
        final List<CognitiveDeployment> union = ListUtils.union(draftItems, cognitiveDeployments);
        return union.stream().filter(deployment -> Objects.equals(deployment.getParent(), account)).collect(Collectors.toList());
    }

    @Nonnull
    @Override
    protected List<ExtendableTextComponent.Extension> getExtensions() {
        return Collections.emptyList();
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof CognitiveDeployment) {
            final CognitiveDeployment deployment = (CognitiveDeployment) item;
            final DeploymentModel model = deployment.getModel();
            return (deployment instanceof CognitiveDeploymentDraft && !deployment.exists()) ? String.format("(New) %s", deployment.getName()) :
                    Objects.isNull(model) ? deployment.getName() : String.format("%s (%s %s)", deployment.getName(), model.getName(), model.getVersion());
        }
        return super.getItemText(item);
    }
}
