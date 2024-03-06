/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerservice.actions;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.containerservice.ContainerServiceActionsContributor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.containerservice.KubernetesCluster;
import io.kubernetes.client.util.FilePersister;
import io.kubernetes.client.util.KubeConfig;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;

public class GetKubuCredentialAction {

    public static final String KUBECONFIG_MERGE_MESSAGE = "Kubeconfig for %s has been merged to local kube config and set as default context.";

    public static void getKubuCredential(@Nonnull KubernetesCluster cluster, @Nonnull Project project, boolean isAdmin) {
        try {
            final byte[] content = isAdmin ? cluster.getAdminKubeConfig() : cluster.getUserKubeConfig();
            mergeConfigToKubConfig(content);
            final Action action = KubernetesUtils.isKubernetesPluginEnabled() ?
                    AzureActionManager.getInstance().getAction(ContainerServiceActionsContributor.OPEN_KUBERNETES_PLUGIN).bind(cluster) :
                    KubernetesUtils.getInstallKubernetesPluginAction();
            AzureMessager.getMessager().info(AzureString.format(KUBECONFIG_MERGE_MESSAGE, cluster.getName()), null, action);
        } catch (final IOException e) {
            AzureMessager.getMessager().error(e);
        }
    }

    @AzureOperation(name = "boundary/kubernetes.merge_into_kube_config")
    private static void mergeConfigToKubConfig(@Nonnull final byte[] content) throws IOException {
        final File configFile = Path.of(System.getProperty("user.home"), KubeConfig.KUBEDIR, KubeConfig.KUBECONFIG).toFile();
        if (!configFile.exists() || configFile.getTotalSpace() == 0) {
            FileUtils.writeByteArrayToFile(configFile, content);
            return;
        }
        final KubeConfig origin = KubeConfig.loadKubeConfig(new FileReader(configFile));
        final ArrayList<Object> users = origin.getUsers();
        final ArrayList<Object> clusters = origin.getClusters();
        final ArrayList<Object> contexts = origin.getContexts();
        final KubeConfig newConfig = KubeConfig.loadKubeConfig(new StringReader(new String(content)));
        final ArrayList<Object> newConfigUsers = ObjectUtils.firstNonNull(newConfig.getUsers(), new ArrayList<>());
        final ArrayList<Object> newConfigClusters = ObjectUtils.firstNonNull(newConfig.getClusters(), new ArrayList<>());
        final ArrayList<Object> newConfigContexts = ObjectUtils.firstNonNull(newConfig.getContexts(), new ArrayList<>());
        for (final Object o : newConfigContexts) {
            if (o instanceof Map) {
                final Object context = ((Map<?, ?>) o).get("context");
                if (context instanceof Map) {
                    final String user = ((Map<?, ?>) context).get("user").toString();
                    if (StringUtils.startsWith(user, "clusterAdmin")) {
                        final String newName = ((Map<?, ?>) o).get("name") + "-admin";
                        //noinspection unchecked,rawtypes
                        ((Map) o).put("name", newName);
                        newConfig.setContext(newName);
                    }
                }
            }
        }
        final ArrayList<Object> mergedUsers = merge(users, newConfigUsers, "user");
        final ArrayList<Object> mergedClusters = merge(clusters, newConfigClusters, "cluster");
        final ArrayList<Object> mergedContexts = merge(contexts, newConfigContexts, "context");
        new FilePersister(configFile)
            .save(mergedContexts, mergedClusters, mergedUsers, origin.getPreferences(), newConfig.getCurrentContext());
    }

    public static ArrayList<Object> merge(ArrayList<Object> origin, ArrayList<Object> newConfig, String type) {
        final ArrayList<Object> result = origin == null ? new ArrayList<>() : new ArrayList<>(origin);
        for (final Object o : newConfig) {
            if (o instanceof Map) {
                final String name = ((Map<?, ?>) o).get("name").toString();
                final Object existingObject = result.stream().filter(map -> (map instanceof Map) &&
                    StringUtils.equals(((Map<?, ?>) map).get("name").toString(), name)).findFirst().orElse(null);
                if (existingObject != null) {
                    AzureMessager.getMessager().info(AzureString.format("skip merging as %s (%s) already exists in kubeconfig.", type, name));
                } else {
                    result.add(o);
                }
            }
        }
        return result;
    }
}
