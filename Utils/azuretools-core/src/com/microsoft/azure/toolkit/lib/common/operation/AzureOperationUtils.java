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

import com.microsoft.azure.management.resources.fluentcore.arm.ResourceUtils;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;
import org.jtwig.environment.EnvironmentConfiguration;
import org.jtwig.environment.EnvironmentConfigurationBuilder;
import org.jtwig.functions.FunctionRequest;
import org.jtwig.functions.SimpleJtwigFunction;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AzureOperationUtils {
    private static final Uri2NameFunction toName = new Uri2NameFunction();
    public static final EnvironmentConfiguration jgConfig = EnvironmentConfigurationBuilder.configuration().functions().add(toName).and().build();

    public static AzureOperation getAnnotation(@NotNull AzureOperationRef ref) {
        final Method method = ref.getMethod();
        return method.getAnnotation(AzureOperation.class);
    }

    public static String getOperationTitle(@NotNull AzureOperationRef ref) {
        final AzureOperation annotation = AzureOperationUtils.getAnnotation(ref);
        final String messageTemplate = annotation.name();
        final String[] parameters = annotation.params();
        final String[] params = Arrays.stream(parameters).map(expression -> interpretExpression(expression, ref)).toArray(String[]::new);
        return MessageFormat.format(String.format(messageTemplate, (Object[]) params), params);
    }

    private static String interpretExpression(String expression, AzureOperationRef ref) {
        final String[] paramNames = ref.getParamNames();
        final Object[] paramValues = ref.getParamValues();
        final Map<String, Object> variables = new HashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            variables.put("$" + paramNames[i], paramValues[i]);
        }
        variables.put("$$", ref.getInstance());
        final String fixedExpression = expression.replace("@.", "$$.").replace("@", "$$.");
        return interpretInline(fixedExpression, variables);
    }

    private static String interpretInline(String expr, Map<String, Object> variables) {
        final JtwigTemplate template = JtwigTemplate.inlineTemplate(String.format("{{%s}}", expr), jgConfig);
        final JtwigModel model = JtwigModel.newModel();
        variables.forEach(model::with);
        return template.render(model);
    }

    private static class Uri2NameFunction extends SimpleJtwigFunction {

        @Override
        public String name() {
            return "uri_to_name";
        }

        @Override
        public Object execute(FunctionRequest request) {
            final String input = getInput(request);
            return ResourceUtils.nameFromResourceId(input);
        }

        private String getInput(FunctionRequest request) {
            request.minimumNumberOfArguments(1).maximumNumberOfArguments(1);
            return request.getEnvironment().getValueEnvironment().getStringConverter().convert(request.get(0));
        }
    }
}
