package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBRadioButton;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Objects;
import java.util.function.BiConsumer;

public class AzureSdkArtifactDetailPanel {
    @Getter
    private JPanel contentPanel;
    private JBRadioButton artifactId;
    private ComboBox<String> version;
    private HyperlinkLabel mavenRepoLink;
    private JPanel links;
    private AzureSdkArtifactEntity artifact;
    @Setter
    private BiConsumer<? super AzureSdkArtifactEntity, String> onArtifactOrVersionSelected;

    public AzureSdkArtifactDetailPanel(AzureSdkArtifactEntity artifact) {
        this.$$$setupUI$$$();
        this.initEventListeners();
        this.version.setBorder(BorderFactory.createEmptyBorder());
        this.setData(artifact);
    }

    private void initEventListeners() {
        this.artifactId.addActionListener((e) -> {
            if (this.artifactId.isSelected()) {
                this.setSelected(true);
            }
        });
        this.version.addItemListener((e) -> {
            if (this.artifactId.isSelected() && e.getStateChange() == ItemEvent.SELECTED) {
                this.onArtifactOrVersionSelected.accept(this.artifact, (String) this.version.getSelectedItem());
            }
        });
    }

    public void setData(@Nonnull final AzureSdkArtifactEntity artifact) {
        this.artifact = artifact;
        this.artifactId.setText(artifact.getArtifactId());
        if (StringUtils.isNotBlank(artifact.getVersionGA())) {
            this.version.addItem(artifact.getVersionGA());
        }
        if (StringUtils.isNotBlank(artifact.getVersionPreview())) {
            this.version.addItem(artifact.getVersionPreview());
        }
        artifact.getLink("repopath").ifPresent(l -> {
            this.mavenRepoLink.setHyperlinkText("Maven");
            this.mavenRepoLink.setHyperlinkTarget(l.getHref());
        });
        for (final AzureSdkArtifactEntity.Link l : artifact.getLinks()) {
            final HyperlinkLabel link = new HyperlinkLabel();
            if (StringUtils.isNotBlank(l.getHref()) && Objects.equals(l.getRel(), "repopath")) {
                link.setHyperlinkText(l.getRel());
                link.setHyperlinkTarget(l.getHref());
                this.links.add(link);
            }
        }
    }

    public void setSelected(boolean selected) {
        this.artifactId.setSelected(selected);
        this.onArtifactOrVersionSelected.accept(this.artifact, (String) this.version.getSelectedItem());
    }

    public void attachToGroup(ButtonGroup group) {
        group.add(this.artifactId);
    }

    public void detachFromGroup(ButtonGroup group) {
        group.remove(this.artifactId);
    }


    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    public void $$$setupUI$$$() {
    }
}
