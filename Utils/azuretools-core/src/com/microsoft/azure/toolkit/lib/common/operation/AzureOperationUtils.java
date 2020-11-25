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

package com.microsoft.azure.toolkit.lib.common.operation;

import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

public class AzureOperationUtils {

    public static AzureOperation getAnnotation(@NotNull AzureOperationRef ref) {
        final Method method = ref.getMethod();
        return method.getAnnotation(AzureOperation.class);
    }

    public static String getOperationTitle(@NotNull AzureOperationRef ref) {
        final AzureOperation annotation = AzureOperationUtils.getAnnotation(ref);
        final String messageTemplate = annotation.value();
        final String[] parameters = annotation.params();
        final String[] params = Arrays.stream(parameters).map(expression -> {
            final String str = interpretExpression(expression, ref);
            //TODO: @wangmi improve this part to get name from uri
            if (Objects.nonNull(str) && str.contains("/")) {
                final String[] parts = str.split("/");
                return str.endsWith("/") ? parts[parts.length - 2] : parts[parts.length - 1];
            }
            return str;
        }).toArray(String[]::new);
        return String.format(messageTemplate, (Object[]) params);
    }

    private static String interpretExpression(String expression, AzureOperationRef ref) {
        final String[] paramNames = ref.getParamNames();
        final Object[] paramValues = ref.getParamValues();
        final Object targetInstance = ref.getInstance();
        final String fixedExpression = StringUtils.substring(expression, 1);
        final String parameterName = StringUtils.contains(expression, ".")
                                     ? StringUtils.substring(fixedExpression, 0, StringUtils.indexOf(fixedExpression, "."))
                                     : fixedExpression;
        Object object = null;
        if (StringUtils.startsWith(expression, "$")) {
            // process parameter
            final int parameterIndex = ArrayUtils.indexOf(paramNames, parameterName);
            if (parameterIndex >= 0) {
                object = paramValues[parameterIndex];
            } else {
                object = null;
            }
        } else if (StringUtils.startsWith(expression, "@")) {
            // member variables
            final Field variableField = FieldUtils.getField(targetInstance.getClass(), parameterName, true);
            try {
                if (variableField != null) {
                    object = variableField.get(targetInstance);
                }
            } catch (IllegalArgumentException | IllegalAccessException e) {
                // swallow exception while get variables
            }
        } else {
            return expression;
        }
        return object != null ? interpretInline(fixedExpression, Collections.singletonMap(parameterName, object)) : null;
    }

    private static String interpretInline(String expr, Map<String, Object> variableMap) {
        final JtwigTemplate template = JtwigTemplate.inlineTemplate(String.format("{{%s}}", expr));
        final JtwigModel model = JtwigModel.newModel();
        variableMap.forEach(model::with);
        return template.render(model);
    }
}
