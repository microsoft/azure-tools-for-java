package com.microsoft.azure.toolkit.intellij.containerregistry;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.PushResponseItem;
import com.intellij.openapi.vfs.VfsUtil;
import com.microsoft.azure.toolkit.ide.containerregistry.ContainerRegistryActionsContributor;
import com.microsoft.azure.toolkit.intellij.connector.dotazure.AzureModule;
import com.microsoft.azure.toolkit.intellij.container.AzureDockerClient;
import com.microsoft.azure.toolkit.intellij.container.model.DockerImage;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerregistry.AzureContainerRegistry;
import com.microsoft.azure.toolkit.lib.containerregistry.ContainerRegistry;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContainerService {
    private static final ContainerService instance = new ContainerService();

    public static ContainerService getInstance() {
        return ContainerService.instance;
    }

    public String pushDockerImage(@Nonnull final IDockerPushConfiguration configuration) throws InterruptedException {
        final IAzureMessager messager = AzureMessager.getMessager();
        final DockerImage image = Objects.requireNonNull(configuration.getDockerImageConfiguration(), "image is not configured.");
        final ContainerRegistry registry = Objects.requireNonNull(Azure.az(AzureContainerRegistry.class).getById(configuration.getContainerRegistryId()), "registry doesn't exist.");
        if (!registry.isAdminUserEnabled()) {
            final Action<ContainerRegistry> enableAdminUser = AzureActionManager.getInstance().getAction(ContainerRegistryActionsContributor.ENABLE_ADMIN_USER).bind(registry);
            throw new AzureToolkitRuntimeException(String.format("Admin user is not enabled for (%s), but it is required to push image to Azure Container Registry.", registry.getName()), enableAdminUser);
        }
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        tm.runOnPooledThread(() -> Optional.of(image)
            .map(DockerImage::getDockerFile)
            .map(f -> VfsUtil.findFileByIoFile(new File(f), true))
            .map(f -> AzureModule.from(f, configuration.getProject()))
            .ifPresent(module -> tm.runLater(() -> tm.write(() -> module
                .initializeWithDefaultProfileIfNot()
                .addApp(registry).save()))));
        final AzureDockerClient dockerClient = AzureDockerClient.from(Objects.requireNonNull(configuration.getDockerHostConfiguration()));
        final String loginServerUrl = registry.getLoginServerUrl();
        final String imageAndTag = configuration.getFinalImageName();
        // tag image with ACR url
        if (!StringUtils.equals(image.getImageName(), imageAndTag)) {
            dockerClient.tagImage(image.getImageName(), configuration.getFinalRepositoryName(), configuration.getFinalTagName());
        }
        // push to ACR
        messager.info(String.format("Pushing to ACR ... [%s] ", loginServerUrl));
        final ResultCallback.Adapter<PushResponseItem> callBack = new ResultCallback.Adapter<>() {
            @Override
            public void onNext(PushResponseItem item) {
                //noinspection deprecation
                final String message = Stream.of(item.getStatus(), item.getId(), item.getProgress()).filter(StringUtils::isNoneBlank).collect(Collectors.joining(" "));
                messager.info(message);
                super.onNext(item);
            }
        };
        dockerClient.pushImage(Objects.requireNonNull(loginServerUrl), registry.getUserName(), registry.getPrimaryCredential(), imageAndTag, callBack);
        return loginServerUrl;
    }
}
