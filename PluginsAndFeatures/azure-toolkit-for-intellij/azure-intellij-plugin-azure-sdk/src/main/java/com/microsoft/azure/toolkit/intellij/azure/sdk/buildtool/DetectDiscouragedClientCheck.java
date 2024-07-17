package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiTypeElement;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class extends the LocalInspectionTool to check for the use of discouraged clients
 * in the code and suggests using other clients instead.
 * The client data is loaded from the configuration file and the client name is checked against the
 * discouraged client name. If the client name matches, a problem is registered with the suggestion message.
 */

public class DetectDiscouragedClientCheck extends LocalInspectionTool {

    @NotNull
    @Override
    public JavaElementVisitor buildVisitor(@NotNull ProblemsHolder holder,boolean isOnTheFly){
        return new DetectDiscouragedClientVisitor(holder,isOnTheFly);
    }

    static class DetectDiscouragedClientVisitor extends JavaElementVisitor {

        // Define the fields for the visitor
        private final boolean isOnTheFly;
        private final ProblemsHolder holder;

        // Constructor for the visitor
        public DetectDiscouragedClientVisitor(ProblemsHolder holder, boolean isOnTheFly) {
            this.holder = holder;
            this.isOnTheFly = isOnTheFly;
        }

        private static Map<String, String> CLIENT_DATA;

        // Define constants for string literals
        private static final String RULE_NAME = "DetectDiscouragedClientCheck";
        private static final String SUGGESTION_KEY = "antipattern_message";
        private static final String CLIENT_NAME_KEY = "client_name";
        private static final Logger LOGGER = Logger.getLogger(DetectDiscouragedClientCheck.class.getName());

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
         */
        @Override
        public void visitTypeElement(PsiTypeElement element) {
            super.visitTypeElement(element);

            // Check if the element is an instance of PsiTypeElement
            if (element instanceof PsiTypeElement && element.getType() != null) {

                String elementType = element.getType().getPresentableText();

                // Use the map to get the suggestion message
                String suggestion = CLIENT_DATA.get(elementType);

                // Register a problem if the client used matches a discouraged client
                if (suggestion != null) {
                    holder.registerProblem(element, suggestion);
                }
            }
        };


        /**
         * Loading the client data from the configuration file.
         * The client data is stored in a JSON object with the client name as the key and the suggestion message as the value.
         *
         * @return
         */

        static Map<String, String> getClientToCheck() throws IOException {
            JSONObject jsonObject = LoadJsonConfigFile.getInstance().getJsonObject();
            JSONObject clientData = jsonObject.getJSONObject(RULE_NAME);

            Map<String, String> clientToCheckMap = new HashMap<>();

            // iterate over the keys in the JSON object to get clients to check and their corresponding messages.
            for (String key : clientData.keySet()) {
                String clientName = clientData.getJSONObject(key).getString(CLIENT_NAME_KEY);
                String suggestion = clientData.getJSONObject(key).getString(SUGGESTION_KEY);
                clientToCheckMap.put(clientName, suggestion);
            }
            return clientToCheckMap;
        }
    }
}
