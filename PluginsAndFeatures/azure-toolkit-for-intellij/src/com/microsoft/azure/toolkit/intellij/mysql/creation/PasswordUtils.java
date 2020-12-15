/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.intellij.mysql.creation;

import com.microsoft.azure.toolkit.intellij.common.AzurePasswordFieldInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LongestCommonSubsequence;

import javax.swing.*;

public class PasswordUtils {
    private static final int PASSWORD_LENGTH_MIN = 8;
    private static final int PASSWORD_LENGTH_MAX = 128;
    private static final int PASSWORD_CATEGORIES_MIN = 3;
    private static final int LONGEST_COMMON_SUBSEQUENCE_BETWEEN_NAME_AND_PASSWORD = 3;

    protected static AzurePasswordFieldInput generatePasswordFieldInput(JPasswordField passwordField, JTextField adminUsernameTextField) {
        return new AzurePasswordFieldInput(passwordField) {
            @Override
            public AzureValidationInfo doValidate() {
                final AzureValidationInfo info = super.doValidate();
                if (!AzureValidationInfo.OK.equals(info)) {
                    return info;
                }
                final String adminUsername = adminUsernameTextField.getText();
                final String value = this.getValue();
                return PasswordUtils.validatePassword(value, adminUsername, this);
            }
        };
    }

    protected static AzurePasswordFieldInput generateConfirmPasswordFieldInput(JPasswordField confirmPasswordField, JPasswordField passwordField) {
        return new AzurePasswordFieldInput(confirmPasswordField) {
            @Override
            public AzureValidationInfo doValidate() {
                final AzureValidationInfo info = super.doValidate();
                if (!AzureValidationInfo.OK.equals(info)) {
                    return info;
                }
                final String value = this.getValue();
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
        LongestCommonSubsequence algorithm = new LongestCommonSubsequence();
        int longestCommonSubsequenceLength = algorithm.apply(username, password);
        if (longestCommonSubsequenceLength >= LONGEST_COMMON_SUBSEQUENCE_BETWEEN_NAME_AND_PASSWORD) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(input).message("Your password cannot contain all or part of the login name." +
                    " Part of a login name is defined as three or more consecutive alphanumeric characters.")
                    .type(AzureValidationInfo.Type.ERROR).build();
        }
        return AzureValidationInfo.OK;
    }

    private static int countCharacterCategories(final String value) {
        int count = 0;
        boolean containsNumber = false;
        boolean containsLowerAlpha = false;
        boolean containsUpperAlpha = false;
        boolean containsSpecialCharacter = false;
        for (char ch : value.toCharArray()) {
            if (!containsNumber && CharUtils.isAsciiNumeric(ch)) {
                count++;
                containsNumber = true;
            }
            if (!containsLowerAlpha & CharUtils.isAsciiAlphaLower(ch)) {
                count++;
                containsLowerAlpha = true;
            }
            if (!containsUpperAlpha & CharUtils.isAsciiAlphaUpper(ch)) {
                count++;
                containsUpperAlpha = true;
            }
            if (!containsSpecialCharacter && !CharUtils.isAsciiAlphanumeric(ch)) {
                count++;
                containsSpecialCharacter = true;
            }
            if (count >= PASSWORD_CATEGORIES_MIN) {
                break;
            }
        }
        return count;
    }

    /**
     * Password and confirm password must match.
     */
    private static AzureValidationInfo validateConfirmPassword(String confirmPassword, String password, AzureFormInput<?> input) {
        if (!StringUtils.equals(confirmPassword, password)) {
            final AzureValidationInfo.AzureValidationInfoBuilder builder = AzureValidationInfo.builder();
            return builder.input(input).message("Password and confirm password must match.").type(AzureValidationInfo.Type.ERROR).build();
        }
        return AzureValidationInfo.OK;
    }
}
