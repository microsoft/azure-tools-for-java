/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerservice.property;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import com.microsoft.azure.toolkit.intellij.common.AzureHideableTitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.component.TextFieldUtils;
import com.microsoft.azure.toolkit.intellij.common.properties.AzResourcePropertiesEditor;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesCluster;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesClusterAgentPool;
import com.microsoft.azure.toolkit.lib.containerservice.model.ContainerServiceNetworkProfile;
import com.microsoft.azure.toolkit.lib.containerservice.model.LoadBalancerSku;
import com.microsoft.azure.toolkit.lib.containerservice.model.NetworkPlugin;
import com.microsoft.azure.toolkit.lib.containerservice.model.NetworkPolicy;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.Optional;

public class KubernetesServicePropertiesEditor extends AzResourcePropertiesEditor<KubernetesCluster> {
    private JPanel pnlContent;
    private JPanel propertyActionPanel;
    private AzureActionButton<Void> btnRefresh;
    private AzureHideableTitledSeparator overviewSeparator;
    private JTextField resourceGroupTextField;
    private JTextField txtKubernetesVersion;
    private JTextField statusTextField;
    private JTextField txtApiServerAddress;
    private JTextField locationTextField;
    private JTextField txtNetworkType;
    private JTextField subscriptionTextField;
    private JTextField txtNodePools;
    private JTextField subscriptionIDTextField;
    private JPanel pnlNodePools;
    private JBTable nodeTables;
    private JPanel pnlOverview;
    private JLabel lblResourceGroup;
    private JLabel lblStatus;
    private JLabel lblLocation;
    private JLabel lblSubscription;
    private JLabel lblSubscriptionId;
    private JLabel lblKuberneteVersion;
    private JLabel lblApiServerAddress;
    private JLabel lblNetworkType;
    private JLabel lblNodePools;
    private JLabel lblNetworkPolicy;
    private JTextField txtNetworkPolicy;
    private JLabel lblPodCidr;
    private JTextField txtPodCidr;
    private JLabel lblServiceCidr;
    private JTextField txtServiceCidr;
    private JLabel lblDnsServiceIP;
    private JTextField txtDndServiceIp;
    private JLabel lblDockerBridgeCidr;
    private JTextField txtDockerBridgeCidr;
    private JLabel lblLoadBalancer;
    private JTextField txtLoadBalancer;
    private AzureHideableTitledSeparator networkingSeparator;
    private JPanel pnlNetworking;
    private AzureHideableTitledSeparator nodePoolSeparator;
    private JPanel pnlRoot;

    private KubernetesCluster cluster;

    public KubernetesServicePropertiesEditor(@Nonnull VirtualFile virtualFile, @Nonnull KubernetesCluster resource, @Nonnull Project project) {
        super(virtualFile, resource, project);
        this.cluster = resource;
        init();
        rerender();
    }

    private void init() {
        final DefaultTableModel model = new DefaultTableModel() {
            public boolean isCellEditable(int var1, int var2) {
                return false;
            }
        };
        model.addColumn("Node pool");
        model.addColumn("Provisioning state");
        model.addColumn("Power State");
        model.addColumn("Mode");
        model.addColumn("Kubernetes version");
        model.addColumn("Node size");
        model.addColumn("Operating system");
        this.nodeTables.setModel(model);
        this.nodeTables.setRowSelectionAllowed(true);
        this.nodeTables.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.nodeTables.getEmptyText().setText("Loading pools");
        this.nodeTables.setBorder(BorderFactory.createEmptyBorder());

        this.lblApiServerAddress.setLabelFor(txtApiServerAddress);
        this.lblDnsServiceIP.setLabelFor(txtDndServiceIp);
        this.lblLocation.setLabelFor(locationTextField);
        this.lblLoadBalancer.setLabelFor(txtLoadBalancer);
        this.lblKuberneteVersion.setLabelFor(txtKubernetesVersion);
        this.lblDockerBridgeCidr.setLabelFor(txtDockerBridgeCidr);
        this.lblNetworkPolicy.setLabelFor(txtNetworkPolicy);
        this.lblNetworkType.setLabelFor(txtNetworkType);
        this.lblNodePools.setLabelFor(txtNodePools);
        this.lblPodCidr.setLabelFor(txtPodCidr);
        this.lblResourceGroup.setLabelFor(resourceGroupTextField);
        this.lblStatus.setLabelFor(statusTextField);
        this.lblServiceCidr.setLabelFor(txtServiceCidr);
        this.lblSubscription.setLabelFor(subscriptionTextField);
        this.lblSubscriptionId.setLabelFor(subscriptionIDTextField);
        TextFieldUtils.disableTextBoard(resourceGroupTextField, statusTextField, locationTextField, subscriptionTextField,
                subscriptionIDTextField, txtApiServerAddress, txtDndServiceIp, txtLoadBalancer, txtKubernetesVersion, txtDockerBridgeCidr, txtNetworkPolicy,
                txtNetworkType, txtNodePools, txtPodCidr, txtServiceCidr);
        TextFieldUtils.makeTextOpaque(resourceGroupTextField, statusTextField, locationTextField, subscriptionTextField,
                subscriptionIDTextField, txtApiServerAddress, txtDndServiceIp, txtLoadBalancer, txtKubernetesVersion, txtDockerBridgeCidr, txtNetworkPolicy,
                txtNetworkType, txtNodePools, txtPodCidr, txtServiceCidr);
        final Action<Void> refreshAction = new Action<Void>(Action.Id.of("user/kubernetes.refresh_properties_view.kubernetes"))
                .withAuthRequired(true)
                .withSource(this.cluster)
                .withIdParam(this.cluster.getName())
                .withHandler(ignore -> {
                    cluster.refresh();
                    rerender();
                });
        this.btnRefresh.setAction(refreshAction);

        this.overviewSeparator.addContentComponent(pnlOverview);
        this.nodePoolSeparator.addContentComponent(pnlNodePools);
        this.networkingSeparator.addContentComponent(pnlNetworking);
    }

