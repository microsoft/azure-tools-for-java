package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBRadioButton;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkPackageEntity;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Map;
import java.util.function.BiConsumer;

public class AzureSdkPackageDetailPanel {
    @Getter
    private JPanel contentPanel;
    private JBRadioButton artifactId;
    private ComboBox<String> version;
    private HyperlinkLabel mavenRepoLink;
    private JPanel links;
    private AzureSdkPackageEntity pkg;
    @Setter
    private BiConsumer<? super AzureSdkPackageEntity, String> onPackageOrVersionSelected;

    public AzureSdkPackageDetailPanel(AzureSdkPackageEntity pkg) {
        this.$$$setupUI$$$();
        this.initEventListeners();
        this.version.setBorder(BorderFactory.createEmptyBorder());
        this.setData(pkg);
    }

    private void initEventListeners() {
        this.artifactId.addActionListener((e) -> {
            if (this.artifactId.isSelected()) {
                this.setSelected(true);
            }
        });
        this.version.addItemListener((e) -> {
            if (this.artifactId.isSelected() && e.getStateChange() == ItemEvent.SELECTED) {
                this.onPackageOrVersionSelected.accept(this.pkg, (String) this.version.getSelectedItem());
            }
        });
    }

    public void setData(@Nonnull final AzureSdkPackageEntity pkg) {
        this.pkg = pkg;
        this.artifactId.setText(pkg.getArtifact());
        if (StringUtils.isNotBlank(pkg.getVersionGA())) {
            this.version.addItem(pkg.getVersionGA());
        }
        if (StringUtils.isNotBlank(pkg.getVersionPreview())) {
            this.version.addItem(pkg.getVersionPreview());
        }
        this.mavenRepoLink.setHyperlinkText("Maven");
        this.mavenRepoLink.setHyperlinkTarget(pkg.getMavenPath());
        for (final Map.Entry<String, String> e : pkg.getLinks().entrySet()) {
            final HyperlinkLabel link = new HyperlinkLabel();
            link.setHyperlinkText(e.getKey());
            link.setHyperlinkTarget(e.getValue());
            this.links.add(link);
        }
    }

    public void setSelected(boolean selected) {
        this.artifactId.setSelected(selected);
        this.onPackageOrVersionSelected.accept(this.pkg, (String) this.version.getSelectedItem());
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
