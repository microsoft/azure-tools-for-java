package com.microsoft.azure.toolkit.intellij.dbtools;

import com.intellij.database.dataSource.DataSourceConfigurable;
import com.intellij.database.dataSource.url.DataInterchange;
import com.intellij.database.dataSource.url.FieldSize;
import com.intellij.database.dataSource.url.template.UrlEditorModel;
import com.intellij.database.dataSource.url.ui.ParamEditorBase;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.ui.components.JBCheckBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AzComponent;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public abstract class AzureParamsEditorBase<TCombo extends DbUtilsComboBoxBase<TItem>, TItem extends AzResource> extends ParamEditorBase<TCombo> {
    public static final String NONE = "<NONE>";
    private final String dataSourceKey;
    @Getter
    @Setter
    private String text = "";
    private boolean updating;

    public AzureParamsEditorBase(
            @NotNull TCombo editorComponent,
            @NotNull DataInterchange interchange,
            @NotNull FieldSize fieldSize,
            @NlsContexts.Label @Nullable String caption,
            @NotNull String dataSourceKey,
            AnAction @NotNull ... actions) {
        super(editorComponent, interchange, fieldSize, caption, actions);
        this.dataSourceKey = dataSourceKey;

        final DbUtilsComboBoxBase<TItem> comboBox = this.getEditorComponent();
        comboBox.addValueChangedListener(this::setResource);
        interchange.addPersistentProperty(dataSourceKey);

        interchange.addPropertyChangeListener((evt -> onPropertiesChanged(evt.getPropertyName(), evt.getNewValue())), this);
    }

    @Override
    protected @NotNull JComponent createComponent(TCombo component) {
        final JPanel container = new JPanel();
        final BoxLayout layout = new BoxLayout(container, BoxLayout.Y_AXIS);
        container.setLayout(layout);

        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(component);

        if (!Azure.az(AzureAccount.class).isLoggedIn()) {
            final NotSignedInTipLabel notSignInTips = createNotSignedInTipLabel();
            notSignInTips.addSignedInListener(() -> {
                notSignInTips.setVisible(false);
                component.reloadItems();
            }, this);

            container.add(notSignInTips);
        }

        final NoResourceTipLabel noResourceTipLabel = createNoResourceTipLabel();
        component.setResourcesLoadedListener(noResourceTipLabel::setVisible);

        container.add(noResourceTipLabel);
        return container;
    }

    protected abstract NotSignedInTipLabel createNotSignedInTipLabel();

    protected abstract NoResourceTipLabel createNoResourceTipLabel();

    protected void setUsername(String user) {
        final UrlEditorModel model = this.getDataSourceConfigurable().getUrlEditor().getEditorModel();
        model.setParameter("user", user);
        model.commit(true);
    }

    @SneakyThrows
    protected void setUseSsl(boolean useSsl) {
        final DataSourceConfigurable configurable = this.getDataSourceConfigurable();
        final JBCheckBox useSSLCheckBox = (JBCheckBox) FieldUtils.readField(configurable.getSshSslPanel(), "myUseSSLJBCheckBox", true);
        useSSLCheckBox.setSelected(useSsl);
    }

    protected void setJdbcUrl(@NotNull String url) {
        final UrlEditorModel model = this.getDataSourceConfigurable().getUrlEditor().getEditorModel();
        model.setUrl(url);
        model.commit(true);
    }

    protected void onPropertiesChanged(String propertyName, Object newValue) {
        if (updating || StringUtils.isEmpty((String) newValue) || !StringUtils.equals(propertyName, "host")) return;

        final DbUtilsComboBoxBase<TItem> comboBox = this.getEditorComponent();
        final TItem resource = comboBox.getValue();
        if (Objects.nonNull(resource) && !Objects.equals(getHostFromValue(resource), newValue)) {
            comboBox.setNull();
            this.setResource(null);
        }
    }

    @SneakyThrows
    protected DataSourceConfigurable getDataSourceConfigurable() {
        return (DataSourceConfigurable) FieldUtils.readField(this.getInterchange(), "myConfigurable", true);
    }

    private void setResource(@Nullable TItem resource) {
        final DataInterchange interchange = this.getInterchange();
        final String oldResourceId = interchange.getProperty(dataSourceKey);
        final String newResourceId = Optional.ofNullable(resource).map(AzComponent::getId).orElse(null);
        this.updating = true;

        final AzureTaskManager manager = AzureTaskManager.getInstance();

        final Consumer<Object> runSetResource = (fromBackground) -> {
            setResource(interchange, fromBackground, resource, oldResourceId, newResourceId);
            this.updating = false;
        };

        if (needsBackgroundUpdateForSetResource()) {
            manager.runOnPooledThread(() -> {
                final Object fromBackground = doBackgroundUpdateForSetResource(resource);
                manager.runLater(() -> runSetResource.accept(fromBackground), AzureTask.Modality.ANY);
            });
        } else {
            manager.runLater(() -> runSetResource.accept(null), AzureTask.Modality.ANY);
        }
    }

    protected abstract void setResource(@NotNull DataInterchange interchange,
                                        @Nullable Object fromBackground,
                                        @Nullable TItem value,
                                        @Nullable String oldResourceId,
                                        @Nullable String newResourceId);

    @Nullable
    protected Object doBackgroundUpdateForSetResource(@Nullable TItem value) {
        return null;
    }

    protected boolean needsBackgroundUpdateForSetResource() {
        return false;
    }

    @Nullable
    protected abstract String getHostFromValue(@NotNull TItem value);
}

