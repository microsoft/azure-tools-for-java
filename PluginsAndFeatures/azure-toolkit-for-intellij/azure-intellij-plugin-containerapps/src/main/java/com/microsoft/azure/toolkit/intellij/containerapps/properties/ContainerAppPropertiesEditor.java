/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.properties;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.table.JBTable;
import com.microsoft.azure.toolkit.intellij.common.AzureActionButton;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureHideableTitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.component.TextFieldUtils;
import com.microsoft.azure.toolkit.intellij.common.properties.AzResourcePropertiesEditor;
import com.microsoft.azure.toolkit.intellij.containerapps.component.EnableComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft.ScaleConfig;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.Revision;
import com.microsoft.azure.toolkit.lib.containerapps.model.*;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

public class ContainerAppPropertiesEditor extends AzResourcePropertiesEditor<ContainerApp> {
    public static final String N_A = "N/A";
    private JPanel pnlContent;
    private JPanel propertyActionPanel;
    private AzureActionButton<Void> btnRefresh;
    private AzureHideableTitledSeparator overviewSeparator;
    private JBLabel resourceGroupTextField;
    private JBLabel txtProvisioningStatus;
    private JBLabel txtRevisionMode;
    private JBLabel locationTextField;
    private JBLabel txtLatestRevisionName;
    private JBLabel subscriptionTextField;
    private JBLabel txtContainerAppsEnvironment;
    private JBLabel subscriptionIDTextField;
    private JPanel pnlNodePools;
    private JBTable revisionsTable;
    private JPanel pnlOverview;
    private JLabel lblResourceGroup;
    private JLabel lblApplicationUrl;
    private JLabel lblLocation;
    private JLabel lblSubscription;
    private JLabel lblSubscriptionId;
    private JLabel lblProvisioningStatus;
    private JLabel lblRevisionMode;
    private JLabel lblLatestRevisionName;
    private JLabel lblContainerAppsEnvironment;
    private JLabel lblIngress;
    private JLabel lblExternalAccess;
    private JLabel lblInsecureConnection;
    private JLabel lblTargetPort;
    private JBIntSpinner txtTargetPort;
    private JLabel lblTransportMethod;
    private AzureHideableTitledSeparator ingressSeparator;
    private JPanel pnlNetworking;
    private AzureHideableTitledSeparator revisionsSeparator;
    private JPanel pnlRoot;
    private AzureActionButton<Void> saveButton;
    private ActionLink resetButton;
    private EnableComboBox cbIngress;
    private EnableComboBox cbExternalAccess;
    private HyperlinkLabel linkApplicationUrl;
    private JTextField txtInsecureConnections;
    private JTextField txtTransportMethod;
    private AzureHideableTitledSeparator titleScale;
    private JPanel pnlScale;
    private JBIntSpinner intMinReplicas;
    private JBIntSpinner intMaxReplicas;
    private JLabel lblWorkloadProfile;
    private JBLabel txtWorkloadProfile;
    private final JTextField[] readOnlyComponents = new JTextField[]{txtInsecureConnections, txtTransportMethod};

    private final ContainerApp containerApp;
    private final ContainerAppDraft draft;
    private final ZoneId zoneId;

    public ContainerAppPropertiesEditor(@Nonnull Project project, @Nonnull ContainerApp resource, @Nonnull VirtualFile virtualFile) {
        super(virtualFile, resource, project);
        this.containerApp = resource;
        this.draft = (ContainerAppDraft) containerApp.update();
        this.zoneId = ZoneId.systemDefault();
        init();
        rerender();
    }

