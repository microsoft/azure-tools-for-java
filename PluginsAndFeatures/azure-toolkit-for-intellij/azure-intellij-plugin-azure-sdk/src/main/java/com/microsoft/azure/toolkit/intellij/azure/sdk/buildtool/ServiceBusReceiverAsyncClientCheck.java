package com.microsoft.azure.toolkit.intellij.azure.sdk.buildtool;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiTypeElement;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;



/**
 * This class extends the LocalInspectionTool to check for the use of ServiceBusReceiverAsyncClient
 * in the code and suggests using ServiceBusProcessorClient instead.
 */
public class ServiceBusReceiverAsyncClientCheck extends LocalInspectionTool {

    public static final String configFileName = "META-INF/ruleConfigs.json";

    // loading the client data from the configuration file
    public static List<String> getClientToCheck() {
        try {

            InputStream inputStream = ServiceBusReceiverAsyncClientCheck.class.getClassLoader().getResourceAsStream(configFileName);
            if (inputStream == null) {
                throw new FileNotFoundException("Configuration file not found");
            }
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                JSONObject jsonObject = new JSONObject(bufferedReader.lines().collect(Collectors.joining()));
                String clientName = jsonObject.getJSONObject("ServiceBusReceiverAsyncClientCheck").getString("clientName");
                String suggestedClient = jsonObject.getJSONObject("ServiceBusReceiverAsyncClientCheck").getString("suggestion");
                return Arrays.asList(clientName, suggestedClient);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    @NotNull
    @Override
    public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitTypeElement(PsiTypeElement element) {
                super.visitTypeElement(element);

                // Check if the element is an instance of PsiTypeElement and if the type matches the discouraged client
                if (element instanceof PsiTypeElement && element.getType() != null) {

                    List<String> clientData = getClientToCheck();
                    if (clientData.isEmpty()) {
                        return;
                    }
                    String clientName = clientData.get(0);
                    String suggestedClient = clientData.get(1);

                    if (element.getType().getPresentableText().equals(clientName)) {
                        holder.registerProblem(element, "Use of "+clientName+" detected. Use "+suggestedClient+" instead.");
                    }

                }
            }

        };
    }
}
