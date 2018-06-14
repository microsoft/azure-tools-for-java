package com.microsoft.azure.sparkserverless.common;

import com.intellij.ide.BrowserUtil;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.hyperlink.HyperlinkAction;

import java.net.URI;

public class JXHyperLinkWithUri extends JXHyperlink {
    @Nullable
    private URI uri;

    public JXHyperLinkWithUri() {
        super();
        this.addActionListener(event -> {
            if (this.uri != null) {
                BrowserUtil.browse(this.uri);
            }
        });
    }

    @Override
    public void setURI(@Nullable URI uri) {
        this.uri = uri;
        // setURI() in JXHyperlink will set uri to text field
        // so we override this method to keep text field not change
        String initialText = this.getText();
        this.setAction(HyperlinkAction.createHyperlinkAction(uri));
        this.setText(initialText);
    }
}