    private void init() {
        final DefaultTableModel model = new DefaultTableModel() {
            public boolean isCellEditable(int var1, int var2) {
                return false;
            }
        };
        final String zoneIdDisplayName = zoneId.getDisplayName(TextStyle.SHORT, Locale.getDefault());
        model.addColumn("Name");
        model.addColumn(String.format("Date created (%s)", zoneIdDisplayName));
        model.addColumn("Provision Status");
        model.addColumn("Traffic");
        model.addColumn("Active");
        this.revisionsTable.setModel(model);
        this.revisionsTable.setRowSelectionAllowed(true);
        this.revisionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.revisionsTable.getEmptyText().setText("Loading pools");
        this.revisionsTable.setBorder(BorderFactory.createEmptyBorder());

        // Ingress
        this.lblIngress.setLabelFor(cbIngress);
        this.lblExternalAccess.setLabelFor(cbExternalAccess);
        this.lblInsecureConnection.setLabelFor(txtInsecureConnections);
        this.lblTargetPort.setLabelFor(txtTargetPort);
        this.lblTransportMethod.setLabelFor(txtInsecureConnections);
        TextFieldUtils.disableTextBoard(readOnlyComponents);
        TextFieldUtils.makeTextOpaque(readOnlyComponents);

        this.txtTargetPort.setMax(65535);
        this.txtTargetPort.setMin(1);
        this.txtTargetPort.setNumber(80);

        initListeners();
        this.overviewSeparator.addContentComponent(pnlOverview);
        this.revisionsSeparator.addContentComponent(pnlNodePools);
        this.ingressSeparator.addContentComponent(pnlNetworking);
        this.titleScale.addContentComponent(pnlScale);
    }

    private void initListeners() {
        this.resetButton.addActionListener(e -> this.reset());
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        final Runnable runnable = () -> AzureTaskManager.getInstance().runOnPooledThread(ContainerAppPropertiesEditor.this::refreshToolbar);
        this.txtTargetPort.addChangeListener(e -> runnable.run());
        this.intMinReplicas.addChangeListener(e -> runnable.run());
        this.intMaxReplicas.addChangeListener(e -> runnable.run());
        this.cbExternalAccess.addValueChangedListener(ignore -> runnable.run());
        this.cbIngress.addValueChangedListener(ignore -> toggleIngress());
        final Action<Void> refreshAction = new Action<Void>(Action.Id.of("user/containerapps.refresh_properties_view.app"))
            .withAuthRequired(true)
            .withSource(this.containerApp)
            .withIdParam(this.containerApp.getName())
            .withHandler(ignore -> this.refresh());
        this.btnRefresh.setAction(refreshAction);
        final Action<Void> saveAction = new Action<Void>(Action.Id.of("user/containerapps.update_container_app.app"))
            .withAuthRequired(true)
            .withSource(this.containerApp)
            .withIdParam(this.containerApp.getName())
            .withHandler(ignore -> this.save());
        this.saveButton.setAction(saveAction);
    }

    private void toggleIngress() {
        final boolean enableIngress = Optional.ofNullable(cbIngress).map(AzureComboBox::getValue).orElse(false);
        lblExternalAccess.setVisible(enableIngress);
        cbExternalAccess.setVisible(enableIngress);
        lblInsecureConnection.setVisible(enableIngress);
        txtInsecureConnections.setVisible(enableIngress);
        lblTargetPort.setVisible(enableIngress);
        txtTargetPort.setVisible(enableIngress);
        lblTransportMethod.setVisible(enableIngress);
        txtTransportMethod.setVisible(enableIngress);
        AzureTaskManager.getInstance().runOnPooledThread(ContainerAppPropertiesEditor.this::refreshToolbar);
    }

    private void setEnabled(boolean enabled) {
        this.resetButton.setVisible(enabled);
        this.saveButton.setEnabled(enabled);
        this.txtTargetPort.setEnabled(enabled);
        this.cbIngress.setEnabled(enabled);
        this.cbIngress.setEditable(enabled);
        this.cbExternalAccess.setEnabled(enabled);
        this.cbExternalAccess.setEditable(enabled);
        this.intMaxReplicas.setEnabled(enabled);
        this.intMinReplicas.setEnabled(enabled);
    }

