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
            "github", "Source Code",
            "repopath", "Maven Repository",
            "msdocs", "Microsoft Docs",
            "ghdocs", "GitHub Docs"
    );

    public AzureSdkArtifactDetailPanel(AzureSdkArtifactEntity artifact) {
        this.artifact = artifact;
        this.$$$setupUI$$$();
        this.init();
        this.initEventListeners();
        this.version.setBorder(BorderFactory.createEmptyBorder());
    }

    private void init() {
        this.artifactId.setText(artifact.getArtifactId());
        this.buildLinks();
    }

    private void initEventListeners() {
        this.artifactId.addActionListener((e) -> {
            if (this.artifactId.isSelected()) {
                this.setSelected(true);
            }
        });
    }

    private void buildLinks() {
        for (final AzureSdkArtifactEntity.Link l : artifact.getLinks()) {
            final HyperlinkLabel link = new HyperlinkLabel();
            if (StringUtils.isNotBlank(l.getHref())) {
                this.links.add(new JToolBar.Separator());
                link.setHyperlinkText(linkNames.get(l.getRel()));
                link.setHyperlinkTarget(l.getHref());
                this.links.add(new JSeparator(SwingConstants.VERTICAL));
                this.links.add(link);
            }
        }
    }

    private void buildVersionSelector() {
        final ArrayList<String> versions = new ArrayList<>();
        if (StringUtils.isNotBlank(artifact.getVersionGA())) {
            versions.add(artifact.getVersionGA());
        }
        if (StringUtils.isNotBlank(artifact.getVersionPreview())) {
            versions.add(artifact.getVersionPreview());
        }
        this.version = new DropDownLink<>(versions.get(0), versions, v -> {
            if (this.artifactId.isSelected()) {
                this.onArtifactOrVersionSelected.accept(this.artifact, this.version.getSelectedItem());
            }
        });
        this.version.setForeground(JBColor.BLACK);
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
        this.buildVersionSelector();
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    public void $$$setupUI$$$() {
    }
}
