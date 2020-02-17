package com.microsoft.intellij.runner.functions.core;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayInitializerMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiEnumConstant;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiLiteral;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.impl.JavaConstantExpressionEvaluator;
import com.microsoft.azure.common.exceptions.AzureExecutionException;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class BindingV2 {

    private String type;

    private BindingEnumV2.Direction direction;

    private Map<String, Object> bindingAttributes = new HashMap<>();

    private static Map<BindingEnumV2, List<String>> requiredAttributeMap = new HashMap<>();

    static {
        //initialize required attributes, which will be saved to function.json even if it equals to its default value
        requiredAttributeMap.put(BindingEnumV2.EventHubTrigger, Arrays.asList("cardinality"));
        requiredAttributeMap.put(BindingEnumV2.HttpTrigger, Arrays.asList("authLevel"));
    }

    public Object getAttribute(String attributeName) {
        return bindingAttributes.get(attributeName);
    }

    public BindingV2(BindingEnumV2 bindingEnum) {
        this.bindingEnum = bindingEnum;
        this.type = bindingEnum.getType();
        this.direction = bindingEnum.getDirection();
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("[ name: ")
                .append(getName())
                .append(", type: ")
                .append(getType())
                .append(", direction: ")
                .append(getDirection())
                .append(" ]")
                .toString();
    }

    public String getName() {
        return (String) bindingAttributes.get("name");
    }

    public void setName(String name) {
        this.bindingAttributes.put("name", name);
    }


    public String getDirection() {
        if (this.direction != null) {
            return direction.toString();
        }
        return null;
    }

    public Map<String, Object> getBindingAttributes() {
        return bindingAttributes;
    }

    public static String getEnumFieldString(final String className, final String fieldName)
            throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException {
        final Class<?> c = Class.forName(className);
        final Field[] a = c.getFields();
        final Optional<Field> targetField = Arrays.stream(a).filter(t -> t.getName().equals(fieldName)).findFirst();
        if (targetField.isPresent()) {
            return Objects.toString(targetField.get().get(null));
        }
        return null;
    }

    public BindingV2(BindingEnumV2 bindingEnum, PsiAnnotation annotation) throws AzureExecutionException {
        this(bindingEnum);
        final PsiNameValuePair[] attributes = annotation.getParameterList().getAttributes();
        for (PsiNameValuePair attribute : attributes) {
            final PsiAnnotationMemberValue value = attribute.getValue();
            String name = attribute.getAttributeName();
            if (value instanceof PsiArrayInitializerMemberValue) {
                PsiAnnotationMemberValue[] initializers = ((PsiArrayInitializerMemberValue) value)
                        .getInitializers();
                List<String> result = Lists.newArrayListWithCapacity(initializers.length);

                for (PsiAnnotationMemberValue initializer : initializers) {
                    result.add(getAnnotationValueAsString(initializer));
                }
                addProperties(result.toArray(new String[0]), name);
            } else {
                String valueText = getAnnotationValueAsString(value);
                addProperties(valueText, name);
            }
        }
    }


    private void addProperties(Object value, String propertyName) {
        if (propertyName.equals("direction") && value instanceof String) {
            this.direction = BindingEnumV2.Direction.fromString((String) value);
            return;
        }

        if (propertyName.equals("type") && value instanceof String) {
            this.type = (String) value;
            return;
        }
        bindingAttributes.put(propertyName, value);
//        if (//!value.equals(propertyMethod.getDefaultValue()) ||
//                (requiredAttributeMap.get(bindingEnum) != null &&
//                        requiredAttributeMap.get(bindingEnum).contains(propertyName))) {
//            bindingAttributes.put(propertyName, value);
//        }
    }

    public static String getAnnotationValueAsString(PsiAnnotationMemberValue value) throws AzureExecutionException {
        if (value instanceof PsiExpression) {
            if (value instanceof PsiReferenceExpression) {
                PsiReferenceExpression referenceExpression = (PsiReferenceExpression) value;
                Object resolved = referenceExpression.resolve();
                if (resolved instanceof PsiEnumConstant) {
                    final PsiEnumConstant enumConstant = (PsiEnumConstant) resolved;
                    final PsiClass enumClass = enumConstant.getContainingClass();
                    if (enumClass != null) {
                        try {
                            return getEnumFieldString(enumClass.getQualifiedName(), enumConstant.getName());
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    } else {
                        return enumConstant.getName();
                    }
                }

            }
            Object obj = JavaConstantExpressionEvaluator.computeConstantExpression((PsiExpression) value, true);
            return Objects.toString(obj, null);
        } else if (value instanceof PsiLiteral) {
            return Objects.toString(((PsiLiteral) value).getValue(), null);
        }
        throw new AzureExecutionException("Cannot get annotation value of type : " + value.getClass().getName());
    }

    public void setAttribute(String attributeName, Object attributeValue) {
        this.bindingAttributes.put(attributeName, attributeValue);
    }

    public BindingEnumV2 getBindingEnum() {
        return bindingEnum;
    }

    private BindingEnumV2 bindingEnum;

    public String getType() {
        return type;
    }

}