    @Override
    protected void rerender() {
        AzureTaskManager.getInstance().runLater(() -> this.setData(this.cluster));
    }

    private void setData(final KubernetesCluster server) {
        resourceGroupTextField.setText(server.getResourceGroupName());
        statusTextField.setText(server.getStatus());
        locationTextField.setText(server.getRegion().getLabel()); // region
        final Subscription subscription = Azure.az(AzureAccount.class).account().getSubscription(server.getSubscriptionId());
        subscriptionTextField.setText(subscription.getName());
        subscriptionIDTextField.setText(subscription.getId());
        txtKubernetesVersion.setText(server.getKubernetesVersion());
        txtApiServerAddress.setText(Optional.ofNullable(server.getApiServerAddress()).orElse("N/A"));

        final ContainerServiceNetworkProfile profile = server.getContainerServiceNetworkProfile();
        txtNetworkType.setText(Optional.ofNullable(profile).map(ContainerServiceNetworkProfile::getNetworkPlugin).map(NetworkPlugin::getValue).orElse("None"));
        txtNetworkPolicy.setText(Optional.ofNullable(profile).map(ContainerServiceNetworkProfile::getNetworkPolicy).map(NetworkPolicy::getValue).orElse("None"));
        txtLoadBalancer.setText(Optional.ofNullable(profile).map(ContainerServiceNetworkProfile::getLoadBalancerSku).map(LoadBalancerSku::getValue).orElse("None"));
        txtPodCidr.setText(Optional.ofNullable(profile).map(ContainerServiceNetworkProfile::getPodCidr).orElse("N/A"));
        txtServiceCidr.setText(Optional.ofNullable(profile).map(ContainerServiceNetworkProfile::getServiceCidr).orElse("N/A"));
        txtDndServiceIp.setText(Optional.ofNullable(profile).map(ContainerServiceNetworkProfile::getDnsServiceIp).orElse("N/A"));
        txtDockerBridgeCidr.setText("N/A");
        AzureTaskManager.getInstance().runInBackground("Loading node pools", () -> cluster.agentPools().list())
            .thenAccept(pools -> AzureTaskManager.getInstance().runLater(() -> fillNodePools(pools)));
    }

    private void fillNodePools(final List<KubernetesClusterAgentPool> agentPools) {
        txtNodePools.setText(String.format("%d node pools", agentPools.size()));
        final DefaultTableModel model = (DefaultTableModel) this.nodeTables.getModel();
        model.setRowCount(0);
        agentPools.forEach(i -> model.addRow(new Object[]{i.getName(), i.getStatus(), i.getPowerStatus().getValue(), i.getAgentPoolMode().getValue(),
                i.getKubernetesVersion(), i.getVirtualMachineSize().getValue(), i.getOsType().getValue()}));
        final int rows = model.getRowCount() < 5 ? 5 : agentPools.size();
        model.setRowCount(rows);
        this.nodeTables.setVisibleRowCount(rows);
    }

    @Override
    public @NotNull JComponent getComponent() {
        return pnlRoot;
    }
}
