package com.microsoft.azure.toolkit.intellij.storage.code.function;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.code.function.FunctionUtils;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.intellij.storage.connection.BaseStorageAccountResourceDefinition;
import com.microsoft.azure.toolkit.lib.storage.IStorageAccount;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

class Utils {

    public static List<Connection<?, ?>> getConnectionWithStorageAccount(@Nonnull final IStorageAccount account, @Nonnull final Module module) {
        return Optional.of(module).map(AzureModule::from)
            .map(AzureModule::getDefaultProfile).map(Profile::getConnectionManager).stream()
            .flatMap(m -> m.getConnections().stream())
            .filter(c -> c.getDefinition().getResourceDefinition() instanceof BaseStorageAccountResourceDefinition)
            .filter(c -> Objects.equals(c.getResource().getData(), account))
            .toList();
    }

    public static IStorageAccount getBindingStorageAccount(@Nonnull final PsiAnnotation annotation) {
        final PsiMethod method = PsiTreeUtil.getParentOfType(annotation, PsiMethod.class);
        final PsiAnnotation accountAnnotation = Objects.isNull(method) ? null : Arrays.stream(method.getAnnotations())
            .filter(ann -> StringUtils.equalsIgnoreCase(ann.getQualifiedName(), "com.microsoft.azure.functions.annotation.StorageAccount"))
            .findFirst().orElse(null);
        return Stream.of(accountAnnotation, annotation).filter(Objects::nonNull)
            .map(FunctionUtils::getConnectionFromAnnotation)
            .filter(Objects::nonNull)
            .filter(c -> c.getResource().getData() instanceof IStorageAccount)
            .map(c -> (IStorageAccount) c.getResource().getData())
            .findFirst().orElse(null);
    }
}
