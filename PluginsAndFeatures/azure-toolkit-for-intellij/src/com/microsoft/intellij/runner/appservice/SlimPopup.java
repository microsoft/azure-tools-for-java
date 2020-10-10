//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.intellij.lang.regexp.intention;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.lang.Language;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.Alarm;
import com.intellij.util.Alarm.ThreadToUse;
import com.intellij.util.ui.JBUI.Borders;
import java.awt.BorderLayout;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.intellij.lang.regexp.RegExpFileType;
import org.intellij.lang.regexp.RegExpLanguage;
import org.intellij.lang.regexp.RegExpMatchResult;
import org.intellij.lang.regexp.RegExpMatcherProvider;
import org.intellij.lang.regexp.RegExpModifierProvider;
import org.jetbrains.annotations.NotNull;

public class CheckRegExpForm {
    public static final Key<Boolean> CHECK_REG_EXP_EDITOR = Key.create("CHECK_REG_EXP_EDITOR");
    private static final String LAST_EDITED_REGEXP = "last.edited.regexp";
    private static final JBColor BACKGROUND_COLOR_MATCH = new JBColor(15203035, 4478274);
    private static final JBColor BACKGROUND_COLOR_NOMATCH = new JBColor(16757152, 7220008);
    private final PsiFile myRegexpFile;
    private EditorTextField mySampleText;
    private EditorTextField myRegExp;
    private JPanel myRootPanel;
    private JBLabel myMessage;
    private Project myProject;

    public CheckRegExpForm(@NotNull PsiFile regexpFile) {
        if (regexpFile == null) {
            $$$reportNull$$$0(0);
        }

        super();
        this.myRegexpFile = regexpFile;
        this.$$$setupUI$$$();
    }

