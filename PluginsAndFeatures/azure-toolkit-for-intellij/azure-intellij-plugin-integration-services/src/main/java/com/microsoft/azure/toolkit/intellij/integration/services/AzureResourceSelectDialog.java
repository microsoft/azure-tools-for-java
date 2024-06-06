package com.microsoft.azure.toolkit.intellij.integration.services;

import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBFont;
import com.microsoft.azure.toolkit.ide.common.IExplorerNodeProvider;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.explorer.AzureExplorer;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.intellij.ui.AnimatedIcon.ANIMATION_IN_RENDERER_ALLOWED;

public class AzureResourceSelectDialog extends AzureDialog<List<AbstractAzResource<?, ?, ?>>> implements AzureFormPanel<List<AbstractAzResource<?, ?, ?>>> {
    @Getter
    private JPanel contentPanel;
    private JBList<Node<?>> listServices;
    private JBList<Node<?>> listResources;
    private JBScrollPane pnlResources;
    private JPanel pnlLoading;
    private JLabel lblLoading;

    public AzureResourceSelectDialog(Project project) {
        super(project);
        this.project = project;
        $$$setupUI$$$();
        this.init();
    }

    public void init() {
        super.init();
        this.contentPanel.setPreferredSize(new Dimension(755, 574));

        this.lblLoading.setIcon(IconUtil.scale(IntelliJAzureIcons.getIcon(AzureIcons.Common.REFRESH_ICON), lblLoading, 1.5f));
        this.lblLoading.setFont(JBFont.h2());

        this.listServices.setCellRenderer(new ListNodeRenderer());
        this.listServices.putClientProperty(ANIMATION_IN_RENDERER_ALLOWED, true);

        this.listResources.setCellRenderer(new ListNodeRenderer());
        this.listResources.putClientProperty(ANIMATION_IN_RENDERER_ALLOWED, true);
        this.listResources.setEmptyText("No resources available");

        final DefaultListModel<Node<?>> modelServices = new DefaultListModel<>();
        final DefaultListModel<Node<?>> modelResources = new DefaultListModel<>();
        this.listServices.setModel(modelServices);
        this.listResources.setModel(modelResources);

        modelServices.addAll(buildAzServiceNodes());
        this.listServices.addListSelectionListener(e -> {
            final Node<?> selectedValue = this.listServices.getSelectedValue();
            if (selectedValue != null) {
                final AzureTaskManager manager = AzureTaskManager.getInstance();
                this.pnlResources.setVisible(false);
                this.pnlLoading.setVisible(true);
                manager.runOnPooledThread(() -> {
                    final List<Node<?>> children = selectedValue.buildChildren();
                    manager.runLater(() -> {
                        modelResources.clear();
                        modelResources.addAll(children);
                        this.pnlResources.setVisible(true);
                        this.pnlLoading.setVisible(false);
                    }, AzureTask.Modality.ANY);
                });
            }
        });
        this.listResources.addListSelectionListener(e -> {
            this.setOKActionEnabled(!this.listResources.getSelectedValuesList().isEmpty());
        });
        this.setOKActionEnabled(false);
    }

    @Nonnull
    public static List<Node<?>> buildAzServiceNodes() {
        return AzureExplorer.manager.getAzServices().stream()
            .map(r -> AzureExplorer.manager.createNode(r, null, IExplorerNodeProvider.ViewType.TYPE_CENTRIC))
            .sorted(Comparator.comparing(Node::getLabel))
            .collect(Collectors.toList());
    }

    @Override
    public AzureForm<List<AbstractAzResource<?, ?, ?>>> getForm() {
        return this;
    }

    @Nonnull
    @Override
    protected String getDialogTitle() {
        return "Select Azure Resource";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return this.contentPanel;
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public List<AbstractAzResource<?, ?, ?>> getValue() {
        return this.listResources.getSelectedValuesList().stream().map(n -> ((AbstractAzResource<?, ?, ?>) n.getValue())).collect(Collectors.toList());
    }

    @Override
    public void setValue(final List<AbstractAzResource<?, ?, ?>> data) {

    }

    private static class ListNodeRenderer extends ColoredListCellRenderer<Node<?>> {
        @Override
        protected void customizeCellRenderer(@Nonnull final JList<? extends Node<?>> jList, final Node<?> node, final int i, final boolean b, final boolean b1) {
            final Node.View view = node.getView();
            final SimpleTextAttributes attributes = view.isEnabled() ? SimpleTextAttributes.REGULAR_ATTRIBUTES : SimpleTextAttributes.GRAY_ATTRIBUTES;
            this.setIcon(Optional.ofNullable(view.getIcon()).map(IntelliJAzureIcons::getIcon).orElseGet(() -> IntelliJAzureIcons.getIcon(AzureIcons.Resources.GENERIC_RESOURCE)));
            this.append(view.getLabel(), attributes);
            this.append(Optional.ofNullable(view.getDescription()).filter(StringUtils::isNotBlank).map(d -> " " + d).orElse(""), SimpleTextAttributes.GRAY_ITALIC_ATTRIBUTES, true);
            this.setToolTipText(Optional.ofNullable(view.getTips()).filter(StringUtils::isNotBlank).orElseGet(view::getLabel));
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