    private void refreshToolbar() {
        // get status from app instead of draft since status of draft is not correct
        final AzResource.FormalStatus formalStatus = this.containerApp.getFormalStatus();
        final AzureTaskManager manager = AzureTaskManager.getInstance();
        manager.runLater(() -> {
            final boolean normal = formalStatus.isRunning() || formalStatus.isStopped();
            this.setEnabled(normal);
            if (normal) {
                manager.runOnPooledThread(() -> {
                    final boolean modified = this.isModified(); // checking modified is slow
                    manager.runLater(() -> {
                        this.resetButton.setVisible(modified);
                        this.saveButton.setEnabled(modified);
                    });
                });
            } else {
                this.resetButton.setVisible(false);
                this.saveButton.setEnabled(false);
            }
        });
    }

    @Override
    public boolean isModified() {
        final IngressConfig ingressConfig = this.containerApp.getIngressConfig();
        final IngressConfig draftIngressConfig = this.getIngressConfig();
        final ScaleConfig scaleConfig = this.containerApp.getScaleConfig();
        final ScaleConfig draftScaleConfig = this.getScaleConfig();
        return !Objects.equals(ingressConfig, draftIngressConfig) || !Objects.equals(scaleConfig, draftScaleConfig);
    }

    @Nullable
    private IngressConfig getIngressConfig() {
        // todo: replace with copy constructor
        final boolean enableIngress = Optional.ofNullable(cbIngress).map(AzureComboBox::getValue).orElse(false);
        if (enableIngress) {
            final IngressConfig previous = Optional.ofNullable(this.containerApp.getIngressConfig())
                .orElseGet(() -> IngressConfig.builder().build());
            final IngressConfig result = IngressConfig.fromIngress(previous.toIngress());
            result.setEnableIngress(true);
            result.setExternal(Optional.ofNullable(cbExternalAccess.getValue()).orElse(false));
            result.setTargetPort(txtTargetPort.getNumber());
            return result;
        } else {
            return IngressConfig.fromIngress(null);
        }
    }

    @Nullable
    private ScaleConfig getScaleConfig() {
        return ContainerAppDraft.ScaleConfig.builder()
            .maxReplicas(this.intMaxReplicas.getNumber())
            .minReplicas(this.intMinReplicas.getNumber())
            .build();
    }

    private void save() {
        // todo: add confirm when disable ingress like portal
        this.setEnabled(false);
        final IngressConfig ingressConfig = getIngressConfig();
        final ScaleConfig scaleConfig = getScaleConfig();
        final ContainerAppDraft.Config config = Optional.ofNullable(this.draft.getConfig()).orElseGet(ContainerAppDraft.Config::new);
        config.setIngressConfig(ingressConfig);
        config.setScaleConfig(scaleConfig);
        draft.setConfig(config);
        AzureTaskManager.getInstance().runInBackground("save updates", this.draft::commit);
    }

    private void reset() {
        this.draft.reset();
        this.rerender();
    }

    private void refresh() {
        this.draft.reset();
        AzureTaskManager.getInstance().runInBackground("Refreshing...", () -> {
            this.containerApp.refresh();
            this.rerender();
        });
    }

    @Override
    protected void rerender() {
        AzureTaskManager.getInstance().runLater(() -> {
            this.refreshToolbar();
            this.setData(this.containerApp);
        });
    }

