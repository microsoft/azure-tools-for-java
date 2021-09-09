package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidator;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.net.URI;
import java.util.List;

/**
 * Editable combo box for Azure callback URLs, which allows adding and removing URLs of a list.
 * It keeps the URLs in the editable combobox model and is not using an item loader.
 */
class AzureEditableCallbackUrlsCombobox extends AzureComboBox<String> {
    AzureEditableCallbackUrlsCombobox() {
        super(false);
    }

    @Override
    protected void init() {
        super.init();

        // adding two extensions, AzureComboBox's createExtension is only supporting one
        var editor = (ComboBoxEditor) this.editor;
        var textField = (ExtendableTextField) editor.getEditorComponent();

        var removeTooltip = MessageBundle.message("dialog.identity.ad.register_app.removeCallbackURL.tooltip");
        textField.addExtension(ExtendableTextComponent.Extension.create(AllIcons.General.Remove, removeTooltip, this::removeSelectedItem));

        var addTooltip = MessageBundle.message("dialog.identity.ad.register_app.addCallbackURL.tooltip");
        textField.addExtension(ExtendableTextComponent.Extension.create(AllIcons.General.Add, addTooltip, this::showAddItemPopup));
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @NotNull
    @Override
    public AzureValidationInfo doValidate() {
        var value = this.getValue();
        if (value == null || value.isEmpty() || isValid(value)) {
            return super.doValidate();
        }

        return AzureValidationInfo.builder()
                .input(this)
                .message(MessageBundle.message("action.azure.aad.registerApp.callbackURLInvalid"))
                .build();
    }

    private void removeSelectedItem() {
        var selected = this.getSelectedItem();
        if (selected != null) {
            this.removeItem(selected);
        }
    }

    private void showAddItemPopup() {
        var title = MessageBundle.message("dialog.identity.ad.register_app.addCallbackURL.dialogTitle");
        var message = MessageBundle.message("dialog.identity.ad.register_app.addCallbackURL.dialogMessage");

        var url = Messages.showInputDialog((Project) null, message, title, null, null, new InputValidator() {
            @Override
            public boolean checkInput(@NlsSafe String s) {
                return isValid(s);
            }

            @Override
            public boolean canClose(@NlsSafe String s) {
                return isValid(s);
            }
        });

        if (url != null) {
            addItem(url);
            setSelectedItem(url);
        }
    }

    public void setUrls(@NotNull List<String> callbackUrls) {
        setItems(callbackUrls);
    }

    private static boolean isValid(@NotNull String value) {
        try {
            var uri = URI.create(value);
            var scheme = uri.getScheme();
            return ("https".equals(scheme) || "http".equals(scheme)) && !uri.getHost().isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
