/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.samples.view;

import com.intellij.dvcs.repo.ClonePathProvider;
import com.intellij.dvcs.ui.CloneDvcsValidationUtils;
import com.intellij.dvcs.ui.FilePathDocumentChildPathHandle;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.SearchTextField;
import com.intellij.ui.SideBorder;
import com.intellij.ui.components.ActionLink;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.IconUtil;
import com.intellij.util.ui.JBUI;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.TextDocumentListenerAdapter;
import com.microsoft.azure.toolkit.intellij.samples.model.GithubOrganization;
import com.microsoft.azure.toolkit.intellij.samples.model.GithubRepository;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.common.utils.TailingDebouncer;
import git4idea.GitUtil;
import git4idea.checkout.GitCheckoutProvider;
import git4idea.commands.Git;
import git4idea.remote.GitRememberedInputs;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;

public class AzureSamplesCloneDialogExtensionComponent extends VcsCloneDialogExtensionComponent {
    private final Project project;
    private final GithubOrganization organization;

    @Getter
    private JPanel view;
    private JPanel repositoriesPanel;
    private ActionLink nextButton;
    private ActionLink prevButton;
    private SearchTextField searchBox;
    private JPanel loadingPanel;
    private JLabel loadingIcon;
    private JBScrollPane scrollPane;
    private JPanel searchBoxPanel;
    private TextFieldWithBrowseButton directoryField;
    private JPanel paginationPanel;
    private HyperlinkLabel orgHtmlLink;

    private int page = 1;
    private boolean last = false;
    private boolean first = false;

    private final List<GithubRepositoryPanel> repositoryPanels = new ArrayList<>();
    private final FilePathDocumentChildPathHandle cloneDirectoryChildHandle;
    private GithubRepositoryPanel selectedPanel;

    public AzureSamplesCloneDialogExtensionComponent(@Nonnull Project project) {
        super();
        this.project = project;
        this.organization = new GithubOrganization("Azure-Samples");
        $$$setupUI$$$();
        init();
        this.cloneDirectoryChildHandle = FilePathDocumentChildPathHandle.Companion
            .install(this.directoryField.getTextField().getDocument(), ClonePathProvider.defaultParentDirectoryPath(this.project, GitRememberedInputs.getInstance()));
    }

    private void init() {
        final TailingDebouncer debouncer = new TailingDebouncer(() -> this.loadPage(1), 500);
        this.searchBox.addDocumentListener((TextDocumentListenerAdapter) debouncer::debounce);

        final JBTextField searchField = this.searchBox.getTextEditor();
        final Dimension size = searchField.getSize();
        final Dimension newSize = new Dimension(size.width, size.height + 4);
        searchField.setPreferredSize(newSize);
        searchField.getEmptyText().setText("Search Azure code samples...");
        searchField.setSize(newSize);
        searchField.setBorder(BorderFactory.createEmptyBorder());
        searchField.setBackground(JBUI.CurrentTheme.EditorTabs.background());
        this.searchBoxPanel.setBackground(JBUI.CurrentTheme.EditorTabs.background());
        this.searchBoxPanel.setBorder(BorderFactory.createCompoundBorder(
            new SideBorder(JBColor.border(), SideBorder.ALL),
            BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));

        this.scrollPane.setBorder(new SideBorder(JBColor.border(), SideBorder.LEFT | SideBorder.RIGHT));
        this.loadingIcon.setIcon(IconUtil.scale(IntelliJAzureIcons.getIcon(AzureIcons.Common.REFRESH_ICON), this.loadingIcon, 1.5f));

        this.paginationPanel.setBorder(new SideBorder(JBColor.border(), SideBorder.ALL));
        this.loadPage(1);

        //noinspection DialogTitleCapitalization
        this.orgHtmlLink.setHyperlinkText("https://github.com/Azure-Samples");
        this.orgHtmlLink.setHyperlinkTarget("https://github.com/Azure-Samples");
    }

    public void loadPrevPage(ActionEvent e) {
        if (!first) {
            loadPage(--page);
        }
    }

    public void loadNextPage(ActionEvent e) {
        if (!last) {
            loadPage(++page);
        }
    }

    private void loadPage(int page) {
        this.page = page;
        this.repositoriesPanel.removeAll();
        this.repositoriesPanel.setVisible(false);
        this.loadingPanel.setVisible(true);
        this.prevButton.setEnabled(false);
        this.nextButton.setEnabled(false);
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        final AzureString title = OperationBundle.description("user/samples.load_repositories");
        this.searchBox.addCurrentTextToHistory();
        tm.runInBackground(title, () -> organization.search(this.searchBox.getText(), page)).thenAccept(result -> tm.runLater(() -> {
            final int pages = result.count() / GithubOrganization.DEFAULT_PAGE_SIZE + ((result.count() % GithubOrganization.DEFAULT_PAGE_SIZE == 0) ? 0 : 1);
            last = page >= pages;
            first = page <= 1; // pages can be 0
            this.setRepositories(result.items());
            this.loadingPanel.setVisible(false);
            this.repositoriesPanel.setVisible(true);
            this.scrollPane.revalidate();
            this.scrollPane.repaint();
            this.nextButton.setEnabled(!last);
            this.prevButton.setEnabled(!first);
        }));
    }

