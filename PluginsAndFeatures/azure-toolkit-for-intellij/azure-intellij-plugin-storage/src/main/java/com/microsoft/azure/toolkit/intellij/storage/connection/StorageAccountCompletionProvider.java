package com.microsoft.azure.toolkit.intellij.storage.connection;

import com.intellij.openapi.module.Module;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.code.CompletionMetadataProvider;
import com.microsoft.azure.toolkit.intellij.connector.code.model.AnnotationIdentifier;
import com.microsoft.azure.toolkit.intellij.connector.code.model.CompletionItem;
import com.microsoft.azure.toolkit.intellij.connector.code.model.ParameterIdentifier;
import com.microsoft.azure.toolkit.intellij.connector.code.provider.AnnotationCompletionMetadata;
import com.microsoft.azure.toolkit.intellij.connector.code.provider.MethodCompletionMetadata;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.Profile;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount;
import com.microsoft.azure.toolkit.lib.storage.StorageAccount;
import com.microsoft.azure.toolkit.lib.storage.blob.BlobFile;
import com.microsoft.azure.toolkit.lib.storage.model.StorageFile;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.intellij.connector.code.model.CompletionItem.DEFAULT_LOOKUP_VALUES_FUNCTION;

public class StorageAccountCompletionProvider implements CompletionMetadataProvider {
    @Override
    public List<MethodCompletionMetadata> getMethodCompletionMetadata() {
        final List<MethodCompletionMetadata> result = new ArrayList<>();
        final ParameterIdentifier parameterIdentifier = buildParameterIdentifier("com.azure.storage.blob.BlobServiceClientBuilder", "connectionString",
                List.of("java.lang.String"), 0);
        result.add(MethodCompletionMetadata.builder()
                .identifier(parameterIdentifier)
                .icon(AzureIcons.StorageAccount.MODULE)
                .connectedResourcesFunction(this::getStorageConnection)
                .completionItemsFunction(c -> convertToCompletionItems(c, connection ->
                        String.format("System.getenv(\"%s\")", String.format("%s_CONNECTION_STRING", connection.getEnvPrefix()))))
                .azureResourcesFunction(module -> Azure.az(AzureStorageAccount.class).accounts(true))
                .resourceDefinition(StorageAccountResourceDefinition.INSTANCE)
                .build());
        return result;
    }

    @Override
    public List<AnnotationCompletionMetadata> getAnnotationCompletionMetadata() {
        final List<AnnotationCompletionMetadata> result = new ArrayList<>();
        result.add(AnnotationCompletionMetadata.builder()
                .identifier(new AnnotationIdentifier("com.microsoft.azure.functions.annotation.BlobOutput", "connection"))
                .icon(AzureIcons.StorageAccount.MODULE)
                .connectedResourcesFunction(this::getStorageConnection)
                .completionItemsFunction((c, properties) -> convertToCompletionItems(c, connection -> String.format("\"%s\")", connection.getEnvPrefix())))
                .azureResourcesFunction(module -> Azure.az(AzureStorageAccount.class).accounts(true))
                .resourceDefinition(StorageAccountResourceDefinition.INSTANCE)
                .build());
        result.add(AnnotationCompletionMetadata.builder()
                .identifier(new AnnotationIdentifier("com.microsoft.azure.functions.annotation.BlobTrigger", "connection"))
                .icon(AzureIcons.StorageAccount.MODULE)
                .connectedResourcesFunction(this::getStorageConnection)
                .completionItemsFunction((c, properties) -> convertToCompletionItems(c, connection -> String.format("\"%s\"", connection.getEnvPrefix())))
                .azureResourcesFunction(module -> Azure.az(AzureStorageAccount.class).accounts(true))
                .resourceDefinition(StorageAccountResourceDefinition.INSTANCE)
                .build());
        result.add(AnnotationCompletionMetadata.builder()
                .identifier(new AnnotationIdentifier("com.microsoft.azure.functions.annotation.BlobTrigger", "path"))
                .icon(AzureIcons.StorageAccount.MODULE)
                .connectedResourcesFunction(this::getStorageConnection)
                .completionItemsFunction(this::getBlobPathCompletionItems)
                .build());
        return result;
    }

    private List<CompletionItem> getBlobPathCompletionItems(@Nonnull final Connection<?, ?> connection,
                                                            @Nonnull final Map<String, String> properties) {
        if (!StringUtils.equals(properties.get("connection"), connection.getEnvPrefix())) {
            return Collections.emptyList();
        }
        final String currentPath = properties.get("path");
        final StorageAccount data = (StorageAccount) connection.getResource().getData();
        return data.canHaveBlobs() ? listBlobContainer(data.getBlobContainerModule()::list, currentPath).stream()
                .map(f -> CompletionItem.builder()
                        .value(getBlobFullPath(f))
                        .displayName(getBlobFullPath(f))
                        .icon(AzureIcons.StorageAccount.CONTAINERS)
                        .lookupValues(Collections.singletonList(getBlobFullPath(f))).build())
                .collect(Collectors.toList()) : Collections.emptyList();
    }

    private List<? extends StorageFile> listBlobContainer(Supplier<List<? extends StorageFile>> supplier, final String prefix) {
        final List<? extends StorageFile> storageFiles = supplier.get();
        final List<? extends StorageFile> result = storageFiles.stream()
                .filter(f -> StringUtils.startsWith(getBlobFullPath(f), prefix))
                .collect(Collectors.toList());
        final StorageFile parentFile = storageFiles.stream()
                .filter(f -> StringUtils.startsWith(prefix, getBlobFullPath(f)))
                .findAny().orElse(null);
        return CollectionUtils.isNotEmpty(result) ? result : Objects.isNull(parentFile) ? Collections.emptyList() :
                listBlobContainer(parentFile.getSubFileModule()::list, prefix);
    }

    private static String getBlobFullPath(final StorageFile file) {
        return file instanceof BlobFile ? ((BlobFile) file).getContainer().getName() + "/" + file.getPath() : file.getName();
    }

    private List<CompletionItem> convertToCompletionItems(@Nonnull final Connection<? extends AzResource, ?> connection,
                                                          @Nonnull final Function<Connection<? extends AzResource, ?>, String> valueFunction) {
        final StorageAccount data = (StorageAccount) connection.getResource().getData();
        final CompletionItem completionItem = CompletionItem.builder()
                .value(valueFunction.apply(connection))
                .displayName(data.getName())
                .hintMessage(data.getResourceTypeName())
                .icon(AzureIcons.StorageAccount.MODULE)
                .lookupValues(DEFAULT_LOOKUP_VALUES_FUNCTION.apply(connection))
                .build();
        return Collections.singletonList(completionItem);
    }

    private List<Connection<? extends AzResource, ?>> getStorageConnection(@Nullable final Module module) {
        final Profile profile = Optional.ofNullable(module).map(m -> AzureModule.from(m).getDefaultProfile()).orElse(null);
        return Objects.isNull(profile) ? Collections.emptyList() : profile.getConnections()
                .stream().filter(c -> c.getResource().getDefinition() instanceof StorageAccountResourceDefinition)
                .map(c -> (Connection<? extends AzResource, ?>) c).collect(Collectors.toList());
    }
}
