package com.microsoft.azure.toolkit.intellij.integration.services.connectionstring;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBPasswordField;
import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.AbstractConnectionStringAzResource;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class AzureResourceConnectDialog extends AzureDialog<AbstractConnectionStringAzResource<?>> implements AzureFormPanel<AbstractConnectionStringAzResource<?>> {
    @Getter
    private JPanel contentPanel;
    private ConnectionStringTypeComboBox selectorResourceType;
    private JBPasswordField txtConnectionString;

    public AzureResourceConnectDialog(Project project) {
        super(project);
        this.project = project;
        $$$setupUI$$$();
        this.init();
    }

    @Override
    protected void init() {
        super.init();
        this.contentPanel.setPreferredSize(new Dimension(550, 130));
    }

    @Override
    public AzureForm<AbstractConnectionStringAzResource<?>> getForm() {
        return this;
    }

    @Nonnull
    @Override
    protected String getDialogTitle() {
        return "Connect Azure Resource with connection string";
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
    public AbstractConnectionStringAzResource<?> getValue() {
        final String type = this.selectorResourceType.getItem();
        final char[] connectionString = this.txtConnectionString.getPassword();
        final AzService service = Azure.getServices(type).get(0);
        return service.getOrInitByConnectionString(String.valueOf(connectionString));
    }

    @Override
    public void setValue(final AbstractConnectionStringAzResource<?> data) {

    }

    @Override
    protected List<ValidationInfo> doValidateAll() {
        final String type = this.selectorResourceType.getItem();
        final char[] password = this.txtConnectionString.getPassword();
        final List<ValidationInfo> result = new ArrayList<>();
        if (StringUtils.isBlank(type)) {
            result.add(new ValidationInfo("\"Resource Type\" is required.", this.selectorResourceType));
        }
        if (ArrayUtils.isEmpty(password)) {
            result.add(new ValidationInfo("\"Connection String\" is required.", this.txtConnectionString));
        }
        return result;
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
}