    private void createUIComponents() {
        this.myProject = this.myRegexpFile.getProject();
        Document document = PsiDocumentManager.getInstance(this.myProject).getDocument(this.myRegexpFile);
        Language language = this.myRegexpFile.getLanguage();
        Object fileType;
        if (language instanceof RegExpLanguage) {
            fileType = RegExpLanguage.INSTANCE.getAssociatedFileType();
        } else {
            fileType = new RegExpFileType(language);
        }

        this.myRegExp = new EditorTextField(document, this.myProject, (FileType)fileType, false, false) {
            protected EditorEx createEditor() {
                EditorEx editor = super.createEditor();
                editor.putUserData(CheckRegExpForm.CHECK_REG_EXP_EDITOR, Boolean.TRUE);
                editor.setEmbeddedIntoDialogWrapper(true);
                return editor;
            }

            protected void updateBorder(@NotNull EditorEx editor) {
                if (editor == null) {
                    $$$reportNull$$$0(0);
                }

                this.setupBorder(editor);
            }
        };
        String sampleText = PropertiesComponent.getInstance(this.myProject).getValue("last.edited.regexp", "Sample Text");
        this.mySampleText = new EditorTextField(sampleText, this.myProject, PlainTextFileType.INSTANCE) {
            protected EditorEx createEditor() {
                EditorEx editor = super.createEditor();
                editor.setEmbeddedIntoDialogWrapper(true);
                return editor;
            }

            protected void updateBorder(@NotNull EditorEx editor) {
                if (editor == null) {
                    $$$reportNull$$$0(0);
                }

                this.setupBorder(editor);
            }
        };
        this.mySampleText.setOneLineMode(false);
        int preferredWidth = Math.max(JBUIScale.scale(250), this.myRegExp.getPreferredSize().width);
        this.myRegExp.setPreferredWidth(preferredWidth);
        this.mySampleText.setPreferredWidth(preferredWidth);
        this.myRootPanel = new JPanel(new BorderLayout()) {
            Disposable disposable;
            Alarm updater;

            public void addNotify() {
                super.addNotify();
                this.disposable = Disposer.newDisposable();
                IdeFocusManager.getGlobalInstance().requestFocus(CheckRegExpForm.this.mySampleText, true);
                this.registerFocusShortcut(CheckRegExpForm.this.myRegExp, "shift TAB", CheckRegExpForm.this.mySampleText);
                this.registerFocusShortcut(CheckRegExpForm.this.myRegExp, "TAB", CheckRegExpForm.this.mySampleText);
                this.registerFocusShortcut(CheckRegExpForm.this.mySampleText, "shift TAB", CheckRegExpForm.this.myRegExp);
                this.registerFocusShortcut(CheckRegExpForm.this.mySampleText, "TAB", CheckRegExpForm.this.myRegExp);
                this.updater = new Alarm(ThreadToUse.POOLED_THREAD, this.disposable);
                DocumentListener documentListener = new DocumentListener() {
                    public void documentChanged(@NotNull DocumentEvent e) {
                        if (e == null) {
                            $$$reportNull$$$0(0);
                        }

                        update();
                    }
                };
                CheckRegExpForm.this.myRegExp.addDocumentListener(documentListener);
                CheckRegExpForm.this.mySampleText.addDocumentListener(documentListener);
                this.update();
                CheckRegExpForm.this.mySampleText.selectAll();
            }

            private void registerFocusShortcut(JComponent source, String shortcut, final EditorTextField target) {
                AnAction action = new AnAction() {
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        if (e == null) {
                            $$$reportNull$$$0(0);
                        }

                        IdeFocusManager.findInstance().requestFocus(target.getFocusTarget(), true);
                    }
                };
                action.registerCustomShortcutSet(CustomShortcutSet.fromString(new String[]{shortcut}), source);
            }

            private void update() {
                this.updater.cancelAllRequests();
                if (!this.updater.isDisposed()) {
                    this.updater.addRequest(() -> {
                        RegExpMatchResult result = CheckRegExpForm.isMatchingText(CheckRegExpForm.this.myRegexpFile, CheckRegExpForm.this.myRegExp.getText(), CheckRegExpForm.this.mySampleText.getText());
                        ApplicationManager.getApplication().invokeLater(() -> {
                            CheckRegExpForm.this.setBalloonState(result);
                        }, ModalityState.any(), (__) -> {
                            return this.updater.isDisposed();
                        });
                    }, 0);
                }

            }

            public void removeNotify() {
                super.removeNotify();
                Disposer.dispose(this.disposable);
                PropertiesComponent.getInstance(CheckRegExpForm.this.myProject).setValue("last.edited.regexp", CheckRegExpForm.this.mySampleText.getText());
            }
        };
        this.myRootPanel.setBorder(Borders.empty(4, 10));
    }

    void setBalloonState(RegExpMatchResult result) {
        this.mySampleText.setBackground(result == RegExpMatchResult.MATCHES ? BACKGROUND_COLOR_MATCH : BACKGROUND_COLOR_NOMATCH);
        switch(result) {
            case MATCHES:
                this.myMessage.setText("Matches!");
                break;
            case NO_MATCH:
                this.myMessage.setText("No match");
                break;
            case TIMEOUT:
                this.myMessage.setText("Pattern is too complex");
                break;
            case BAD_REGEXP:
                this.myMessage.setText("Bad pattern");
                break;
            case INCOMPLETE:
                this.myMessage.setText("More input expected");
                break;
            default:
                throw new AssertionError();
        }

        this.myRootPanel.revalidate();
        Balloon balloon = JBPopupFactory.getInstance().getParentBalloonFor(this.myRootPanel);
        if (balloon != null && !balloon.isDisposed()) {
            balloon.revalidate();
        }

    }

    @NotNull
    public JComponent getPreferredFocusedComponent() {
        EditorTextField var10000 = this.mySampleText;
        if (var10000 == null) {
            $$$reportNull$$$0(1);
        }

        return var10000;
    }

    @NotNull
    public JPanel getRootPanel() {
        JPanel var10000 = this.myRootPanel;
        if (var10000 == null) {
            $$$reportNull$$$0(2);
        }

        return var10000;
    }

    public static boolean isMatchingTextTest(@NotNull PsiFile regexpFile, @NotNull String sampleText) {
        if (regexpFile == null) {
            $$$reportNull$$$0(3);
        }

        if (sampleText == null) {
            $$$reportNull$$$0(4);
        }

        return isMatchingText(regexpFile, regexpFile.getText(), sampleText) == RegExpMatchResult.MATCHES;
    }

    static RegExpMatchResult isMatchingText(@NotNull PsiFile regexpFile, String regexpText, @NotNull String sampleText) {
        if (regexpFile == null) {
            $$$reportNull$$$0(5);
        }

        if (sampleText == null) {
            $$$reportNull$$$0(6);
        }

        Language regexpFileLanguage = regexpFile.getLanguage();
        RegExpMatcherProvider matcherProvider = (RegExpMatcherProvider)RegExpMatcherProvider.EP.forLanguage(regexpFileLanguage);
        if (matcherProvider != null) {
            RegExpMatchResult result = (RegExpMatchResult)ReadAction.compute(() -> {
                PsiLanguageInjectionHost host = InjectedLanguageUtil.findInjectionHost(regexpFile);
                return host != null ? matcherProvider.matches(regexpText, regexpFile, host, sampleText, 1000L) : null;
            });
            if (result != null) {
                return result;
            }
        }

        Integer patternFlags = (Integer)ReadAction.compute(() -> {
            PsiLanguageInjectionHost host = InjectedLanguageUtil.findInjectionHost(regexpFile);
            int flags = 0;
            if (host != null) {
                Iterator var3 = RegExpModifierProvider.EP.allForLanguage(host.getLanguage()).iterator();

                while(var3.hasNext()) {
                    RegExpModifierProvider provider = (RegExpModifierProvider)var3.next();
                    flags = provider.getFlags(host, regexpFile);
                    if (flags > 0) {
                        break;
                    }
                }
            }

            return flags;
        });

        try {
            Matcher matcher = Pattern.compile(regexpText, patternFlags).matcher(StringUtil.newBombedCharSequence(sampleText, 1000L));
            if (matcher.matches()) {
                return RegExpMatchResult.MATCHES;
            } else {
                return matcher.hitEnd() ? RegExpMatchResult.INCOMPLETE : RegExpMatchResult.NO_MATCH;
            }
        } catch (ProcessCanceledException var7) {
            return RegExpMatchResult.TIMEOUT;
        } catch (Exception var8) {
            return RegExpMatchResult.BAD_REGEXP;
        }
    }
}
