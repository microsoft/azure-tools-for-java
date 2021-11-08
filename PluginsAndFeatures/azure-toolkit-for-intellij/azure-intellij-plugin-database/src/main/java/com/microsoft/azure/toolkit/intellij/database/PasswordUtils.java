/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.database;

import com.microsoft.azure.toolkit.intellij.common.component.AzurePasswordFieldInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.Arrays;

public class PasswordUtils {
    private static final int PASSWORD_LENGTH_MIN = 8;
    private static final int PASSWORD_LENGTH_MAX = 128;
    private static final int PASSWORD_CATEGORIES_MIN = 3;
    private static final int LONGEST_COMMON_SUBSEQUENCE_BETWEEN_NAME_AND_PASSWORD = 3;

    public static AzurePasswordFieldInput generatePasswordFieldInput(JPasswordField passwordField, JTextField adminUsernameTextField) {
        return new AzurePasswordFieldInput(passwordField) {
            @Override
            public AzureValidationInfo doValidate(final String value) {
                final String adminUsername = adminUsernameTextField.getText();
                return PasswordUtils.validatePassword(value, adminUsername, this);
            }
        };
    }

    public static AzurePasswordFieldInput generateConfirmPasswordFieldInput(JPasswordField confirmPasswordField, JPasswordField passwordField) {
        return new AzurePasswordFieldInput(confirmPasswordField) {
            @Override
            public AzureValidationInfo doValidate(final String value) {
                final char[] password = passwordField.getPassword();
                final String passwordAsString = password != null ? String.valueOf(password) : StringUtils.EMPTY;
                return PasswordUtils.validateConfirmPassword(value, passwordAsString, this);
            }
        };
    }

    /**
     * Your password must be at least 8 characters and at most 128 characters.
     * Your password must contain characters from three of the following categories – English uppercase letters, English lowercase letters, numbers (0-9),
     * and non-alphanumeric characters (!, $, #, %, etc.).
     * Your password cannot contain all or part of the login name. Part of a login name is defined as three or more consecutive alphanumeric characters.
     */
    private static AzureValidationInfo validatePassword(String password, String username, AzureFormInput<?> input) {
        // validate length
        if (StringUtils.length(password) < PASSWORD_LENGTH_MIN || StringUtils.length(password) > PASSWORD_LENGTH_MAX) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(input).message("Your password must be at least 8 characters and at most 128 characters.")
                    .type(AzureValidationInfo.Type.ERROR).build();
        }
        // validate character categories.
        if (countCharacterCategories(password) < PASSWORD_CATEGORIES_MIN) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(input).message("Your password must contain characters from three of the following categories – " +
                    "English uppercase letters, English lowercase letters, numbers (0-9), and non-alphanumeric characters (!, $, #, %, etc.).")
                    .type(AzureValidationInfo.Type.ERROR).build();
        }
        // validate longest common subsequence between username and password.
        int longestCommonSubstringLength = longestCommonSubstringLength(username, password);
        int usernameLength = StringUtils.length(username);
        if ((usernameLength > 0 && longestCommonSubstringLength == usernameLength)
                || longestCommonSubstringLength >= LONGEST_COMMON_SUBSEQUENCE_BETWEEN_NAME_AND_PASSWORD) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(input).message("Your password cannot contain all or part of the login name." +
                    " Part of a login name is defined as three or more consecutive alphanumeric characters.")
                    .type(AzureValidationInfo.Type.ERROR).build();
        }
        return AzureValidationInfo.success(input);
    }

    private static int longestCommonSubstringLength(String left, String right) {
        if (StringUtils.isAnyBlank(left, right)) {
            return 0;
        }
        int max = 0;
        final int[][] dp = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i < left.length(); i++) {
            for (int j = 0; j < right.length(); j++) {
                if (left.charAt(i) == right.charAt(j)) {
                    dp[i + 1][j + 1] = dp[i][j] + 1;
                } else {
                    dp[i + 1][j + 1] = 0;
                }
                max = Math.max(max, dp[i + 1][j + 1]);
            }
        }
        return max;
    }

    private static int countCharacterCategories(final String value) {
        final Boolean[] categories = new Boolean[] {false, false, false, false};
        for (char ch : value.toCharArray()) {
            categories[0] = categories[0] || CharUtils.isAsciiNumeric(ch);
            categories[1] = categories[1] || CharUtils.isAsciiAlphaLower(ch);
            categories[2] = categories[2] || CharUtils.isAsciiAlphaUpper(ch);
            categories[3] = categories[3] || !CharUtils.isAsciiAlphanumeric(ch);
        }
        return (int) Arrays.stream(categories).filter(e -> e).count();
    }

    /**
     * Password and confirm password must match.
     */
    private static AzureValidationInfo validateConfirmPassword(String confirmPassword, String password, AzureFormInput<?> input) {
        if (!StringUtils.equals(confirmPassword, password)) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(input).message("Password and confirm password must match.").type(AzureValidationInfo.Type.ERROR).build();
        }
        return AzureValidationInfo.success(input);
    }
}
