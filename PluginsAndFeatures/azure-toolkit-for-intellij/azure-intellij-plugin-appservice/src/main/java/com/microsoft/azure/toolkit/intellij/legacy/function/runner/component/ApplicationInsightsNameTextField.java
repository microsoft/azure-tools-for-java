package com.microsoft.azure.toolkit.intellij.legacy.function.runner.component;

import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.azure.toolkit.intellij.common.AzureBundle.message;

public class ApplicationInsightsNameTextField extends AzureTextInput {
    private static final String APP_INSIGHTS_NAME_INVALID_CHARACTERS = "[*;/?:@&=+$,<>#%\\\"\\{}|^'`\\\\\\[\\]]";

    public ApplicationInsightsNameTextField() {
        super();
        this.addValidator(this::doValidateValue);
        this.setRequired(true);
    }

    public AzureValidationInfo doValidateValue() {
        final String applicationInsightsName = this.getValue();
        if (StringUtils.isEmpty(applicationInsightsName)) {
            return AzureValidationInfo.error(message("function.applicationInsights.validate.empty"), this);
        }
        if (applicationInsightsName.length() > 255) {
            return AzureValidationInfo.error(message("function.applicationInsights.validate.length"), this);
        }
        if (applicationInsightsName.endsWith(".")) {
            return AzureValidationInfo.error(message("function.applicationInsights.validate.point"), this);
        }
        if (applicationInsightsName.endsWith(" ") || applicationInsightsName.startsWith(" ")) {
            return AzureValidationInfo.error(message("function.applicationInsights.validate.space"), this);
        }
        final Pattern pattern = Pattern.compile(APP_INSIGHTS_NAME_INVALID_CHARACTERS);
        final Matcher matcher = pattern.matcher(applicationInsightsName);
        final Set<String> invalidCharacters = new HashSet<>();
        while (matcher.find()) {
            invalidCharacters.add(matcher.group());
        }
        if (!invalidCharacters.isEmpty()) {
            return AzureValidationInfo.error(message("function.applicationInsights.validate.invalidChar", String.join(",", invalidCharacters)), this);
        }
        return AzureValidationInfo.success(this);
    }
}
