package com.microsoft.azure.toolkit.intellij.facet;

import com.intellij.facet.ui.*;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.intellij.facet.AzureFacetConfiguration.AzureFacetState;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.nio.file.Path;

public class AzureFacetEditorTab extends FacetEditorTab {
    private final AzureFacetState state;
    private final AzureFacetEditorPanel panel;

    /**
     * Only org.intellij.sdk.facet.AzureFacetState is captured so it can be updated per user changes in the EditorTab.
     *
     * @param state     {@link AzureFacetState} object persisting {@link AzureFacet} state.
     * @param context   Facet editor context, can be used to get e.g. the current project module.
     * @param validator Facet validator manager, can be used to get and apply a custom validator for this facet.
     */
    public AzureFacetEditorTab(@Nonnull AzureFacetState state, @Nonnull FacetEditorContext context,
                               @Nonnull FacetValidatorsManager validator) {
        super();
        this.state = state;
        this.panel = new AzureFacetEditorPanel(context.getModule());
        validator.registerValidator(new FacetEditorValidator() {
            @Override
            public ValidationResult check() {
                return AzureFacetEditorTab.this.validate();
            }
        }, this.panel.getInputs());
    }

    /**
     * Provides the {@link JPanel} displayed in the Project Structure | Facet UI
     *
     * @return {@link JPanel} to be displayed in the {@link AzureFacetEditorTab}.
     */
    @Nonnull
    @Override
    public JComponent createComponent() {
        return this.panel.getContentPanel();
    }

    /**
     * @return the name of this facet for display in this editor tab.
     */
    @Override
    @Nls(capitalization = Nls.Capitalization.Title)
    public String getDisplayName() {
        return AzureFacetType.INSTANCE.getPresentableName();
    }

    /**
     * Determines if the facet state entered in the UI differs from the currently stored state.
     * Called when user changes text in {@link #panel}.
     *
     * @return {@code true} if the state returned from the panel's UI differs from the stored facet state.
     */
    @Override
    public boolean isModified() {
        return !StringUtil.equals(state.getDotAzurePath(), this.panel.getValue().trim());
    }

    /**
     * Stores new facet state (text) entered by the user.
     * Called when {@link #isModified()} returns true.
     *
     * @throws ConfigurationException if anything generates an exception.
     */
    @Override
    public void apply() throws ConfigurationException {
        // Not much to go wrong here, but fulfill the contract
        try {
            final String newTextContent = this.panel.getValue();
            state.setDotAzurePath(newTextContent);
        } catch (final Exception e) {
            throw new ConfigurationException(e.toString());
        }
    }

    /**
     * Copies current {@link AzureFacetState} into the {@link #panel} UI element.
     * This method is called each time this editor tab is needed for display.
     */
    @Override
    public void reset() {
        this.panel.setValue(state.getDotAzurePath());
    }

    public ValidationResult validate() {
        final String strDotAzureDir = panel.getValue();
        if (StringUtils.isBlank(strDotAzureDir)) {
            return new ValidationResult("Path to Azure resource connection configuration files cannot be empty");
        }
        final VirtualFile vfDotAzureDir = VfsUtil.findFile(Path.of(strDotAzureDir), true);
        if (vfDotAzureDir == null || !vfDotAzureDir.isDirectory() || vfDotAzureDir.findChild("profiles.xml") == null) {
            return new ValidationResult("invalid path to Azure resource connection configuration files");
        }
        return ValidationResult.OK;
    }
}