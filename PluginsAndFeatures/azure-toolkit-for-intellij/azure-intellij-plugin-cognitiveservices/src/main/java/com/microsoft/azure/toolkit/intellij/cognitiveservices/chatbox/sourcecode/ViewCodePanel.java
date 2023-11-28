package com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.sourcecode;

import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.ex.FileTypeManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;
import com.intellij.util.ui.UIUtil;
import com.microsoft.azure.toolkit.intellij.cognitiveservices.chatbox.ChatBot;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveAccount;
import com.microsoft.azure.toolkit.lib.cognitiveservices.CognitiveDeployment;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.apache.commons.lang3.BooleanUtils;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;

public class ViewCodePanel {
    @Nonnull
    private final ChatBot chatBot;

    private JTextPane lblHeadMsg;
    private JTextPane lblTailMsg;
    private JBLabel lblKey;
    private ExtendableTextField txtApiBase;
    private ExtendableTextField txtKey;
    private ExtendableTextField txtEndpoint;
    private JBLabel lblEndpoint;
    private JPanel contentPanel;
    private JPanel editorContainer;
    private JComboBox<?> comboLanguage;

    public ViewCodePanel(@Nonnull final ChatBot chatBot) {
        this.chatBot = chatBot;
        this.init();
    }

    @SuppressWarnings("deprecation")
    private void init() {
        final CognitiveDeployment deployment = this.chatBot.getDeployment();
        final CognitiveAccount account = deployment.getParent();

        final String key = Objects.requireNonNull(this.chatBot.getPrimaryKey());
        final String endpoint = deployment.getEndpoint();
        final String apiBase = account.getEndpoint();
        this.txtApiBase.setText(apiBase);
        this.txtApiBase.addExtension(ExtendableTextComponent.Extension.create(AllIcons.Actions.Copy, "Copy Api base", () -> copyApiBase(apiBase)));

        this.lblEndpoint.setIcon(AllIcons.General.ContextHelp);
        this.txtEndpoint.setText(endpoint);
        this.txtEndpoint.addExtension(ExtendableTextComponent.Extension.create(AllIcons.Actions.Copy, "Copy endpoint", () -> copyEndpoint(endpoint)));
        this.lblKey.setIcon(AllIcons.General.ContextHelp);
        this.txtKey.setText("*".repeat(key.length()));
        this.txtKey.addExtension(ExtendableTextComponent.Extension.create(AllIcons.Actions.Copy, "Copy key", () -> copyKey(key)));
        this.txtKey.addExtension(ExtendableTextComponent.Extension.create(AllIcons.General.InspectionsEye, "Show/Hide", () -> {
            if (BooleanUtils.isTrue((Boolean) this.txtKey.getClientProperty("keyVisible"))) {
                this.txtKey.putClientProperty("keyVisible", false);
                this.txtKey.setText("*".repeat(key.length()));
            } else {
                this.txtKey.putClientProperty("keyVisible", true);
                this.txtKey.setText(key);
            }
        }));

        this.lblTailMsg.setContentType("text/html");
        this.lblTailMsg.setEditorKit(new UIUtil.JBWordWrapHtmlEditorKit());
        Messages.configureMessagePaneUi(this.lblTailMsg, "<html><body>You should use environment variables or a secret management tool " +
            "like Azure Key Vault to prevent accidental exposure of your key in applications. " +
            "<a href=\"https://go.microsoft.com/fwlink/?linkid=2189347\">Learn more here</a></body></html");
        this.comboLanguage.addItemListener(e -> {
            final ISourceCodeGenerator generator = (ISourceCodeGenerator) e.getItem();
            this.editorContainer.removeAll();
            this.editorContainer.add(this.createCodeViewer(generator), BorderLayout.CENTER);
            this.editorContainer.revalidate();
            this.editorContainer.repaint();
        });
        this.editorContainer.add(this.createCodeViewer((ISourceCodeGenerator) this.comboLanguage.getItemAt(0)));
    }

    @AzureOperation(value = "user/openai.copy_key", source = "this.chatBot.getDeployment()")
    private static void copyKey(final String key) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(key), null);
    }

    @AzureOperation(value = "user/openai.copy_endpoint", source = "this.chatBot.getDeployment()")
    private static void copyEndpoint(final String endpoint) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(endpoint), null);
    }

    @AzureOperation(value = "user/openai.copy_apibase", source = "this.chatBot.getDeployment()")
    private static void copyApiBase(final String apiBase) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(apiBase), null);
    }

    @Nonnull
    public String getCode() {
        final ISourceCodeGenerator generator = (ISourceCodeGenerator) this.comboLanguage.getSelectedItem();
        return Objects.nonNull(generator) ? generator.generateCode(this.chatBot) : "";
    }

    private EditorTextField createCodeViewer(final ISourceCodeGenerator generator) {
        final Project project = DataManager.getInstance().getDataContext(this.contentPanel).getData(CommonDataKeys.PROJECT);
        final DocumentImpl document = new DocumentImpl("", true);
        final FileType fileType = FileTypeManagerEx.getInstance().getFileTypeByExtension(generator.getLanguage());
        final EditorTextField editor = new EditorTextField(document, project, fileType, true, false);
        editor.addSettingsProvider(e -> { // add scrolling/line number features
            e.setHorizontalScrollbarVisible(true);
            e.setVerticalScrollbarVisible(true);
            e.getSettings().setLineNumbersShown(true);
        });
        editor.setText(generator.generateCode(this.chatBot));
        return editor;
    }

    private void createUIComponents() {
        this.comboLanguage = new ComboBox<ISourceCodeGenerator>() {
            {
                this.setRenderer(new SimpleListCellRenderer<>() {
                    public void customize(@Nonnull JList<? extends ISourceCodeGenerator> l, ISourceCodeGenerator v, int i, boolean b, boolean b1) {
                        this.setText(v.getName());
                    }
                });
                this.addItem(new JavaSourceCodeGenerator());
                this.addItem(new JsonSourceCodeGenerator());
                this.addItem(new CurlSourceCodeGenerator());
            }
        };
    }
}
