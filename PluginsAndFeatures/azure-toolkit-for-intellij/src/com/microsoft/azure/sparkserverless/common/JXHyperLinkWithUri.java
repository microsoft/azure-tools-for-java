package com.microsoft.azure.sparkserverless.common;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.jdesktop.swingx.JXHyperlink;
import org.jdesktop.swingx.hyperlink.HyperlinkAction;

import java.net.URI;

public class JXHyperLinkWithUri extends JXHyperlink {
    private URI uri;

    @Override
    public void setURI(@NotNull URI uri) {
        this.uri = uri;
        // setURI() in JXHyperlink will set uri to text field
        // so we override this method to keep text field not change
        String initialText = this.getText();
        this.setAction(HyperlinkAction.createHyperlinkAction(uri));
        this.setText(initialText);
    }

    @NotNull
    public URI getURI() {
        return this.uri;
    }
}
