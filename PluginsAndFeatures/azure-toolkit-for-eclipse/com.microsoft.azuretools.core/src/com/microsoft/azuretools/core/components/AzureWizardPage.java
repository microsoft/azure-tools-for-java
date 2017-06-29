package com.microsoft.azuretools.core.components;

import com.microsoft.azuretools.telemetry.AppInsightsClient;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;

public abstract class AzureWizardPage extends WizardPage{
    private static final String WIZARD_PAGE = "WizardPage";
    private static final String TITLE = "Title";
    private static final String WIZARD_DIALOG = "WizardDialog";
    
    protected AzureWizardPage(String pageName) {
        super(pageName);
    }
    
    protected AzureWizardPage(String pageName,
            String title,
            ImageDescriptor titleImage) {
        super(pageName, title, titleImage);
    }

    public void sendButtonClickedTelemetry(String action) {
        final Map<String, String> properties = new HashMap<>();
        String simpleName = this.getClass().getSimpleName();
        if (simpleName == null) {
            simpleName = "";
        }
        properties.put(WIZARD_PAGE, simpleName);
        String title = this.getTitle();
        if (title == null) {
            title = "";
        }
        properties.put(TITLE, title);
        String wizardDialog = this.getShell().getText();
        if (wizardDialog == null) {
            wizardDialog = "";
        }
        properties.put(WIZARD_DIALOG, wizardDialog);
        AppInsightsClient.createByType(AppInsightsClient.EventType.WizardStep,
                simpleName, action, properties);
    }
}
