/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.intellij.ui;

import com.intellij.openapi.project.Project;
import com.microsoft.intellij.ui.components.AzureDialogWrapper;
import org.jdesktop.swingx.JXHyperlink;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;
import java.awt.*;
import java.net.URI;
import java.util.List;

public class ArtifactValidationWindow extends AzureDialogWrapper {
    private JPanel contentPane;
    private JEditorPane editorPane;

    private List<String> issues;

    public static void go(@Nullable Project project, List<String> issues) {
        ArtifactValidationWindow w = new ArtifactValidationWindow(project, issues);
        w.show();
    }

    public ArtifactValidationWindow(@Nullable Project project, List<String> issues) {
        super(project, true, IdeModalityType.PROJECT);
        setModal(true);
        setTitle("Artifact Validation");
        setCancelButtonText("Close");
        this.issues = issues;

        StringBuilder sb = new StringBuilder();
        sb.append("<H3>The following issues were found:</H3>");
        sb.append("<ul>");
        for (String issue : issues) {
            sb.append("<li>" + issue + "</li><br/>");
        }
        sb.append("</ul>");

        sb.append("Please fix the issues first.<br/>");
        sb.append("<a href=\"https://www.jetbrains.com/help/idea/2017.1/working-with-artifacts.html\">Working with Artifacts Help</a>");


        editorPane.setText(sb.toString());
        editorPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);

        editorPane.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if(e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    // Do something with e.getURL() here
                    //e.getURL().toString()
                    JXHyperlink link = new JXHyperlink();
                    link.setURI(URI.create(e.getURL().toString()));
                    link.doClick();
                }
            }
        });

        Font font = UIManager.getFont("Label.font");
        String bodyRule = "body { font-family: " + font.getFamily() + "; " +
                "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument)editorPane.getDocument()).getStyleSheet().addRule(bodyRule);

        init();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    @Override
    protected Action[] createActions() {
        return new Action[]{this.getCancelAction()};
    }
}
