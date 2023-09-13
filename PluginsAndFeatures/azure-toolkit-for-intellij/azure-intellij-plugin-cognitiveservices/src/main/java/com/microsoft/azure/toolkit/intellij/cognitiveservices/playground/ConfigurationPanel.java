package com.microsoft.azure.toolkit.intellij.cognitiveservices.playground;

import com.intellij.icons.AllIcons;
import com.intellij.ui.components.ActionLink;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.components.AzureSlider;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.model.Configuration;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigurationPanel implements AzureForm<Configuration> {
    public static final String LEARN_MORE_URL = "https://go.microsoft.com/fwlink/?linkid=2189780";
    private JLabel lblMaxResponse;
    private JLabel lblTemperature;
    private JLabel lblStopSequence;
    private AzureTextInput txtStopSequence;
    private JPanel pnlRoot;
    private AzureSlider sliderTopN;
    private AzureSlider sliderTemperature;
    private AzureSlider sliderMaxResponse;
    private AzureSlider sliderFrequency;
    private AzureSlider sliderPresence;
    private JLabel lblTopP;
    private JLabel lblFrequency;
    private JLabel lblPresence;
    private ActionLink lblLearnMore;

    public ConfigurationPanel() {
        $$$setupUI$$$();
        init();
    }

    private void init() {
        this.lblMaxResponse.setIcon(AllIcons.General.ContextHelp);
        this.lblMaxResponse.setBorder(JBUI.Borders.empty(20, 0, 6, 0));
        this.lblFrequency.setIcon(AllIcons.General.ContextHelp);
        this.lblFrequency.setBorder(JBUI.Borders.empty(6, 0));
        this.lblPresence.setIcon(AllIcons.General.ContextHelp);
        this.lblPresence.setBorder(JBUI.Borders.empty(6, 0));
        this.lblTemperature.setIcon(AllIcons.General.ContextHelp);
        this.lblTemperature.setBorder(JBUI.Borders.empty(6, 0));
        this.lblTopP.setIcon(AllIcons.General.ContextHelp);
        this.lblTopP.setBorder(JBUI.Borders.empty(6, 0));
        this.lblStopSequence.setIcon(AllIcons.General.ContextHelp);
        this.lblStopSequence.setBorder(JBUI.Borders.empty(6, 0));

        this.lblLearnMore.setExternalLinkIcon();
        this.lblLearnMore.addActionListener(ignore ->
            AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(LEARN_MORE_URL));
        this.getInputs().forEach(input -> input.addValueChangedListener(ignore -> this.fireValueChangedEvent()));
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.sliderMaxResponse = new AzureSlider(1000, 0, 4000, 40, 1);
        this.sliderTemperature = new AzureSlider(0.7, 0, 1, 0.01, 100);
        this.sliderTopN = new AzureSlider(0.95, 0, 1, 0.01, 100);
        this.sliderFrequency = new AzureSlider(0, 0, 2, 0.01, 100);
        this.sliderPresence = new AzureSlider(0, 0, 2, 0.01, 100);
        this.txtStopSequence = new AzureTextInput() {
            @Override
            protected synchronized void setValidationExtension(@Nullable Extension extension) {
                // do nothing here
            }
        };
    }

    @Override
    public Configuration getValue() {
        return Configuration.builder()
            .maxResponse((int) Math.round(sliderMaxResponse.getValue()))
            .temperature(sliderTemperature.getValue())
            .topP(sliderTopN.getValue())
            .stopSequences(Arrays.stream(txtStopSequence.getValue().split(";")).filter(StringUtils::isNotBlank).collect(Collectors.toList()))
            .frequencyPenalty(sliderFrequency.getValue())
            .presencePenalty(sliderPresence.getValue())
            .build();
    }

    @Override
    public void setValue(@Nonnull Configuration data) {
        this.sliderMaxResponse.setValue(Double.valueOf(data.getMaxResponse()));
        this.sliderTemperature.setValue(data.getTemperature());
        this.sliderTopN.setValue(data.getTopP());
        this.txtStopSequence.setValue(String.join(";", data.getStopSequences()));
        this.sliderFrequency.setValue(data.getFrequencyPenalty());
        this.sliderPresence.setValue(data.getPresencePenalty());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(this.sliderMaxResponse, this.sliderTemperature, this.sliderTopN, this.txtStopSequence, this.sliderFrequency, this.sliderPresence);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
