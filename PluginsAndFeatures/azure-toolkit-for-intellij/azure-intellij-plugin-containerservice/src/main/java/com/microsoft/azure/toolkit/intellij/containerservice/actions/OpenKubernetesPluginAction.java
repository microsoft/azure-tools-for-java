/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerservice.actions;

import com.google.common.util.concurrent.SettableFuture;
import com.intellij.execution.services.ServiceEventListener;
import com.intellij.execution.services.ServiceViewContributor;
import com.intellij.execution.services.ServiceViewManager;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.kubernetes.api.Context;
import com.intellij.kubernetes.api.KubernetesApiProvider;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusConnection;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesCluster;
import io.kubernetes.client.util.KubeConfig;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class OpenKubernetesPluginAction {

    public static final String NULL_SERVICE_MESSAGE = "failed to get Kubernetes service view, please check whether kubernetes plugin is correctly installed.";

    public static void selectKubernetesInKubernetesPlugin(@Nonnull final KubernetesCluster cluster, @Nonnull final Project project) {
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Services");
        if (Objects.nonNull(toolWindow) && !toolWindow.isActive()) {
            toolWindow.activate(() -> selectKubernetesInKubernetesPlugin(cluster, project));
            return;
        }
        final ServiceViewContributor<?> contributor = Arrays.stream(ServiceViewContributor.CONTRIBUTOR_EP_NAME.getExtensions())
                .filter(serviceView -> StringUtils.equalsIgnoreCase(serviceView.getViewDescriptor(project).getId(), "Kubernetes"))
                .findFirst().orElse(null);
        if (Objects.isNull(contributor)) {
            AzureMessager.getMessager().warning(NULL_SERVICE_MESSAGE);
            return;
        }
        final KubernetesApiProvider apiProvider = KubernetesApiProvider.Companion.getInstance(project);
        final Context context = apiProvider.getExistingContexts().stream()
                .filter(c -> StringUtils.equals(c.getName(), cluster.getName()))
                .findFirst().orElse(null);
        if (Objects.isNull(context)) {
            addClusterContext(cluster, contributor, project);
        } else {
            final ServiceViewContributor<?> service = contributor.getServices(project).stream()
                    .filter(s -> s instanceof ServiceViewContributor)
                    .map(s -> (ServiceViewContributor<?>) s)
                    .filter(s -> StringUtils.equals(getServiceViewName(s, project), cluster.getName()))
                    .findFirst().orElse(null);
            focusServiceView(service, contributor, project);
        }
    }

    private static void focusServiceView(@Nullable final ServiceViewContributor<?> service,
                                         @Nonnull final ServiceViewContributor<?> contributor, final Project project) {
        if (Objects.isNull(service)) {
            // todo: null handling
            return;
        }
        ServiceViewManager.getInstance(project).select(service, contributor.getClass(), true, true);
        ServiceViewManager.getInstance(project).expand(service, contributor.getClass());
    }

    private static String getServiceViewName(@Nonnull final ServiceViewContributor<?> view, @Nonnull final Project project) {
        final ItemPresentation presentation = view.getViewDescriptor(project).getPresentation();
        return Optional.ofNullable(presentation.getPresentableText())
                .filter(StringUtils::isNotBlank)
                .orElseGet(() -> presentation instanceof PresentationData ?
                        ((PresentationData) presentation).getColoredText().get(0).getText() : presentation.toString());
    }

    public static void addClusterContext(@Nonnull final KubernetesCluster cluster, ServiceViewContributor<?> contributor, @Nonnull final Project project) {
        final Context result = Optional.ofNullable(getAvailableContext(cluster, project))
                .orElseGet(() -> OpenKubernetesPluginAction.importClusterContext(cluster, project));
        if (Objects.isNull(result)) {
            return;
        }
        final KubernetesApiProvider apiProvider = KubernetesApiProvider.Companion.getInstance(project);
        apiProvider.addContexts$intellij_clouds_kubernetes(List.of(result));
        final MessageBus messageBus = project.getMessageBus();
        final MessageBusConnection connect = messageBus.connect();
        connect.subscribe(ServiceEventListener.TOPIC, (ServiceEventListener) serviceEvent -> {
            final boolean isAddEvent = serviceEvent.type == ServiceEventListener.EventType.SERVICE_ADDED;
            final boolean isKubernetesEvent = serviceEvent.contributorClass == contributor.getClass();
            final boolean isTargetService = serviceEvent.target instanceof ServiceViewContributor &&
                    StringUtils.equals(getServiceViewName((ServiceViewContributor<?>) serviceEvent.target, project), cluster.getName());
            if (isAddEvent && isKubernetesEvent && isTargetService) {
                focusServiceView((ServiceViewContributor<?>) serviceEvent.target, contributor, project);
                connect.disconnect();
            }
        });
    }

    private static Context getAvailableContext(@Nonnull final KubernetesCluster cluster, @Nonnull final Project project) {
        final KubernetesApiProvider apiProvider = KubernetesApiProvider.Companion.getInstance(project);
        return apiProvider.getActualState$intellij_clouds_kubernetes().getAvailableContexts().stream()
                .filter(c -> StringUtils.equals(c.getName(), cluster.getName()))
                .findFirst().orElse(null);
    }

    @Nullable
    private static Context importClusterContext(@Nonnull final KubernetesCluster cluster, @Nonnull final Project project) {
        GetKubuCredentialAction.getKubuCredential(cluster, project, false);
        final Path path = Path.of(System.getProperty("user.home"), KubeConfig.KUBEDIR, KubeConfig.KUBECONFIG);
        final VirtualFile file = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(path);
        if (Objects.isNull(file)) {
            return null;
        }
        final SettableFuture<Context> future = SettableFuture.create();
        KubernetesApiProvider.Companion.getInstance(project).checkConfiguration$intellij_clouds_kubernetes(file, new Continuation<>() {
            @Nonnull
            @Override
            public CoroutineContext getContext() {
                return EmptyCoroutineContext.INSTANCE;
            }

            @Override
            @SuppressWarnings("KotlinInternalInJava")
            public void resumeWith(@Nonnull Object o) {
                if (o instanceof com.intellij.kubernetes.api.ContextsConfiguration configuration) {
                    final Context context = configuration.getContexts().entrySet().stream()
                            .filter(c -> StringUtils.equals(c.getKey().getName(), cluster.getName()))
                            .findFirst()
                            .map(Map.Entry::getValue)
                            .orElse(null);
                    future.set(context);
                } else {
                    future.set(null);
                }
            }
        });
        try {
            return future.get();
        } catch (final InterruptedException | ExecutionException e) {
            return null;
        }
    }

}
