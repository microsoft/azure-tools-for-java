package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiTypeElement;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class extends the LocalInspectionTool to check for the use of ServiceBusReceiverAsyncClient
 * in the code and suggests using ServiceBusProcessorClient instead.
 * The client data is loaded from the configuration file and the client name is checked against the
 * discouraged client name. If the client name matches, a problem is registered with the suggestion message.
 */
public class ServiceBusReceiverAsyncClientCheck extends LocalInspectionTool {

    private static List<String> CLIENT_DATA = null;

    // Define constants for string literals
    private static final String RULE_NAME = "ServiceBusReceiverAsyncClientCheck";
    private static final String SUGGESTION_KEY = "antipattern_message";
    private static final String CLIENT_NAME_KEY = "client_name";
    private static final Logger LOGGER = Logger.getLogger(ServiceBusReceiverAsyncClientCheck.class.getName());

    static {
        try {
            CLIENT_DATA = getClientToCheck();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error loading client data", e);
        }
    }


    /**
     * This method builds a visitor to check for the discouraged client name in the code.
     * If the client name matches the discouraged client, a problem is registered with the suggestion message.
     * @param holder
     * @param isOnTheFly
     * @return PsiElementVisitor
     */
    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitTypeElement(PsiTypeElement element) {
                super.visitTypeElement(element);

                // Check if the element is an instance of PsiTypeElement
                if (element instanceof PsiTypeElement && element.getType() != null) {

                    if (CLIENT_DATA.isEmpty()) {
                        return;
                    }
                    String clientName = CLIENT_DATA.get(0);
                    String suggestion = CLIENT_DATA.get(1);

                    // Register a problem if the client used matches the discouraged client
                    if (element.getType().getPresentableText().equals(clientName)) {
                        holder.registerProblem(element, suggestion);
                    }
                }
            }

        };
    }

    /**
     * Loading the client data from the configuration file.
     *
     * @return List of client name and suggestion message
     */
    static List<String> getClientToCheck() throws IOException {

        final JSONObject jsonObject = LoadJsonConfigFile.getInstance().getJsonObject();
        final String clientName = jsonObject.getJSONObject(RULE_NAME).getString(CLIENT_NAME_KEY);
        final String suggestion= jsonObject.getJSONObject(RULE_NAME).getString(SUGGESTION_KEY);
        return Arrays.asList(clientName, suggestion);

    }
}
