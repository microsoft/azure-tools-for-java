package com.microsoft.azure.toolkit.intellij.cognitiveservices;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.guidance.GuidanceViewManager;
import com.microsoft.azure.toolkit.intellij.common.action.IntellijAzureActionManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.cognitiveservices.AzureCognitiveServices;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

import static com.microsoft.azure.toolkit.ide.cognitiveservices.CognitiveServicesActionsContributor.OPEN_DEPLOYMENT_IN_PLAYGROUND;
import static com.microsoft.azure.toolkit.intellij.cognitiveservices.IntelliJCognitiveServicesActionsContributor.TRY_OPENAI;
import static com.microsoft.azure.toolkit.intellij.cognitiveservices.IntelliJCognitiveServicesActionsContributor.TRY_PLAYGROUND;

public class CognitiveServicesStartupListener implements ProjectActivity {
    @Override
    public Object execute(@Nonnull Project project, @Nonnull Continuation<? super Unit> continuation) {
        tryOpenAI(project);
        tryPlayground();
        return null;
    }

    private static void tryOpenAI(@Nonnull Project project) {
        if (!IntellijAzureActionManager.isSuppressed(TRY_OPENAI)) {
            final Action<Project> tryOpenAI = new Action<>(TRY_OPENAI)
                .withLabel("Try Azure OpenAI")
                .withHandler((_d, e) -> {
                    GuidanceViewManager.getInstance().openCourseView(project, "hello-openai");
                    IntellijAzureActionManager.suppress(TRY_OPENAI);
                })
                .withAuthRequired(false);
            final AzureString msg = AzureString.format("You can use Azure OpenAI to build your own \"%s\" or other models. " +
                "<a href='https://go.microsoft.com/fwlink/?linkid=2202896'>Learn more</a> about Azure OpenAI.", "Copilot");
            final AzureActionManager am = AzureActionManager.getInstance();
            final Action<Action.Id<?>> suppress = Optional.ofNullable(am).map(m -> m.getAction(ResourceCommonActionsContributor.SUPPRESS_ACTION).bind(TRY_OPENAI)).orElse(null);
            if(Objects.nonNull(suppress)){
                AzureMessager.getMessager().info(msg, "Azure OpenAI is supported!", tryOpenAI, suppress);
            }else{
                AzureMessager.getMessager().info(msg, "Azure OpenAI is supported!", tryOpenAI);
            }
        }
    }

    private static void tryPlayground() {
        if (!IntellijAzureActionManager.isSuppressed(TRY_PLAYGROUND)) {
            AzureEventBus.once("account.subscription_changed.account", (_a, _b) -> Azure.az(AzureCognitiveServices.class).list().stream()
                .flatMap(m -> m.accounts().list().stream())
                .flatMap(a -> a.deployments().list().stream())
                .filter(d -> d.getModel().isGPTModel())
                .findFirst().ifPresent(d -> {
                    final Action<CognitiveDeployment> tryPlayGround =
                        new Action<>(TRY_PLAYGROUND)
                            .withIdParam(d.getName())
                            .withLabel("Open in AI Playground")
                            .withSource(d)
                            .withHandler((_d, e) -> {
                                AzureActionManager.getInstance().getAction(OPEN_DEPLOYMENT_IN_PLAYGROUND).handle(d, e);
                                IntellijAzureActionManager.suppress(TRY_PLAYGROUND);
                            })
                            .withAuthRequired(true);
                    final Action<Action.Id<?>> suppress = AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.SUPPRESS_ACTION).bind(TRY_PLAYGROUND);
                    final AzureString msg = AzureString.format("GPT* model based deployment (%s) is detected in your Azure OpenAI service (%s). " +
                        "You can try your own \"%s\" in AI playground.", d.getName(), d.getParent().getName(), "Copilot");
                    AzureMessager.getMessager().info(msg, "GPT* model is detected!", tryPlayGround, suppress);
                }));
        }
    }
}
