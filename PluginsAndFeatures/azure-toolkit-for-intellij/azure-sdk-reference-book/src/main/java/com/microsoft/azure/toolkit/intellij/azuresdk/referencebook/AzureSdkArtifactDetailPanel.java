package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.google.common.collect.ImmutableMap;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.DropDownLink;
import com.intellij.ui.components.JBRadioButton;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.BiConsumer;

public class AzureSdkArtifactDetailPanel {
    @Getter
    private JPanel contentPanel;
    private JBRadioButton artifactId;
    private DropDownLink<String> version;
    private JPanel links;
    @Setter
    private BiConsumer<? super AzureSdkArtifactEntity, String> onArtifactOrVersionSelected;
    private final AzureSdkArtifactEntity artifact;
    private final Map<String, String> linkNames = ImmutableMap.of(
        "github", "GitHub Repository",
        "repopath", "Maven Repository",
        "msdocs", "Microsoft Docs",
        "javadoc", "Javadoc");

    public AzureSdkArtifactDetailPanel(AzureSdkArtifactEntity artifact) {
        this.artifact = artifact;
        this.$$$setupUI$$$();
        this.artifactId.setText(artifact.getArtifactId());
        this.initEventListeners();
        this.version.setBorder(BorderFactory.createEmptyBorder());
    }

    private void initEventListeners() {
        this.artifactId.addActionListener((e) -> {
            if (this.artifactId.isSelected()) {
                this.setSelected(true);
            }
        });
    }

    private void setLinks(@Nonnull final Map<String, String> links) {
        this.links.removeAll();
        linkNames.forEach((type, name) -> {
            final String url = links.get(type);
            if (StringUtils.isNotBlank(url)) {
                final HyperlinkLabel link = new HyperlinkLabel();
                this.links.add(new JToolBar.Separator());
                link.setHyperlinkText(name);
                link.setHyperlinkTarget(url);
                this.links.add(new JSeparator(SwingConstants.VERTICAL));
                this.links.add(link);
            }
        });
    }

    private DropDownLink<String> buildVersionSelector() {
        final ArrayList<String> versions = new ArrayList<>();
        if (StringUtils.isNotBlank(artifact.getVersionGA())) {
            versions.add(artifact.getVersionGA());
        }
        if (StringUtils.isNotBlank(artifact.getVersionPreview())) {
            versions.add(artifact.getVersionPreview());
        }
        final DropDownLink<String> version = new DropDownLink<>(versions.get(0), versions, (String v) -> {
            if (this.artifactId.isSelected()) {
                this.setLinks(this.artifact.getLinks(v));
                this.onArtifactOrVersionSelected.accept(this.artifact, this.version.getSelectedItem());
            }
        }, true);
        version.setForeground(JBColor.BLACK);
        return version;
    }

    public void setSelected(boolean selected) {
        this.artifactId.setSelected(selected);
        this.onArtifactOrVersionSelected.accept(this.artifact, this.version.getSelectedItem());
    }

    public void attachToGroup(ButtonGroup group) {
        group.add(this.artifactId);
    }

    public void detachFromGroup(ButtonGroup group) {
        group.remove(this.artifactId);
    }

    private void createUIComponents() {
        this.links = new JPanel();
        this.version = this.buildVersionSelector();
        this.setLinks(artifact.getLinks(this.version.getSelectedItem()));
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    public void $$$setupUI$$$() {
    }
}
