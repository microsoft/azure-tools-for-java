package com.microsoft.azuretools.core.utils;

import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.Activator;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.dialogs.MessageDialog;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public class EclipseAzureMessager implements IAzureMessager {
    @Override
    public boolean show(IAzureMessage raw) {
        if (raw.getPayload() instanceof Throwable) {
            Activator.getDefault().log("caught an error by messager", ((Throwable) raw.getPayload()));
        }
        switch (raw.getType()) {
            case ALERT:
            case CONFIRM:
                AzureTaskManager.getInstance().runLater(() -> {
                    final String title = StringUtils.firstNonBlank(raw.getTitle(), DEFAULT_TITLE);
//                    MessageDialogBuilder.yesNo(title, raw.getContent()).guessWindowAndAsk();
//                    MessageDialog.
                });
                return true;
            default:
        }
        final AzureTask<?> task = AzureTaskContext.current().getTask();
        final Boolean backgrounded = Optional.ofNullable(task).map(AzureTask::getBackgrounded).orElse(null);
        if (Objects.equals(backgrounded, Boolean.FALSE) && raw.getType() == IAzureMessage.Type.ERROR) {
            this.showErrorDialog(raw);
        } else {
            this.showNotification(raw);
        }
        return true;
    }

    private void showErrorDialog(@Nonnull IAzureMessage message) {
        Activator.getDefault().log(message.getContent());
        MessageDialog.openError(null, message.getTitle(), message.getContent());
    }

    private void showNotification(@Nonnull IAzureMessage message) {
        Activator.getDefault().log(message.getContent());
        MessageDialog.openError(null, message.getTitle(), message.getContent());
    }
}
