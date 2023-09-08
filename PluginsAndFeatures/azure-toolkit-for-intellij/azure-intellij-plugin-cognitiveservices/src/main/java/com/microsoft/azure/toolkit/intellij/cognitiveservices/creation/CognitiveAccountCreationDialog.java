package com.microsoft.azure.toolkit.intellij.cognitiveservices.creation;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBFont;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.ide.cognitiveservices.CognitiveServicesActionsContributor;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.components.CognitiveAccountRegionComboBox;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.components.CognitiveAccountSkuComboBox;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.components.CognitiveSubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.cognitiveservices.AzureCognitiveServices;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccountDraft;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CognitiveAccountCreationDialog extends AzureDialog<CognitiveAccountDraft> implements AzureForm<CognitiveAccountDraft> {
    public static final String REGISTER_SUBSCRIPTION_TEXT = "Azure OpenAI Service is currently available to customers via an application form. The selected subscription has not been enabled for use of the service and does not have quota for any pricing tiers. " +
            "<a href = \"https://aka.ms/oai/access\">Click here to request access to Azure OpenAI service</a>.\u2197";
    private JLabel lblSubscription;
    private CognitiveSubscriptionComboBox cbSubscription;
    private JLabel lblResourceGroup;
    private ResourceGroupComboBox cbResourceGroup;
    private JLabel lblName;
    private AzureTextInput txtName;
    private JPanel pnlRoot;
    private JLabel lblRegion;
    private JLabel lblSku;
    private CognitiveAccountRegionComboBox cbRegion;
    private CognitiveAccountSkuComboBox cbSku;
    private JBLabel lblRegister;

    public CognitiveAccountCreationDialog(Project project) {
        super(project);
        $$$setupUI$$$();
        this.init();
    }

    @Override
    protected void init() {
        super.init();
        this.lblSubscription.setLabelFor(cbSubscription);
        this.cbSubscription.setRequired(true);
        this.cbSubscription.setUsePreferredSizeAsMinimum(false);
        this.lblResourceGroup.setLabelFor(cbResourceGroup);
        this.cbResourceGroup.setRequired(true);
        this.cbResourceGroup.setUsePreferredSizeAsMinimum(false);
        this.lblName.setLabelFor(txtName);
        this.txtName.setRequired(true);
        this.lblRegion.setLabelFor(cbRegion);
        this.cbRegion.setRequired(true);
        this.lblSku.setLabelFor(cbSku);
        this.cbSku.setRequired(true);

        this.cbSubscription.addItemListener(this::onSelectSubscription);
        this.cbRegion.addItemListener(this::onSelectRegion);


        this.lblRegister.setText(REGISTER_SUBSCRIPTION_TEXT);
        this.lblRegister.setFont(JBFont.regular());
        this.lblRegister.setVisible(false);
        this.lblRegister.setIconWithAlignment(AllIcons.General.Warning, SwingConstants.LEFT, SwingConstants.TOP);
        this.lblRegister.setAllowAutoWrapping(true);
        this.lblRegister.setCopyable(true);// this makes label auto wrapping
        this.lblRegister.setForeground(UIUtil.getContextHelpForeground());
    }

    private void onSelectRegion(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof Region) {
            this.cbSku.setRegion((Region) e.getItem());
        }
    }

    private void onSelectSubscription(@Nonnull ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED && e.getItem() instanceof Subscription) {
            final Subscription subscription = (Subscription) e.getItem();
            this.cbResourceGroup.setSubscription(subscription);
            this.cbRegion.setSubscription(subscription);
            this.cbSku.setSubscription(subscription);
            this.lblRegister.setVisible(!Azure.az(AzureCognitiveServices.class).isOpenAIEnabled(subscription.getId()));
            this.lblRegister.setIconWithAlignment(AllIcons.General.Warning, SwingConstants.LEFT, SwingConstants.TOP);
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbSubscription = new CognitiveSubscriptionComboBox(false);
        this.cbRegion = new CognitiveAccountRegionComboBox();
        this.cbSku = new CognitiveAccountSkuComboBox();
        this.lblRegister = new JBLabel() {
            @Override
            protected @NotNull HyperlinkListener createHyperlinkListener() {
                return event -> Optional.of(event).filter(e -> e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
                        .ifPresent(ignore -> AzureActionManager.getInstance().getAction(CognitiveServicesActionsContributor.ENABLE_OPENAI).handle(cbSubscription.getValue()));
            }
        };

    }

    @Override
    public AzureForm<CognitiveAccountDraft> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Azure OpenAI Service";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public CognitiveAccountDraft getValue() {
        final Subscription subscription = Objects.requireNonNull(cbSubscription.getValue());
        final String name = Objects.requireNonNull(txtName.getValue());
        final ResourceGroup resourceGroup = Objects.requireNonNull(cbResourceGroup.getValue());
        final CognitiveAccountDraft draft = Azure.az(AzureCognitiveServices.class)
                .accounts(subscription.getId()).create(name, resourceGroup.getName());
        final CognitiveAccountDraft.Config config = new CognitiveAccountDraft.Config();
        config.setResourceGroup(resourceGroup);
        config.setName(name);
        config.setRegion(cbRegion.getValue());
        config.setSku(cbSku.getValue());
        draft.setConfig(config);
        return draft;
    }

    @Override
    public void setValue(@Nonnull final CognitiveAccountDraft data) {
        cbSubscription.setValue(s -> StringUtils.equalsIgnoreCase(s.getId(), data.getSubscriptionId()));
        txtName.setValue(data.getName());
        Optional.ofNullable(data.getResourceGroup()).ifPresent(cbResourceGroup::setValue);
        Optional.ofNullable(data.getRegion()).ifPresent(cbRegion::setValue);
        Optional.ofNullable(data.getSku()).ifPresent(cbSku::setValue);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(txtName, cbSubscription, cbResourceGroup, cbRegion, cbSku);
    }

    private void $$$setupUI$$$() {
    }
}