    private void setData(@Nonnull final ContainerApp containerApp) {
        resourceGroupTextField.setText(containerApp.getResourceGroupName());
        locationTextField.setText(Optional.ofNullable(containerApp.getRegion()).map(Region::getLabel).orElse(N_A)); // region
        final Subscription subscription = Azure.az(AzureAccount.class).account().getSubscription(containerApp.getSubscriptionId());
        subscriptionTextField.setText(subscription.getName());
        subscriptionIDTextField.setText(subscription.getId());
        txtProvisioningStatus.setText(containerApp.getProvisioningState()); // todo: replace with provision status
        txtRevisionMode.setText(Optional.ofNullable(containerApp.getRevisionMode()).map(RevisionMode::getValue).orElse(N_A));
        txtLatestRevisionName.setText(Optional.ofNullable(containerApp.getLatestRevisionName()).orElse(N_A));
        txtContainerAppsEnvironment.setText(Optional.ofNullable(containerApp.getManagedEnvironment()).map(AzResource::getName).orElse(N_A));
        final String ingressFqdn = containerApp.getIngressFqdn();
        if (StringUtils.isEmpty(ingressFqdn)) {
            linkApplicationUrl.setEnabled(false);
            linkApplicationUrl.setText("Ingress disabled");
            linkApplicationUrl.setHyperlinkTarget(N_A);
            linkApplicationUrl.setIcon(null);
        } else {
            linkApplicationUrl.setEnabled(true);
            linkApplicationUrl.setHyperlinkText("https://" + ingressFqdn);
            linkApplicationUrl.setHyperlinkTarget("https://" + ingressFqdn);
        }
        // ingress
        Optional.ofNullable(containerApp.getIngressConfig()).ifPresent(c -> {
            this.cbIngress.setValue(c.isEnableIngress());
            this.cbExternalAccess.setValue(c.isExternal());
            this.txtInsecureConnections.setText(Optional.of(c.isAllowInsecure()).map(String::valueOf).orElse("false"));
            this.txtTransportMethod.setText(Optional.ofNullable(c.getTransport()).map(TransportMethod::getDisplayName).orElse(null));
            this.txtTargetPort.setNumber(c.getTargetPort());
        });
        Optional.ofNullable(containerApp.getScaleConfig()).ifPresent(c -> {
            // https://learn.microsoft.com/en-us/azure/container-apps/scale-app?pivots=azure-cli
            this.intMaxReplicas.setNumber(Optional.ofNullable(c.getMaxReplicas()).orElse(10));
            this.intMinReplicas.setNumber(Optional.ofNullable(c.getMinReplicas()).orElse(0));
        });
        final ResourceConfiguration resourceConfiguration = containerApp.getResourceConfiguration();
        final boolean isWorkloadProfile = Optional.ofNullable(resourceConfiguration)
                .map(ResourceConfiguration::getWorkloadProfile).isPresent();
        lblWorkloadProfile.setVisible(isWorkloadProfile);
        txtWorkloadProfile.setVisible(isWorkloadProfile);
        txtWorkloadProfile.setText(Optional.ofNullable(resourceConfiguration)
                .map(ResourceConfiguration::getWorkloadProfile)
                .map(WorkloadProfile::getName)
                .orElse(N_A));
        AzureTaskManager.getInstance().runInBackground("Loading revisions.", () -> this.containerApp.revisions().list())
            .thenAccept(pools -> AzureTaskManager.getInstance().runLater(() -> fillRevisions(pools)));
    }

    private void fillRevisions(List<Revision> pools) {
        final DefaultTableModel model = (DefaultTableModel) this.revisionsTable.getModel();
        model.setRowCount(0);
        final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        pools.forEach(i -> {
            final String date = Optional.ofNullable(i.getCreatedTime())
                .map(odt -> odt.atZoneSameInstant(zoneId))
                .map(formatter::format).orElse(N_A);
            model.addRow(new Object[]{i.getName(), date, i.getProvisioningState(), i.getTrafficWeight(), i.isActive()});
        });
        final int rows = model.getRowCount() < 5 ? 5 : pools.size();
        model.setRowCount(rows);
        this.revisionsTable.setVisibleRowCount(rows);
    }

    @Override
    public @Nonnull JComponent getComponent() {
        return pnlRoot;
    }

    private void createUIComponents() {
        this.txtTargetPort = new JBIntSpinner(80, 1, 65535);
        this.intMaxReplicas = new JBIntSpinner(10, 1, 300);
        this.intMinReplicas = new JBIntSpinner(0, 0, 300);
    }
}