    private void setRepositories(final List<GithubRepository> repositories) {
        if (CollectionUtils.isEmpty(repositories)) {
            return;
        }
        this.repositoryPanels.clear();
        for (int i = 0; i < repositories.size(); i++) {
            final GithubRepository repository = repositories.get(i);
            final GithubRepositoryPanel repositoryPanel = new GithubRepositoryPanel(repository, this.project);
            repositoryPanel.getContentPanel().addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    repositoryPanel.toggleHoverStatus(true);
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    final Component c = SwingUtilities.getDeepestComponentAt(e.getComponent(), e.getX(), e.getY());
                    repositoryPanel.toggleHoverStatus(c != null && SwingUtilities.isDescendingFrom(c, repositoryPanel.getContentPanel()));
                }
            });
            addSelectionListener(repositoryPanel.getContentPanel(), new MouseAdapter() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    select(repositoryPanel);
                }
            });
            if (i != 0) {
                this.repositoriesPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
            }
            this.repositoriesPanel.add(repositoryPanel.getContentPanel());
            this.repositoryPanels.add(repositoryPanel);
        }
        if (CollectionUtils.isNotEmpty(this.repositoryPanels)) {
            final GithubRepositoryPanel panel = this.repositoryPanels.get(0);
            select(panel);
        }
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    public void setVisible(boolean visible) {
        this.view.setVisible(visible);
    }

    private void addSelectionListener(@Nonnull final JComponent component, @Nonnull MouseListener mouseListener) {
        component.addMouseListener(mouseListener);
        component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Arrays.stream(component.getComponents()).filter(c -> c instanceof JComponent && !(c instanceof HyperlinkLabel))
            .forEach(child -> addSelectionListener((JComponent) child, mouseListener));
    }

    private void select(final GithubRepositoryPanel panel) {
        repositoryPanels.forEach(p -> p.toggleSelectedStatus(false));
        panel.toggleSelectedStatus(true);
        this.selectedPanel = panel;
        final String path = StringUtil.trimEnd(ClonePathProvider.relativeDirectoryPathForVcsUrl(project, this.selectedPanel.getRepo().getCloneUrl()), GitUtil.DOT_GIT);
        cloneDirectoryChildHandle.trySetChildPath(path);
        this.getDialogStateListener().onOkActionEnabled(true);
    }

    private void createUIComponents() {
        this.repositoriesPanel = new JPanel();
        this.repositoriesPanel.setLayout(new BoxLayout(this.repositoriesPanel, BoxLayout.Y_AXIS));
        this.prevButton = new ActionLink("Previous page", this::loadPrevPage);
        this.nextButton = new ActionLink("Next page", this::loadNextPage);
        this.prevButton.setAutoHideOnDisable(false);
        this.nextButton.setAutoHideOnDisable(false);
        this.prevButton.setIcon(AllIcons.General.ChevronLeft);
        this.nextButton.setIcon(AllIcons.General.ChevronRight, true);

        this.directoryField = new TextFieldWithBrowseButton();
        final FileChooserDescriptor fcd = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        fcd.setShowFileSystemRoots(true);
        fcd.setHideIgnored(false);
        this.directoryField.addBrowseFolderListener("Destination Directory", "Select a parent directory for the clone", project, fcd);
    }

    @Override
    @ExceptionNotification
    @AzureOperation(value = "user/samples.clone_repository.repo", params = "this.selectedPanel.getRepo().getFullName()")
    public void doClone(@Nonnull final CheckoutProvider.Listener checkoutListener) {
        final Path parent = Paths.get(directoryField.getText()).toAbsolutePath().getParent();
        final ValidationInfo destinationValidation = CloneDvcsValidationUtils.createDestination(parent.toString());
        if (destinationValidation != null) {
            AzureMessager.getMessager().error("Unable to find destination directory");
            return;
        }
        final LocalFileSystem lfs = LocalFileSystem.getInstance();
        final VirtualFile destinationParent = Optional.ofNullable(lfs.findFileByIoFile(parent.toFile()))
            .or(() -> Optional.ofNullable(lfs.refreshAndFindFileByIoFile(parent.toFile())))
            .orElse(null);
        if (destinationParent == null) {
            AzureMessager.getMessager().error("Unable to find destination directory");
            return;
        }
        final String directoryName = Paths.get(directoryField.getText()).getFileName().toString();
        final String parentDirectory = parent.toAbsolutePath().toString();
        final GithubRepository repo = this.selectedPanel.getRepo();
        GitCheckoutProvider.clone(project, Git.getInstance(), checkoutListener, destinationParent, repo.getCloneUrl(), directoryName, parentDirectory);
    }

    @Nonnull
    @Override
    public List<ValidationInfo> doValidateAll() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return this.searchBox;
    }

    @Override
    public void onComponentSelected() {
        getDialogStateListener().onOkActionNameChanged("Clone");
        final IdeFocusManager focusManager = IdeFocusManager.getInstance(project);
        focusManager.requestFocus(this.searchBox, true);
        if (Objects.nonNull(this.selectedPanel)) {
            this.select(this.selectedPanel);
        }
    }
}
